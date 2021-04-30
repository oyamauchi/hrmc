package compiler

import ast.Add
import ast.Inbox
import ast.IntConstant
import ast.Outbox
import hrm.CopyTo
import hrm.FixedAddr
import hrm.Label
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompilerTest {
  @Test
  fun `multiple temps`() {
    val program = listOf(
      Outbox(Add(Add(Inbox, Inbox), Inbox))
    )

    val compiler = Compiler(emptyMap(), 10)
    val compiled = compiler.compile(program)

    assertEquals(
      listOf(
        hrm.Inbox,
        CopyTo(9),
        hrm.Inbox,
        CopyTo(8),
        hrm.Inbox,
        hrm.Add(FixedAddr(8)),
        hrm.Add(FixedAddr(9)),
        hrm.Outbox
      ),
      compiled.filter { it !is Label }
    )
  }

  @Test
  fun `nonexistent constant`() {
    val program = listOf(Outbox(IntConstant(123)))
    val compiler = Compiler(emptyMap(), 10)
    val exc = assertFailsWith<IllegalStateException> { compiler.compile(program) }
    assertEquals("IntValue(n=123) is not in presets", exc.message)
  }
}
