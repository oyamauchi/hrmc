import hrm.*

data class TestCase(
  val program: List<Instruction>,
  val presetMemory: Map<Int, Value>,
  val inbox: Iterable<Value>,
  val expectedOutbox: List<Value>
)

val fibonacci = TestCase(
  listOf(
    Label(0),
    Inbox,
    CopyTo(2),
    CopyFrom(9),
    CopyTo(0),
    BumpUp(Constant(0)),
    CopyTo(1),
    Outbox,
    Label(1),
    CopyFrom(2),
    Sub(Constant(1)),
    JumpIfNegative(0),
    CopyFrom(1),
    Outbox,
    CopyFrom(1),
    CopyTo(3),
    Add(Constant(0)),
    CopyTo(1),
    CopyFrom(3),
    CopyTo(0),
    Jump(1)
  ),
  mapOf(9 to IntValue(0)),
  listOf(IntValue(5), IntValue(18)),
  listOf(1, 1, 2, 3, 5, 1, 1, 2, 3, 5, 8, 13).map { IntValue(it) }
)

val vowels = TestCase(
  listOf(
    Label(0),
    Inbox,
    CopyTo(6),
    Sub(Constant(0)),
    JumpIfZero(0),
    CopyFrom(6),
    Sub(Constant(1)),
    JumpIfZero(0),
    CopyFrom(6),
    Sub(Constant(2)),
    JumpIfZero(0),
    CopyFrom(6),
    Sub(Constant(3)),
    JumpIfZero(0),
    CopyFrom(6),
    Sub(Constant(4)),
    JumpIfZero(0),
    CopyFrom(6),
    Outbox,
    Jump(0)
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
)

fun main() {
  val (program, presets, inbox, expectedOut) = vowels

  val machine = Machine(program, presets)
  val outbox = machine.execute(inbox)

  println(outbox)
  println(expectedOut)
}