package ast


sealed class Expression {
  open val subexpressions: List<Expression> = emptyList()
}

object Inbox : Expression()

data class Outbox(val value: Expression) : Expression() {
  override val subexpressions get() = listOf(value)
}

data class ReadVar(val variable: String) : Expression()

data class AssignVar(val variable: String, val value: Expression) : Expression() {
  override val subexpressions get() = listOf(value)
}

data class ReadMem(val address: String) : Expression()

data class WriteMem(val address: String, val value: Expression) : Expression() {
  override val subexpressions get() = listOf(value)
}

///////////////////////////////////////////////////////////////////////////////
// Constants
///////////////////////////////////////////////////////////////////////////////

data class IntConstant(val value: Int) : Expression()

data class LetterConstant(val value: Char) : Expression()

///////////////////////////////////////////////////////////////////////////////
// Control flow
///////////////////////////////////////////////////////////////////////////////

enum class CompareOp {
  Equal,
  NotEqual,
  LessThan,
  LessOrEqual,
  GreaterThan,
  GreaterOrEqual
}

data class Compare(
  val operator: CompareOp,
  val left: Expression,
  val right: Expression
) {
  val subexpressions get() = listOf(left, right)
}

data class While(
  val condition: Compare?,
  val body: List<Expression>
) : Expression() {
  override val subexpressions: List<Expression>
    get() = (condition?.subexpressions ?: emptyList()) + body
}

data class If(
  val condition: Compare,
  val trueBody: List<Expression>,
  val falseBody: List<Expression>
) : Expression() {
  override val subexpressions: List<Expression>
    get() = condition.subexpressions + trueBody + falseBody
}

object Terminate : Expression()


///////////////////////////////////////////////////////////////////////////////
// Arithmetic
///////////////////////////////////////////////////////////////////////////////

data class Add(
  val left: Expression,
  val right: Expression
) : Expression() {
  override val subexpressions get() = listOf(left, right)
}

data class Subtract(
  val left: Expression,
  val right: Expression
) : Expression() {
  override val subexpressions get() = listOf(left, right)
}

data class Inc(
  val variable: String
) : Expression()

data class Dec(
  val variable: String
) : Expression()
