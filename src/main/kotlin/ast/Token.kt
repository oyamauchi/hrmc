package ast

enum class SymbolType {
  WHILE,
  IF,
  ELSE,
  INBOX,
  OUTBOX,
  LEFT_BRACE,
  RIGHT_BRACE,
  LEFT_PAREN,
  RIGHT_PAREN,
  EQUAL,
  EQUAL_EQUAL,
  NOT_EQUAL,
  LESS_THAN,
  GREATER_THAN,
  LESS_OR_EQUAL,
  GREATER_OR_EQUAL,
  STAR,
  PLUS_PLUS,
  MINUS_MINUS,
  PLUS,
  MINUS,
  BREAK,
  CONTINUE,
  RETURN
}

data class Position(
  val line: Int, val column: Int
)

sealed class Token {
  abstract val position: Position
}

data class Symbol(val type: SymbolType, override val position: Position): Token()
data class Identifier(val name: String, override val position: Position): Token()
data class IntToken(val value: Int, override val position: Position): Token()
data class LetterToken(val value: Char, override val position: Position): Token()
