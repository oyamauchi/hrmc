package ast

fun lex(text: String): List<Token> {
  val output = mutableListOf<Token>()
  var position = 0
  var line = 1
  var col = 1

  val mapping = linkedMapOf<Regex, (MatchResult, Position) -> Token>(
    "\\{".toRegex() to { _, p -> Symbol(SymbolType.LEFT_BRACE, p) },
    "}".toRegex() to { _, p -> Symbol(SymbolType.RIGHT_BRACE, p) },
    "\\(".toRegex() to { _, p -> Symbol(SymbolType.LEFT_PAREN, p) },
    "\\)".toRegex() to { _, p -> Symbol(SymbolType.RIGHT_PAREN, p) },
    "!=".toRegex() to { _ , p-> Symbol(SymbolType.NOT_EQUAL, p) },
    "==".toRegex() to { _, p -> Symbol(SymbolType.EQUAL_EQUAL, p) },
    "=".toRegex() to { _, p -> Symbol(SymbolType.EQUAL, p) },
    "\\*".toRegex() to { _, p -> Symbol(SymbolType.STAR, p) },
    "<=".toRegex() to { _, p -> Symbol(SymbolType.LESS_OR_EQUAL, p) },
    ">=".toRegex() to { _, p -> Symbol(SymbolType.GREATER_OR_EQUAL, p) },
    "<".toRegex() to { _, p -> Symbol(SymbolType.LESS_THAN, p) },
    ">".toRegex() to { _, p -> Symbol(SymbolType.GREATER_THAN, p) },
    "\\+\\+".toRegex() to { _, p -> Symbol(SymbolType.PLUS_PLUS, p) },
    "--".toRegex() to { _, p -> Symbol(SymbolType.MINUS_MINUS, p) },
    "\\+".toRegex() to { _, p -> Symbol(SymbolType.PLUS, p) },
    "-".toRegex() to { _, p -> Symbol(SymbolType.MINUS, p) },
    "&&".toRegex() to { _, p -> Symbol(SymbolType.LOGICAL_AND, p) },
    "\\|\\|".toRegex() to { _, p -> Symbol(SymbolType.LOGICAL_OR, p) },

    "inbox\\b".toRegex() to { _, p -> Symbol(SymbolType.INBOX, p) },
    "outbox\\b".toRegex() to { _, p -> Symbol(SymbolType.OUTBOX, p) },
    "while\\b".toRegex() to { _, p -> Symbol(SymbolType.WHILE, p) },
    "if\\b".toRegex() to { _, p -> Symbol(SymbolType.IF, p) },
    "else\\b".toRegex() to { _, p -> Symbol(SymbolType.ELSE, p) },
    "return\\b".toRegex() to { _, p -> Symbol(SymbolType.RETURN, p) },
    "break\\b".toRegex() to { _, p -> Symbol(SymbolType.BREAK, p) },
    "continue\\b".toRegex() to { _, p -> Symbol(SymbolType.CONTINUE, p) },

    "\\d+".toRegex() to { match, p -> IntToken(match.groupValues[0].toInt(), p) },
    "'([A-Z])'".toRegex() to { match, p -> LetterToken(match.groupValues[1][0], p) },
    "[a-zA-Z_]+".toRegex() to { match, p -> Identifier(match.groupValues[0], p) }
  )

  while (position < text.length) {
    if (text[position].isWhitespace()) {
      if (text[position] == '\n') {
        line++
        col = 1
      } else {
        col++
      }
      position++
      continue
    }

    if (text.substring(position).startsWith("//")) {
      val endOfLine = text.indexOf('\n', position)
      // No need to update line/col here; handling of the following newline will do it
      position = if (endOfLine == -1) text.length else endOfLine
      continue
    }

    var added = false

    for ((regex, callback) in mapping) {
      val matchResult = regex.find(text, position)
      if (matchResult != null && matchResult.range.first == position) {
        output.add(callback(matchResult, Position(line, col)))
        position = matchResult.range.last + 1
        // None of the tokens have an embedded newline, so no need to update line
        col += matchResult.value.length
        added = true
        break
      }
    }

    if (!added) {
      val endOfSnippet = minOf(text.length, position + 20)
      error(
        "Unable to parse program at line $line, column $col: " +
            "${text.substring(position, endOfSnippet).replace('\n', ' ')}..."
      )
    }
  }

  return output
}
