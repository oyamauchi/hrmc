package hrm

import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class GameLevelTest(
  private val testCase: TestCase
) {

  data class TestCase(
    val name: String,
    val program: List<Instruction>,
    val presets: Map<Int, Value>,
    val inbox: List<Value>,
    val expectedOutbox: List<Value>
  ) {
    override fun toString() = name
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = listOf(
      TestCase(
        "Vowel Incinerator",
        listOf(
          Label('a'),
          Inbox,
          CopyTo(6),
          Sub(FixedAddr(0)),
          JumpIfZero('a'),
          CopyFrom(6),
          Sub(FixedAddr(1)),
          JumpIfZero('a'),
          CopyFrom(6),
          Sub(FixedAddr(2)),
          JumpIfZero('a'),
          CopyFrom(6),
          Sub(FixedAddr(3)),
          JumpIfZero('a'),
          CopyFrom(6),
          Sub(FixedAddr(4)),
          JumpIfZero('a'),
          CopyFrom(6),
          Outbox,
          Jump('a')
        ),
        mapOf(
          0 to LetterValue('A'),
          1 to LetterValue('E'),
          2 to LetterValue('I'),
          3 to LetterValue('O'),
          4 to LetterValue('U'),
          5 to IntValue(0)
        ),
        "OLDAWAKETIME".map { LetterValue(it) },
        "LDWKTM".map { LetterValue(it) }
      ),
      TestCase(
        "Fibonacci Visitor",
        listOf(
          Label('a'),
          Inbox,
          CopyTo(2),
          CopyFrom(9),
          CopyTo(0),
          BumpUp(FixedAddr(0)),
          CopyTo(1),
          Outbox,
          Label('b'),
          CopyFrom(2),
          Sub(FixedAddr(1)),
          JumpIfNegative('a'),
          CopyFrom(1),
          Outbox,
          CopyFrom(1),
          CopyTo(3),
          Add(FixedAddr(0)),
          CopyTo(1),
          CopyFrom(3),
          CopyTo(0),
          Jump('b')
        ),
        mapOf(9 to IntValue(0)),
        listOf(IntValue(5), IntValue(18)),
        listOf(1, 1, 2, 3, 5, 1, 1, 2, 3, 5, 8, 13).map { IntValue(it) }
      )
    )
  }

  @Test
  fun test() {
    val machine = Machine(testCase.program, testCase.presets)
    val outbox = machine.execute(testCase.inbox)
    assertEquals(testCase.expectedOutbox, outbox)
  }
}
