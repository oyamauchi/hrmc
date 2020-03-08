package compiler

import hrm.*
import ast.*
import ast.Add
import ast.Inbox
import ast.Outbox
import java.util.LinkedList
import java.util.Queue

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
  private val tempSlot: Int = availableSlots.poll()

  private var labelCounter = 0

  fun compile(program: List<Expression>): List<Instruction> {
    val output = mutableListOf<Instruction>()
    val terminateLabel = newLabel()

    allocateVars(program)

    program.forEach { visit(it, output, terminateLabel) }
    output.add(terminateLabel)

    return output
  }

  private fun allocateVars(program: List<Expression>) {
    program.forEach {
      if (it is AssignVar && it.variable !in variableMap) {
        val slot = availableSlots.poll() ?: error("Could not allocate enough variables")
        variableMap[it.variable] = slot
      }

      allocateVars(it.subexpressions)
    }
  }

  private fun newLabel(): Label {
    return Label(labelCounter++)
  }

  private fun visitBinary(
    left: Expression,
    right: Expression,
    combiner: (MemRef) -> Instruction,
    output: MutableList<Instruction>,
    terminateLabel: Label
  ): Boolean {
    val rightSlot: MemRef = when (right) {
      is ReadVar -> Constant(variableMap[right.variable]!!)
      is ReadMem -> Dereference(variableMap[right.address]!!)
      is AssignVar -> {
        visit(right, output, terminateLabel)
        Constant(variableMap[right.variable]!!)
      }
      is WriteMem -> {
        visit(right, output, terminateLabel)
        Dereference(variableMap[right.address]!!)
      }

      else -> {
        visit(right, output, terminateLabel)
        output.add(CopyTo(tempSlot))
        Constant(tempSlot)
      }
    }

    visit(left, output, terminateLabel)
    return output.add(combiner(rightSlot))
  }

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

  private fun visit(expr: Expression, output: MutableList<Instruction>, terminateLabel: Label) {
    when (expr) {
      is Inbox -> output.add(hrm.Inbox)
      is Outbox -> {
        visit(expr.value, output, terminateLabel)
        output.add(hrm.Outbox)
      }

      is ReadVar -> output.add(CopyFrom(variableMap[expr.variable]!!))
      is AssignVar -> {
        visit(expr.value, output, terminateLabel)
        output.add(CopyTo(variableMap[expr.variable]!!))
      }

      is ReadMem -> output.add(CopyFrom(Dereference(variableMap[expr.address]!!)))
      is WriteMem -> {
        visit(expr.value, output, terminateLabel)
        output.add(CopyTo(Dereference(variableMap[expr.address]!!)))
      }

      is IntConstant -> {
        val value = IntValue(expr.value)
        val slot = constantPool[value]!!
        output.add(CopyFrom(slot))
      }

      is LetterConstant -> {
        val value = LetterValue(expr.value)
        val slot = constantPool[value]!!
        output.add(CopyFrom(slot))
      }

      is While -> {
        val loopTop = newLabel()
        val conditionCheck = newLabel()
        val afterLoop = newLabel()

        val condition = expr.condition

        if (condition == null) {
          output.add(loopTop)
          expr.body.forEach {
            visit(it, output, terminateLabel)
          }
          output.add(Jump(loopTop.n))
          return
        }

        val (left, right, testInstr, negate) =
          computeConditionalJump(expr.condition)

        val rightIsZero = right == IntConstant(0)

        if (negate) {
          output.add(conditionCheck)
          if (!rightIsZero) {
            visitBinary(left, right, { Sub(it) }, output, terminateLabel)
          }
          output.add(testInstr(afterLoop))
          expr.body.forEach { visit(it, output, terminateLabel) }
          output.add(Jump(conditionCheck.n))
          output.add(afterLoop)
        } else {
          output.add(Jump(conditionCheck.n))
          output.add(loopTop)
          expr.body.forEach { visit(it, output, terminateLabel) }
          output.add(conditionCheck)
          if (!rightIsZero) {
            visitBinary(left, right, { Sub(it) }, output, terminateLabel)
          }
          output.add(testInstr(loopTop))
        }
      }

      is If -> {
        val secondBlockLabel = newLabel()
        val after = newLabel()

        val (left, right, testInstr, negate) = computeConditionalJump(expr.condition)

        if (right != IntConstant(0)) {
          visitBinary(left, right, { Sub(it) }, output, terminateLabel)
        }

        output.add(testInstr(secondBlockLabel))

        if (negate) {
          expr.trueBody.forEach { visit(it, output, terminateLabel) }
          output.add(Jump(after.n))
          output.add(secondBlockLabel)
          expr.falseBody.forEach { visit(it, output, terminateLabel) }
        } else {
          expr.falseBody.forEach { visit(it, output, terminateLabel) }
          output.add(Jump(after.n))
          output.add(secondBlockLabel)
          expr.trueBody.forEach { visit(it, output, terminateLabel) }
        }

        output.add(after)
      }

      Terminate -> output.add(Jump(terminateLabel.n))

      is Add -> visitBinary(
        expr.left, expr.right, { hrm.Add(it) }, output, terminateLabel
      )

      is Subtract -> visitBinary(
        expr.left, expr.right, { Sub(it) }, output, terminateLabel
      )

      is Inc -> output.add(BumpUp(Constant(variableMap[expr.variable]!!)))
      is Dec -> output.add(BumpDown(Constant(variableMap[expr.variable]!!)))
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
