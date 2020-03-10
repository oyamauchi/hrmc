package compiler

import hrm.*
import ast.*
import ast.Add
import ast.Inbox
import ast.Outbox
import java.util.LinkedList
import java.util.Queue

class Compiler(
  presets: Map<Int, hrm.Value>,
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
  private val output = mutableListOf<Instruction>()
  private lateinit var terminateLabel: Label

  fun compile(program: List<Expression>): List<Instruction> {
    output.clear()
    labelCounter = 0
    terminateLabel = newLabel()

    program.forEach { visit(it) }
    output.add(terminateLabel)

    return output.toList()
  }

  private fun getVarSlot(variable: String): Int {
    return variableMap.computeIfAbsent(variable) {
      availableSlots.poll() ?: error("Could not allocate enough variables")
    }
  }

  private fun newLabel(): Label {
    return Label(labelCounter++)
  }

  private fun visitBinary(
    left: Expression,
    right: Expression,
    combiner: (MemRef) -> Instruction
  ): Boolean {
    val rightSlot: MemRef = when (right) {
      is ReadVar -> Constant(getVarSlot(right.variable))
      is ReadMem -> Dereference(getVarSlot(right.address))
      is AssignVar -> {
        visit(right)
        Constant(getVarSlot(right.variable))
      }
      is WriteMem -> {
        visit(right)
        Dereference(getVarSlot(right.address))
      }

      else -> {
        visit(right)
        output.add(CopyTo(tempSlot))
        Constant(tempSlot)
      }
    }

    visit(left)
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

  private fun visit(expr: Expression) {
    when (expr) {
      is Inbox -> output.add(hrm.Inbox)
      is Outbox -> {
        visit(expr.value)
        output.add(hrm.Outbox)
      }

      is ReadVar -> output.add(CopyFrom(getVarSlot(expr.variable)))
      is AssignVar -> {
        visit(expr.value)
        output.add(CopyTo(getVarSlot(expr.variable)))
      }

      is ReadMem -> output.add(CopyFrom(Dereference(getVarSlot(expr.address))))
      is WriteMem -> {
        visit(expr.value)
        output.add(CopyTo(Dereference(getVarSlot(expr.address))))
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
          expr.body.forEach { visit(it) }
          output.add(Jump(loopTop.n))
          return
        }

        val (left, right, testInstr, negate) =
          computeConditionalJump(expr.condition)

        val rightIsZero = right == IntConstant(0)

        if (negate) {
          output.add(conditionCheck)
          if (!rightIsZero) {
            visitBinary(left, right) { Sub(it) }
          }
          output.add(testInstr(afterLoop))
          expr.body.forEach { visit(it) }
          output.add(Jump(conditionCheck.n))
          output.add(afterLoop)
        } else {
          output.add(Jump(conditionCheck.n))
          output.add(loopTop)
          expr.body.forEach { visit(it) }
          output.add(conditionCheck)
          if (!rightIsZero) {
            visitBinary(left, right) { Sub(it) }
          }
          output.add(testInstr(loopTop))
        }
      }

      is If -> {
        val secondBlockLabel = newLabel()
        val after = newLabel()

        val (left, right, testInstr, negate) = computeConditionalJump(expr.condition)

        if (right != IntConstant(0)) {
          visitBinary(left, right) { Sub(it) }
        }

        output.add(testInstr(secondBlockLabel))

        if (negate) {
          expr.trueBody.forEach { visit(it) }
          output.add(Jump(after.n))
          output.add(secondBlockLabel)
          expr.falseBody.forEach { visit(it) }
        } else {
          expr.falseBody.forEach { visit(it) }
          output.add(Jump(after.n))
          output.add(secondBlockLabel)
          expr.trueBody.forEach { visit(it) }
        }

        output.add(after)
      }

      Terminate -> output.add(Jump(terminateLabel.n))

      is Add -> visitBinary(expr.left, expr.right) { hrm.Add(it) }

      is Subtract -> visitBinary(expr.left, expr.right) { Sub(it) }

      is Inc -> output.add(BumpUp(Constant(getVarSlot(expr.variable))))
      is Dec -> output.add(BumpDown(Constant(getVarSlot(expr.variable))))
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
