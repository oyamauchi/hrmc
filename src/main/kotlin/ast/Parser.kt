package ast

/*
start:
- stmt*

stmt:
- OUTBOX LPAREN expr RPAREN SEMICOLON
- RETURN SEMICOLON
- BREAK SEMICOLON
- CONTINUE SEMICOLON
- while [ LPAREN or-condition RPARENT ] stmt
- if LPAREN or-condition RPAREN stmt [ else stmt ]
- LBRACE stmt* RBRACE
- expr SEMICOLON

expr:
- expr PLUS term
- expr MINUS term
- term

term:
- letter
- int
- LPAREN expr RPAREN
- INBOX LPAREN RPAREN
- lval [ EQUAL expr ]
- PLUS_PLUS lval
- MINUS_MINUS lval

lval:
- IDENT
- STAR IDENT

or-condition:
- and-condition
- or-condition LOGICAL_OR and-condition

and-condition:
- condition
- and-condition LOGICAL_AND condition

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

  fun parse(): List<Statement> {
    val result = mutableListOf<Statement>()
    while (position < tokens.size) {
      result.add(readStmt())
    }
    return result
  }

  private class ParseException(
    position: Position?,
    message: String
  ) : RuntimeException("Parse error${position?.let { " at line ${it.line}, column ${it.column}" } ?: ""}: $message")

  private fun expect(vararg symbolTypes: SymbolType): SymbolType {
    if (position >= tokens.size) {
      throw ParseException(null, "Expected ${symbolTypes.toList()}; found end of input")
    }

    val token = tokens[position]
    if (token !is Symbol || token.type !in symbolTypes) {
      throw ParseException(token.position, "Expected ${symbolTypes.toList()}; found $token")
    }

    position++
    return token.type
  }

  private fun headIs(vararg symbolTypes: SymbolType): Boolean {
    return position < tokens.size && tokens[position].let { it is Symbol && it.type in symbolTypes }
  }

  private fun readStmt(): Statement {
    return when (val head = tokens[position]) {
      is Symbol -> when (head.type) {
        SymbolType.LEFT_BRACE -> {
          position++
          val statements = mutableListOf<Statement>()
          while (!headIs(SymbolType.RIGHT_BRACE)) {
            statements.add(readStmt())
          }
          expect(SymbolType.RIGHT_BRACE)
          StatementList(statements)
        }

        SymbolType.OUTBOX -> {
          position++
          expect(SymbolType.LEFT_PAREN)
          val operand = readExpr()
          expect(SymbolType.RIGHT_PAREN)
          expect(SymbolType.SEMICOLON)
          Outbox(operand)
        }

        SymbolType.BREAK -> {
          position++
          expect(SymbolType.SEMICOLON)
          Break
        }
        SymbolType.CONTINUE -> {
          position++
          expect(SymbolType.SEMICOLON)
          Continue
        }
        SymbolType.RETURN -> {
          position++
          expect(SymbolType.SEMICOLON)
          Return
        }

        SymbolType.IF -> {
          position++
          expect(SymbolType.LEFT_PAREN)
          val condition = readOrCondition()
          expect(SymbolType.RIGHT_PAREN)
          val trueBody = readStmt()
          val falseBody = if (headIs(SymbolType.ELSE)) {
            position++
            readStmt()
          } else {
            null
          }

          If(condition, trueBody, falseBody)
        }

        SymbolType.WHILE -> {
          position++
          val condition = if (headIs(SymbolType.LEFT_PAREN)) {
            expect(SymbolType.LEFT_PAREN)
            val c = readOrCondition()
            expect(SymbolType.RIGHT_PAREN)
            c
          } else {
            null
          }
          val body = readStmt()

          While(condition, body)
        }

        else -> {
          val expr = readExpr()
          expect(SymbolType.SEMICOLON)
          ExpressionStatement(expr)
        }
      }

      else -> {
        val expr = readExpr()
        expect(SymbolType.SEMICOLON)
        ExpressionStatement(expr)
      }
    }
  }

  private fun readExpr(): Expression {
    var left = readTerm()

    while (headIs(SymbolType.PLUS, SymbolType.MINUS)) {
      val operatorSymbol = expect(SymbolType.PLUS, SymbolType.MINUS)
      val right = readTerm()
      left = when (operatorSymbol) {
        SymbolType.PLUS -> Add(left, right)
        SymbolType.MINUS -> Subtract(left, right)
        else -> error("Should not happen")
      }
    }

    return left
  }

  private fun readTerm(): Expression {
    if (position >= tokens.size) {
      throw ParseException(null, "Unexpected end of input")
    }

    return when (val head = tokens[position]) {
      is IntToken -> {
        position++
        IntConstant(head.value)
      }
      is LetterToken -> {
        position++
        LetterConstant(head.value)
      }

      is Identifier -> {
        val name = readLval().first
        if (headIs(SymbolType.EQUAL)) {
          expect(SymbolType.EQUAL)
          val right = readExpr()
          AssignVar(name, right)
        } else {
          ReadVar(name)
        }
      }

      is Symbol -> when (head.type) {
        SymbolType.INBOX -> {
          expect(SymbolType.INBOX)
          expect(SymbolType.LEFT_PAREN)
          expect(SymbolType.RIGHT_PAREN)
          Inbox
        }

        SymbolType.LEFT_PAREN -> {
          expect(SymbolType.LEFT_PAREN)
          val expr = readExpr()
          expect(SymbolType.RIGHT_PAREN)
          expr
        }

        SymbolType.STAR -> {
          val name = readLval().first
          if (headIs(SymbolType.EQUAL)) {
            expect(SymbolType.EQUAL)
            val right = readExpr()
            WriteMem(name, right)
          } else {
            ReadMem(name)
          }
        }

        SymbolType.PLUS_PLUS,
        SymbolType.MINUS_MINUS -> {
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

        SymbolType.SEMICOLON,
        SymbolType.OUTBOX,
        SymbolType.BREAK,
        SymbolType.CONTINUE,
        SymbolType.RETURN,
        SymbolType.IF,
        SymbolType.WHILE,
        SymbolType.RIGHT_PAREN,
        SymbolType.LEFT_BRACE,
        SymbolType.RIGHT_BRACE,
        SymbolType.ELSE,
        SymbolType.EQUAL,
        SymbolType.EQUAL_EQUAL,
        SymbolType.NOT_EQUAL,
        SymbolType.LESS_THAN,
        SymbolType.GREATER_THAN,
        SymbolType.LESS_OR_EQUAL,
        SymbolType.GREATER_OR_EQUAL,
        SymbolType.LOGICAL_AND,
        SymbolType.LOGICAL_OR,
        SymbolType.PLUS,
        SymbolType.MINUS -> throw ParseException(head.position, "Unexpected token ${head.type}")
      }
    }
  }

  private fun readOrCondition(): Condition {
    var left = readAndCondition()

    while (headIs(SymbolType.LOGICAL_OR)) {
      expect(SymbolType.LOGICAL_OR)
      val right = readAndCondition()
      left = OrCondition(left, right)
    }

    return left
  }

  private fun readAndCondition(): Condition {
    var left: Condition = readCondition()

    while (headIs(SymbolType.LOGICAL_AND)) {
      expect(SymbolType.LOGICAL_AND)
      val right = readCondition()
      left = AndCondition(left, right)
    }

    return left
  }

  private fun readCondition(): Compare {
    val left = readExpr()
    val compareOp = (tokens[position] as? Symbol)?.let {
      when (it.type) {
        SymbolType.EQUAL_EQUAL -> CompareOp.Equal
        SymbolType.NOT_EQUAL -> CompareOp.NotEqual
        SymbolType.LESS_THAN -> CompareOp.LessThan
        SymbolType.LESS_OR_EQUAL -> CompareOp.LessOrEqual
        SymbolType.GREATER_THAN -> CompareOp.GreaterThan
        SymbolType.GREATER_OR_EQUAL -> CompareOp.GreaterOrEqual
        else -> throw ParseException(it.position, "Invalid comparison operator $it")
      }
    } ?: throw ParseException(tokens[position].position, "Invalid comparison operator ${tokens[position]}")

    position++
    val right = readExpr()
    return Compare(compareOp, left, right)
  }

  private fun readLval(): Pair<String, Boolean> {
    val dereference = if (headIs(SymbolType.STAR)) {
      expect(SymbolType.STAR)
      true
    } else {
      false
    }

    val ident = readIdent()
    return Pair(ident, dereference)
  }

  private fun readIdent(): String {
    if (position <= tokens.size && tokens[position] is Identifier) {
      position++
      return (tokens[position - 1] as Identifier).name
    } else {
      throw ParseException(tokens[position].position, "Expected identifier; found ${tokens[position]}")
    }
  }
}
