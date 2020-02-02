package hrm

/**
 * The HRM instruction set.
 *
 * In the game, not all instructions are available in all the levels, but this whole project
 * doesn't account for that. The earlier levels are simple enough that you don't need a compiler.
 */
sealed class Instruction

object Inbox : Instruction()

object Outbox : Instruction()

data class CopyFrom(
  val source: MemRef
) : Instruction() {
  constructor(index: Int) : this(Constant(index))
}

data class CopyTo(
  val dest: MemRef
) : Instruction() {
  constructor(index: Int) : this(Constant(index))
}

data class Add(
  val addend: MemRef
) : Instruction()

data class Sub(
  val subtrahend: MemRef
) : Instruction()

data class BumpUp(
  val operand: MemRef
) : Instruction()

data class BumpDown(
  val operand: MemRef
) : Instruction()

data class Label(
  val n: Int = 0
) : Instruction()

data class Jump(
  val labelN: Int
) : Instruction()

data class JumpIfZero(
  val labelN: Int
) : Instruction()

data class JumpIfNegative(
  val labelN: Int
) : Instruction()
