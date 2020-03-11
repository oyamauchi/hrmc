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
  RETURN
}

sealed class Token

data class Symbol(val type: SymbolType): Token()
data class Identifier(val name: String): Token()
data class IntToken(val value: Int): Token()
data class LetterToken(val value: Char): Token()
