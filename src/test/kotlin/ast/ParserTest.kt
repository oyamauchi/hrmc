package ast

import org.junit.Test
import kotlin.test.assertEquals

class ParserTest {
  private val POSITION = Position(1, 1)

  @Test
  fun `chained arithmetic`() {
    val tokens = listOf(
      Identifier("a", POSITION),
      Symbol(SymbolType.PLUS, POSITION),
      Identifier("b", POSITION),
      Symbol(SymbolType.MINUS, POSITION),
      Identifier("c", POSITION),
      Symbol(SymbolType.MINUS, POSITION),
      Identifier("d", POSITION),
      Symbol(SymbolType.MINUS, POSITION),
      Identifier("e", POSITION)
    )

    assertEquals(
      listOf(
        Subtract(
          Subtract(
            Subtract(
              Add(
                ReadVar("a"),
                ReadVar("b")
              ),
              ReadVar("c")
            ),
            ReadVar("d")
          ),
          ReadVar("e")
        )
      ),
      Parser(tokens).parse()
    )
  }

  @Test
  fun `chained arithmetic on RHS of assignment`() {
    val tokens = listOf(
      Identifier("a", POSITION),
      Symbol(SymbolType.EQUAL, POSITION),
      Identifier("b", POSITION),
      Symbol(SymbolType.MINUS, POSITION),
      Identifier("c", POSITION),
      Symbol(SymbolType.PLUS, POSITION),
      Identifier("d", POSITION)
    )

    assertEquals(
      listOf(
        AssignVar(
          "a",
          Add(
            Subtract(
              ReadVar("b"),
              ReadVar("c")
            ),
            ReadVar("d")
          )
        )
      ),
      Parser(tokens).parse()
    )
  }
}
