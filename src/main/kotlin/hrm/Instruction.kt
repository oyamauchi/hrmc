package hrm

sealed class Instruction

object Inbox : Instruction()

object Outbox : Instruction()

data class CopyFrom(
    val source: MemRef
) : Instruction()

data class CopyTo(
    val dest: MemRef
) : Instruction()

data class Add(
    val addend: MemRef
) : Instruction()

data class Sub(
    val subtrahend: MemRef
) : Instruction()

data class Label(
    val n: Int = 0
): Instruction()

data class Jump(
    val dest: Label
) : Instruction()

data class JumpIfZero(
    val dest: Label
) : Instruction()

data class JumpIfNegative(
    val dest: Label
) : Instruction()
