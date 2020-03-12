package hrm

import kotlin.test.Test
import kotlin.test.assertEquals

class MachineTest {

  @Test(expected = MachineException::class)
  fun `duplicate labels`() {
    Machine(listOf(Label(0), Inbox, Label(0)))
  }

  @Test(expected = MachineException::class)
  fun `nonexistent label`() {
    Machine(listOf(Jump(0)))
  }

  @Test(expected = MachineException::class, timeout = 10000L)
  fun `infinite loop`() {
    Machine(listOf(Label(0), Jump(0))).execute(values(1, 2, 3))
  }

  @Test(expected = MachineException::class)
  fun `can't outbox empty`() {
    Machine(listOf(Outbox)).execute(values(1, 2, 3))
  }

  @Test
  fun `inbox and outbox`() {
    val m = Machine(listOf(Label(0), Inbox, Outbox, Jump(0)))

    assertEquals(emptyList(), m.execute(emptyList()))
    assertEquals(values(1, 2, 'T'), m.execute(values(1, 2, 'T')))
  }
}
