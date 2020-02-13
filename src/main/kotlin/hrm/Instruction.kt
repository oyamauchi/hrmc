package hrm

/**
 * The HRM instruction set.
 *
 * In the game, not all instructions are available in all the levels, but this whole project
 * doesn't account for that. The earlier levels are simple enough that you don't need a compiler.
 */
sealed class Instruction

object Inbox : Instruction() {
  override fun toString() = "-> inbox"
}

object Outbox : Instruction() {
  override fun toString() = "outbox ->"
}

data class CopyFrom(
  val source: MemRef
) : Instruction() {
  constructor(index: Int) : this(Constant(index))
  override fun toString() = "copyfrom $source"
}

data class CopyTo(
  val dest: MemRef
) : Instruction() {
  constructor(index: Int) : this(Constant(index))
  override fun toString() = "copyto $dest"
}

data class Add(
  val addend: MemRef
) : Instruction() {
  override fun toString() = "add $addend"
}

data class Sub(
  val subtrahend: MemRef
) : Instruction() {
  override fun toString() = "sub $subtrahend"
}

data class BumpUp(
  val operand: MemRef
) : Instruction() {
  override fun toString() = "bump+ $operand"
}

data class BumpDown(
  val operand: MemRef
) : Instruction() {
  override fun toString() = "bump- $operand"
}

data class Label(
  val n: Int = 0
) : Instruction() {
  override fun toString() = "  $n:"
}

interface HasTargetLabel {
  val labelN: Int
}

data class Jump(
  override val labelN: Int
) : Instruction(), HasTargetLabel {
  override fun toString() = "jump              $labelN"
}

data class JumpIfZero(
  override val labelN: Int
) : Instruction(), HasTargetLabel {
  override fun toString() = "jump if zero      $labelN"
}

data class JumpIfNegative(
  override val labelN: Int
) : Instruction(), HasTargetLabel {
  override fun toString() = "jump if negative  $labelN"
}
