package ast

import org.junit.Test
import kotlin.test.assertEquals

class LexerTest {
  @Test
  fun `all tokens`() {
    val text = """
      while if else
      inbox outbox
      {}()
      = == != < > <= >=
      * ++ --
      + -
      break continue return
      // comment
      argBlarg_flarg
      128 'A'
    """.trimIndent()

    assertEquals(
      listOf(
        Symbol(SymbolType.WHILE, Position(1, 1)),
        Symbol(SymbolType.IF, Position(1, 7)),
        Symbol(SymbolType.ELSE, Position(1, 10)),
        Symbol(SymbolType.INBOX, Position(2, 1)),
        Symbol(SymbolType.OUTBOX, Position(2, 7)),
        Symbol(SymbolType.LEFT_BRACE, Position(3, 1)),
        Symbol(SymbolType.RIGHT_BRACE, Position(3, 2)),
        Symbol(SymbolType.LEFT_PAREN, Position(3, 3)),
        Symbol(SymbolType.RIGHT_PAREN, Position(3, 4)),
        Symbol(SymbolType.EQUAL, Position(4, 1)),
        Symbol(SymbolType.EQUAL_EQUAL, Position(4, 3)),
        Symbol(SymbolType.NOT_EQUAL, Position(4, 6)),
        Symbol(SymbolType.LESS_THAN, Position(4, 9)),
        Symbol(SymbolType.GREATER_THAN, Position(4, 11)),
        Symbol(SymbolType.LESS_OR_EQUAL, Position(4, 13)),
        Symbol(SymbolType.GREATER_OR_EQUAL, Position(4, 16)),
        Symbol(SymbolType.STAR, Position(5, 1)),
        Symbol(SymbolType.PLUS_PLUS, Position(5, 3)),
        Symbol(SymbolType.MINUS_MINUS, Position(5, 6)),
        Symbol(SymbolType.PLUS, Position(6, 1)),
        Symbol(SymbolType.MINUS, Position(6, 3)),
        Symbol(SymbolType.BREAK, Position(7, 1)),
        Symbol(SymbolType.CONTINUE, Position(7, 7)),
        Symbol(SymbolType.RETURN, Position(7, 16)),
        Identifier("argBlarg_flarg", Position(9,1)),
        IntToken(128, Position(10,1)),
        LetterToken('A', Position(10,5))
      ),
      lex(text)
    )
  }
}