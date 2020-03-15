package ast

fun lex(text: String): List<Token> {
  val output = mutableListOf<Token>()
  var position = 0

  val mapping = linkedMapOf<Regex, (MatchResult) -> Token>(
    "\\{".toRegex() to { _ -> Symbol(SymbolType.LEFT_BRACE) },
    "}".toRegex() to { _ -> Symbol(SymbolType.RIGHT_BRACE) },
    "\\(".toRegex() to { _ -> Symbol(SymbolType.LEFT_PAREN) },
    "\\)".toRegex() to { _ -> Symbol(SymbolType.RIGHT_PAREN) },
    "!=".toRegex() to { _ -> Symbol(SymbolType.NOT_EQUAL) },
    "==".toRegex() to { _ -> Symbol(SymbolType.EQUAL_EQUAL) },
    "=".toRegex() to { _ -> Symbol(SymbolType.EQUAL) },
    "\\*".toRegex() to { _ -> Symbol(SymbolType.STAR) },
    "<=".toRegex() to { _ -> Symbol(SymbolType.LESS_OR_EQUAL) },
    ">=".toRegex() to { _ -> Symbol(SymbolType.GREATER_OR_EQUAL) },
    "<".toRegex() to { _ -> Symbol(SymbolType.LESS_THAN) },
    ">".toRegex() to { _ -> Symbol(SymbolType.GREATER_THAN) },
    "\\+\\+".toRegex() to { _ -> Symbol(SymbolType.PLUS_PLUS) },
    "--".toRegex() to { _ -> Symbol(SymbolType.MINUS_MINUS) },
    "\\+".toRegex() to { _ -> Symbol(SymbolType.PLUS) },
    "-".toRegex() to { _ -> Symbol(SymbolType.MINUS) },

    "inbox\\b".toRegex() to { _ -> Symbol(SymbolType.INBOX) },
    "outbox\\b".toRegex() to { _ -> Symbol(SymbolType.OUTBOX) },
    "while\\b".toRegex() to { _ -> Symbol(SymbolType.WHILE) },
    "if\\b".toRegex() to { _ -> Symbol(SymbolType.IF) },
    "else\\b".toRegex() to { _ -> Symbol(SymbolType.ELSE) },
    "return\\b".toRegex() to { _ -> Symbol(SymbolType.RETURN) },
    "break\\b".toRegex() to { _ -> Symbol(SymbolType.BREAK) },
    "continue\\b".toRegex() to { _ -> Symbol(SymbolType.CONTINUE) },

    "\\d+".toRegex() to { match -> IntToken(match.groupValues[0].toInt()) },
    "'([A-Z])'".toRegex() to { match -> LetterToken(match.groupValues[1][0]) },
    "[a-zA-Z_]+".toRegex() to { match -> Identifier(match.groupValues[0]) }
  )

  while (position < text.length) {
    if (text[position].isWhitespace()) {
      position++
      continue
    }

    if (text.substring(position).startsWith("//")) {
      val endOfLine = text.indexOf('\n', position)
      position = if (endOfLine == -1) text.length else endOfLine
      continue
    }

    var added = false

    for ((regex, callback) in mapping) {
      val matchResult = regex.find(text, position)
      if (matchResult != null && matchResult.range.first == position) {
        output.add(callback(matchResult))
        position = matchResult.range.last + 1
        added = true
        break
      }
    }

    if (!added) {
      val endOfSnippet = minOf(text.length, position + 20)
      error(
        "Unable to parse program at position $position: " +
            "${text.substring(position, endOfSnippet).replace('\n', ' ')}..."
      )
    }
  }

  return output
}
