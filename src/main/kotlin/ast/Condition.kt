package ast

enum class CompareOp {
  Equal,
  NotEqual,
  LessThan,
  LessOrEqual,
  GreaterThan,
  GreaterOrEqual
}

sealed class Condition

data class OrCondition(
  val left: Condition,
  val right: Condition
): Condition()

data class AndCondition(
  val left: Condition,
  val right: Condition
): Condition()

data class Compare(
  val operator: CompareOp,
  val left: Expression,
  val right: Expression
): Condition()
