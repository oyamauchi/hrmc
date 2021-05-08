package compiler

import hrm.*
import ast.*
import ast.Add
import ast.Inbox
import ast.Outbox
import java.util.LinkedList
import java.util.Queue
import java.util.Stack

class Compiler(
  presets: Map<Int, Value>,
  memorySize: Int
) {
  private val constantPool = presets.map { it.value to it.key }.toMap()

  // Variables are allocated to slots in memory, starting from the highest indexes and going down.
  // This is so that the low indexes can be used as a zero-indexed array if needed.
  private val availableSlots: Queue<Int> =
    LinkedList((memorySize - 1 downTo 0) - constantPool.values)
  private val variableMap: MutableMap<String, Int> = mutableMapOf()

  private val breakLabelStack: Stack<Label> = Stack()
  private val continueLabelStack: Stack<Label> = Stack()
  private var labelCounter = 'a'
  private val output = mutableListOf<Instruction>()
  private lateinit var terminateLabel: Label

  fun compile(program: List<Statement>): List<Instruction> {
    output.clear()
    breakLabelStack.clear()
    continueLabelStack.clear()
    labelCounter = 'a'
    terminateLabel = newLabel()

    program.forEach { visitStatement(it) }
    output.add(terminateLabel)

    return output.toList()
  }

  private fun getVarSlot(variable: String, create: Boolean = false): Int {
    return if (create) {
      variableMap.computeIfAbsent(variable) {
        availableSlots.poll() ?: error("Could not allocate enough variables")
      }
    } else {
      variableMap[variable] ?: error("Variable $variable not defined before use")
    }
  }

  private fun getConstSlot(value: Value): Int {
    return constantPool[value] ?: error("$value is not in presets")
  }

  private fun newLabel(): Label {
    return Label(labelCounter++)
  }

  private fun visitBinary(
    left: Expression,
    right: Expression,
    combiner: (MemRef) -> Instruction
  ): Boolean {
    var tempSlot: Int? = null
    val rightSlot: MemRef = when (right) {
      is ReadVar -> FixedAddr(getVarSlot(right.variable))
      is ReadMem -> Dereference(getVarSlot(right.address))
      is AssignVar -> {
        visit(right)
        FixedAddr(getVarSlot(right.variable))
      }
      is WriteMem -> {
        visit(right)
        Dereference(getVarSlot(right.address))
      }
      is IntConstant -> FixedAddr(getConstSlot(IntValue(right.value)))
      is LetterConstant -> FixedAddr(getConstSlot(LetterValue(right.value)))

      else -> {
        visit(right)
        tempSlot = availableSlots.poll()
        output.add(CopyTo(tempSlot))
        FixedAddr(tempSlot)
      }
    }

    visit(left)
    output.add(combiner(rightSlot))

    tempSlot?.let {
      availableSlots.offer(it)
    }

    return true
  }

  private fun visitCondition(condition: Condition, trueLabel: Label, falseLabel: Label) {
    return when (condition) {
      is AndCondition -> {
        val rhsLabel = newLabel()
        visitCondition(condition.left, rhsLabel, falseLabel)
        output.add(rhsLabel)
        visitCondition(condition.right, trueLabel, falseLabel)
      }
      is OrCondition -> {
        val rhsLabel = newLabel()
        visitCondition(condition.left, trueLabel, rhsLabel)
        output.add(rhsLabel)
        visitCondition(condition.right, trueLabel, falseLabel)
      }
      is Compare -> {
        val (left, right, testInstr, negate) = computeConditionalJump(condition)

        if (right == IntConstant(0)) {
          visit(left)
        } else {
          visitBinary(left, right) { Sub(it) }
        }

        if (negate) {
          output.add(testInstr(falseLabel))
          output.add(Jump(trueLabel.n))
        } else {
          output.add(testInstr(trueLabel))
          output.add(Jump(falseLabel.n))
        }
        Unit
      }
    }
  }

  /**
   * Computes how to codegen a jump for a comparison. This means which side of the condition to subtract from the other,
   * which jump instruction to use, and whether to negate the condition (i.e. swap the true and false destinations).
   *
   *  CONDITION     CODEGEN
   *  x == y        if  x - y == 0
   *  x != y        !if x - y == 0
   *  x < y         if  x - y < 0
   *  x <= y        !if y - x < 0
   *  x > y         if  y - x < 0
   *  x >= y        !if x - y < 0
   */
  private fun computeConditionalJump(condition: Compare): CondJump {
    return when (condition.operator) {
      CompareOp.Equal -> CondJump(condition.left, condition.right, { JumpIfZero(it.n) }, false)
      CompareOp.NotEqual -> CondJump(condition.left, condition.right, { JumpIfZero(it.n) }, true)
      CompareOp.LessThan -> CondJump(
        condition.left,
        condition.right,
        { JumpIfNegative(it.n) },
        false
      )
      CompareOp.LessOrEqual -> CondJump(
        condition.right,
        condition.left,
        { JumpIfNegative(it.n) },
        true
      )
      CompareOp.GreaterThan -> CondJump(
        condition.right,
        condition.left,
        { JumpIfNegative(it.n) },
        false
      )
      CompareOp.GreaterOrEqual -> CondJump(
        condition.left,
        condition.right,
        { JumpIfNegative(it.n) },
        true
      )
    }
  }

  private fun visitStatement(stmt: Statement) {
    when (stmt) {
      is ExpressionStatement -> {
        visit(stmt.expr)
        true
      }

      is StatementList -> {
        stmt.statements.forEach { visitStatement(it) }
        true
      }

      is Outbox -> {
        visit(stmt.value)
        output.add(hrm.Outbox)
      }

      Return -> output.add(Jump(terminateLabel.n))

      Break -> output.add(Jump(breakLabelStack.lastElement().n))

      Continue -> output.add(Jump(continueLabelStack.lastElement().n))

      is If -> {
        val trueLabel = newLabel()
        val falseLabel = newLabel()
        val after = newLabel()

        visitCondition(stmt.condition, trueLabel, falseLabel)
        output.add(trueLabel)
        visitStatement(stmt.trueBody)
        output.add(Jump(after.n))
        output.add(falseLabel)
        stmt.falseBody?.let { visitStatement(it) }

        output.add(after)
      }

      is While -> {
        val loopTop = newLabel()
        val conditionCheck = newLabel()
        val afterLoop = newLabel()

        val condition = stmt.condition

        if (condition == null) {
          breakLabelStack.push(afterLoop)
          continueLabelStack.push(loopTop)
          output.add(loopTop)
          visitStatement(stmt.body)
          output.add(Jump(loopTop.n))
          output.add(afterLoop)
          breakLabelStack.pop()
          continueLabelStack.pop()
          return
        }

        breakLabelStack.push(afterLoop)
        continueLabelStack.push(conditionCheck)

        output.add(conditionCheck)
        visitCondition(stmt.condition, loopTop, afterLoop)
        output.add(loopTop)
        visitStatement(stmt.body)
        output.add(Jump(conditionCheck.n))
        output.add(afterLoop)

        breakLabelStack.pop()
        continueLabelStack.pop()
        true
      }
    }.let {}
  }

  private fun visit(expr: Expression) {
    when (expr) {
      is Inbox -> output.add(hrm.Inbox)

      is ReadVar -> output.add(CopyFrom(getVarSlot(expr.variable)))
      is AssignVar -> {
        visit(expr.value)
        output.add(CopyTo(getVarSlot(expr.variable, create = true)))
      }

      is ReadMem -> output.add(CopyFrom(Dereference(getVarSlot(expr.address))))
      is WriteMem -> {
        visit(expr.value)
        output.add(CopyTo(Dereference(getVarSlot(expr.address))))
      }

      is IntConstant -> {
        val value = IntValue(expr.value)
        val slot = getConstSlot(value)
        output.add(CopyFrom(slot))
      }

      is LetterConstant -> {
        val value = LetterValue(expr.value)
        val slot = getConstSlot(value)
        output.add(CopyFrom(slot))
      }

      is Add -> visitBinary(expr.left, expr.right) { hrm.Add(it) }

      is Subtract -> visitBinary(expr.left, expr.right) { Sub(it) }

      is IncVar -> output.add(BumpUp(FixedAddr(getVarSlot(expr.variable))))
      is DecVar -> output.add(BumpDown(FixedAddr(getVarSlot(expr.variable))))
      is IncMem -> output.add(BumpUp(Dereference(getVarSlot(expr.address))))
      is DecMem -> output.add(BumpDown(Dereference(getVarSlot(expr.address))))
    }.let {}
  }

  /**
   * Compute left - right. Then, if negate is true, pass the false label to testInstr and output
   * the result. If negate is false, pass the true label to testInstr and output the result.
   */
  private data class CondJump(
    val left: Expression,
    val right: Expression,
    val testInstr: (Label) -> Instruction,
    val negate: Boolean
  )
}
