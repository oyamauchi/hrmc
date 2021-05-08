package ast

sealed class Statement

data class Outbox(val value: Expression): Statement()

data class If(val condition: Condition, val trueBody: Statement, val falseBody: Statement?): Statement()

data class While(val condition: Condition?, val body: Statement): Statement()

object Break: Statement()

object Continue: Statement()

object Return: Statement()

data class StatementList(val statements: List<Statement>): Statement()

data class ExpressionStatement(val expr: Expression): Statement()
