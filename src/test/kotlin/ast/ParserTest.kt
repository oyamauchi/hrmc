package ast

import org.junit.Test
import kotlin.test.assertEquals

class ParserTest {
  @Test
  fun `chained arithmetic`() {
    val tokens = listOf(
      Identifier("a"),
      Symbol(SymbolType.PLUS),
      Identifier("b"),
      Symbol(SymbolType.MINUS),
      Identifier("c"),
      Symbol(SymbolType.MINUS),
      Identifier("d"),
      Symbol(SymbolType.MINUS),
      Identifier("e")
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
      Identifier("a"),
      Symbol(SymbolType.EQUAL),
      Identifier("b"),
      Symbol(SymbolType.MINUS),
      Identifier("c"),
      Symbol(SymbolType.PLUS),
      Identifier("d")
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
