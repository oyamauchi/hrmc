package lang


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

sealed class Condition {
  abstract val subexpressions: List<Expression>
}

object True : Condition() {
  override val subexpressions get() = emptyList<Expression>()
}

enum class CompareOp {
  Equal,
  NotEqual,
  LessThan,
  GreaterThan
}

data class IsZero(
  val expression: Expression
) : Condition() {
  override val subexpressions get() = listOf(expression)
}

data class IsNotZero(
  val expression: Expression
) : Condition() {
  override val subexpressions get() = listOf(expression)
}

data class Compare(
  val operator: CompareOp,
  val left: Expression,
  val right: Expression
) : Condition() {
  override val subexpressions get() = listOf(left, right)
}

data class While(
  val condition: Condition,
  val body: List<Expression>
) : Expression() {
  override val subexpressions: List<Expression>
    get() = condition.subexpressions + body
}

data class If(
  val condition: Condition,
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
