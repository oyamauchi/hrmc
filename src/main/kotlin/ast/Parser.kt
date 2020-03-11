package ast

/*
start:
- expr-list

expr-list
- empty
- expr expr-list

expr:
- term PLUS term
- term MINUS term
- term

term:
- letter
- int
- LPAREN expr RPAREN
- while [ LPAREN condition RPAREN ] LBRACE expr-list RBRACE
- if LPAREN condition RPAREN LBRACE expr-list RBRACE [ else LBRACE expr-list RBRACE ]
- RETURN
- INBOX LPAREN RPAREN
- OUTBOX LPAREN expr RPAREN
- lval EQUAL expr
- PLUS_PLUS lval
- MINUS_MINUS lval

lval:
- IDENT
- STAR IDENT

condition:
- expr EQUAL_EQUAL expr
- expr NOT_EQUAL expr
- expr LESS_THAN expr
- expr LESS_OR_EQUAL expr
- expr GREATER_THAN expr
- expr GREATER_OR_EQUAL expr

 */
class Parser(private val tokens: List<Token>) {
  private var position = 0
  private var maxFailPosition = -1

  fun parse(): List<Expression> {
    val result = readExpressionList()
    if (position < tokens.size) {
      throw IllegalArgumentException(
        "Tokens at farthest: ${tokens.subList(maxFailPosition, tokens.size)}")
    }
    return result
  }

  private class ParseException(
    val position: Int,
    message: String? = null
  ) : RuntimeException(message)

  private fun <T> withMark(parse: () -> T): T? {
    val original = position
    return try {
      parse()
    } catch (e: ParseException) {
      if (position > maxFailPosition) {
        maxFailPosition = position
      }
      position = original
      null
    }
  }

  private fun expect(vararg symbolTypes: SymbolType): SymbolType {
    if (position >= tokens.size) {
      throw ParseException(position)
    }

    val token = tokens[position]
    if (token !is Symbol || token.type !in symbolTypes) {
      throw ParseException(position)
    }

    position++
    return token.type
  }

  private fun headIs(symbolType: SymbolType): Boolean {
    return position < tokens.size && tokens[position] == Symbol(symbolType)
  }

  private fun readExpressionList(): List<Expression> {
    return withMark {
      val expr = readExpr()
      listOf(expr) + readExpressionList()
    }
      ?: emptyList()
  }

  private fun readExpr(): Expression {
    return withMark {
      val left = readTerm()
      when {
        headIs(SymbolType.PLUS) -> {
          expect(SymbolType.PLUS)
          val right = readTerm()
          Add(left, right)
        }
        headIs(SymbolType.MINUS) -> {
          expect(SymbolType.MINUS)
          val right = readTerm()
          Subtract(left, right)
        }
        else -> left
      }
    } ?: throw ParseException(position)
  }

  private fun readTerm(): Expression {
    if (position >= tokens.size) {
      throw ParseException(position)
    }

    val simpleToken = tokens[position].let {
      when (it) {
        is IntToken -> IntConstant(it.value)
        is LetterToken -> LetterConstant(it.value)
        Symbol(SymbolType.RETURN) -> Terminate
        Symbol(SymbolType.BREAK) -> Break
        else -> null
      }
    }

    if (simpleToken != null) {
      position++
      return simpleToken
    }

    return withMark {
      expect(SymbolType.INBOX)
      expect(SymbolType.LEFT_PAREN)
      expect(SymbolType.RIGHT_PAREN)
      Inbox
    }
      ?: withMark {
        expect(SymbolType.OUTBOX)
        expect(SymbolType.LEFT_PAREN)
        val operand = readExpr()
        expect(SymbolType.RIGHT_PAREN)
        Outbox(operand)
      }
      ?: withMark {
        expect(SymbolType.WHILE)
        val condition = if (headIs(SymbolType.LEFT_PAREN)) {
          expect(SymbolType.LEFT_PAREN)
          val c = readCondition()
          expect(SymbolType.RIGHT_PAREN)
          c
        } else {
          null
        }
        expect(SymbolType.LEFT_BRACE)
        val body = readExpressionList()
        expect(SymbolType.RIGHT_BRACE)
        While(condition, body)
      }
      ?: withMark {
        expect(SymbolType.IF)
        expect(SymbolType.LEFT_PAREN)
        val condition = readCondition()
          ?: throw ParseException(position, "if without condition")
        expect(SymbolType.RIGHT_PAREN)
        expect(SymbolType.LEFT_BRACE)
        val trueBody = readExpressionList()
        expect(SymbolType.RIGHT_BRACE)
        val falseBody = if (headIs(SymbolType.ELSE)) {
          expect(SymbolType.ELSE)
          expect(SymbolType.LEFT_BRACE)
          val body = readExpressionList()
          expect(SymbolType.RIGHT_BRACE)
          body
        } else {
          emptyList()
        }
        If(condition, trueBody, falseBody)
      }
      ?: withMark {
        expect(SymbolType.LEFT_PAREN)
        val expr = readExpr()
        expect(SymbolType.RIGHT_PAREN)
        expr
      }
      ?: withMark {
        val (name, isDereference) = readLval()
        expect(SymbolType.EQUAL)
        val right = readExpr()
        if (isDereference) {
          WriteMem(name, right)
        } else {
          AssignVar(name, right)
        }
      }
      ?: withMark {
        val (name, isDereference) = readLval()
        if (isDereference) {
          ReadMem(name)
        } else {
          ReadVar(name)
        }
      }
      ?: withMark {
        val operator = expect(SymbolType.PLUS_PLUS, SymbolType.MINUS_MINUS)
        val (name, isDereference) = readLval()
        if (isDereference) {
          if (operator == SymbolType.PLUS_PLUS) {
            IncMem(name)
          } else {
            DecMem(name)
          }
        } else {
          if (operator == SymbolType.PLUS_PLUS) {
            IncVar(name)
          } else {
            DecVar(name)
          }
        }
      }
      ?: throw ParseException(position)
  }

  private fun readCondition(): Compare? {
    return withMark {
      val left = readExpr()
      val operator = expect(
        SymbolType.EQUAL_EQUAL,
        SymbolType.NOT_EQUAL,
        SymbolType.LESS_THAN,
        SymbolType.LESS_OR_EQUAL,
        SymbolType.GREATER_THAN,
        SymbolType.GREATER_OR_EQUAL
      )
      val right = readExpr()
      Compare(symbolToCompareOp(operator), left, right)
    }
  }

  private fun readLval(): Pair<String, Boolean> {
    return withMark {
      expect(SymbolType.STAR)
      Pair(readIdent(), true)
    }
      ?: withMark {
        Pair(readIdent(), false)
      }
      ?: throw ParseException(position)
  }

  private fun readIdent(): String {
    if (position <= tokens.size && tokens[position] is Identifier) {
      position++
      return (tokens[position - 1] as Identifier).name
    } else {
      throw ParseException(position)
    }
  }

  private fun symbolToCompareOp(symbolType: SymbolType): CompareOp {
    return when (symbolType) {
      SymbolType.EQUAL_EQUAL -> CompareOp.Equal
      SymbolType.NOT_EQUAL -> CompareOp.NotEqual
      SymbolType.LESS_THAN -> CompareOp.LessThan
      SymbolType.LESS_OR_EQUAL -> CompareOp.LessOrEqual
      SymbolType.GREATER_THAN -> CompareOp.GreaterThan
      SymbolType.GREATER_OR_EQUAL -> CompareOp.GreaterOrEqual
      else -> throw RuntimeException("Can't convert symbol $symbolType")
    }
  }
}
