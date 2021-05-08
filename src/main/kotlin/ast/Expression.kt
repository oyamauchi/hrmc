package ast


sealed class Expression

object Inbox : Expression()

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
