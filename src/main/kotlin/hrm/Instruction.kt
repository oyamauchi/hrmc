package hrm

/**
 * The HRM instruction set.
 *
 * In the game, not all instructions are available in all the levels, but this whole project
 * doesn't account for that. The earlier levels are simple enough that you don't need a compiler.
 */
sealed class Instruction

object Inbox : Instruction() {
  override fun toString() = "    INBOX"
}

object Outbox : Instruction() {
  override fun toString() = "    OUTBOX"
}

data class CopyFrom(
  val source: MemRef
) : Instruction() {
  constructor(index: Int) : this(FixedAddr(index))
  override fun toString() = "    COPYFROM $source"
}

data class CopyTo(
  val dest: MemRef
) : Instruction() {
  constructor(index: Int) : this(FixedAddr(index))
  override fun toString() = "    COPYTO   $dest"
}

data class Add(
  val addend: MemRef
) : Instruction() {
  override fun toString() = "    ADD      $addend"
}

data class Sub(
  val subtrahend: MemRef
) : Instruction() {
  override fun toString() = "    SUB      $subtrahend"
}

data class BumpUp(
  val operand: MemRef
) : Instruction() {
  override fun toString() = "    BUMPUP   $operand"
}

data class BumpDown(
  val operand: MemRef
) : Instruction() {
  override fun toString() = "    BUMPDOWN $operand"
}

data class Label(
  val n: Char
) : Instruction() {
  override fun toString() = "$n:"
}

interface HasTargetLabel {
  val labelN: Char
  fun withNewTarget(n: Char): Instruction
}

data class Jump(
  override val labelN: Char
) : Instruction(), HasTargetLabel {
  override fun toString() = "    JUMP     $labelN"
  override fun withNewTarget(n: Char) = copy(labelN = n)
}

data class JumpIfZero(
  override val labelN: Char
) : Instruction(), HasTargetLabel {
  override fun toString() = "    JUMPZ    $labelN"
  override fun withNewTarget(n: Char) = copy(labelN = n)
}

data class JumpIfNegative(
  override val labelN: Char
) : Instruction(), HasTargetLabel {
  override fun toString() = "    JUMPN    $labelN"
  override fun withNewTarget(n: Char) = copy(labelN = n)
}
