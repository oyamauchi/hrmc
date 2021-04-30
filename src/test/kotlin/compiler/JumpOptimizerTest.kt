package compiler

import hrm.Inbox
import hrm.Jump
import hrm.Label
import org.junit.Test
import kotlin.test.assertEquals

class JumpOptimizerTest {
  @Test(timeout = 1000)
  fun `avoid infinite loop in optimizer`() {
    // Make sure the jump-to-jump optimizer recognizes this
    val opt = JumpOptimizer(
      listOf(
        Label('a'),
        Jump('a')
      )
    )

    assertEquals(
      listOf(Label('a'), Jump('a')),
      opt.optimize()
    )
  }

  @Test
  fun `jumps to next with intervening labels`() {
    val opt = JumpOptimizer(
      listOf(
        Inbox,
        Jump('b'),
        Label('a'),
        Label('b'),
        Inbox,
        Jump('a')
      )
    )

    assertEquals(
      listOf(Inbox, Label('a'), Inbox, Jump('a')),
      opt.optimize()
    )
  }
}
