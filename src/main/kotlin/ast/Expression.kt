package ast


sealed class Expression

object Inbox : Expression()

data class Outbox(val value: Expression) : Expression()

data class ReadVar(val variable: String) : Expression()

data class AssignVar(val variable: String, val value: Expression) : Expression()

data class ReadMem(val address: String) : Expression()

data class WriteMem(val address: String, val value: Expression) : Expression()

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

data class While(
  val condition: Condition?,
  val body: List<Expression>
) : Expression()

data class If(
  val condition: Condition,
  val trueBody: List<Expression>,
  val falseBody: List<Expression>
) : Expression()

object Terminate : Expression()

object Break: Expression()

object Continue: Expression()

///////////////////////////////////////////////////////////////////////////////
// Arithmetic
///////////////////////////////////////////////////////////////////////////////

data class Add(
  val left: Expression,
  val right: Expression
) : Expression()

data class Subtract(
  val left: Expression,
  val right: Expression
) : Expression()

data class IncVar(
  val variable: String
) : Expression()

data class IncMem(
  val address: String
) : Expression()

data class DecVar(
  val variable: String
) : Expression()

data class DecMem(
  val address: String
) : Expression()
