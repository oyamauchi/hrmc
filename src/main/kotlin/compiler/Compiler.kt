package compiler

import hrm.*
import lang.*
import lang.Add
import lang.Inbox
import lang.Outbox
import java.util.LinkedList
import java.util.Queue

class Compiler(
  private val constantPool: Map<Value, Int>,
  memorySize: Int
) {
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

        output.add(Jump(conditionCheck.n))
        output.add(loopTop)
        expr.body.forEach {
          visit(it, output, terminateLabel)
        }

        output.add(conditionCheck)

        when (expr.condition) {
          is True -> output.add(Jump(loopTop.n))
          is IsZero -> {
            visit(expr.condition.expression, output, terminateLabel)
            output.add(JumpIfZero(loopTop.n))
          }
          is IsNotZero -> {
            visit(expr.condition.expression, output, terminateLabel)
            output.add(JumpIfZero(afterLoop.n))
            output.add(Jump(loopTop.n))
          }
          is Compare -> {
            visit(expr.condition.right, output, terminateLabel)
            output.add(CopyTo(tempSlot))

            visit(expr.condition.left, output, terminateLabel)
            output.add(Sub(Constant(tempSlot)))

            when (expr.condition.operator) {
              CompareOp.Equal -> output.add(JumpIfZero(loopTop.n))
              CompareOp.LessThan -> output.add(JumpIfNegative(loopTop.n))

              CompareOp.NotEqual -> {
                output.add(JumpIfZero(afterLoop.n))
                output.add(Jump(loopTop.n))
              }
              CompareOp.GreaterThan -> TODO()
            }
          }
        }.let{}

        output.add(afterLoop)
      }

      is If -> {
        val secondBlockLabel = newLabel()
        val after = newLabel()

        val (jumpToSecond, secondBlockIsTrue) =when (expr.condition) {
          is True -> error("Don't do If(True)")

          is IsZero -> {
            visit(expr.condition.expression, output, terminateLabel)
            Pair(JumpIfZero(secondBlockLabel.n), true)
          }
          is IsNotZero -> {
            visit(expr.condition.expression, output, terminateLabel)
            Pair(JumpIfZero(secondBlockLabel.n), false)
          }
          is Compare -> {
            visit(expr.condition.right, output, terminateLabel)
            output.add(CopyTo(tempSlot))

            visit(expr.condition.left, output, terminateLabel)
            output.add(Sub(Constant(tempSlot)))

            when (expr.condition.operator) {
              CompareOp.Equal -> Pair(JumpIfZero(secondBlockLabel.n) as Instruction, true)
              CompareOp.NotEqual -> Pair(JumpIfZero(secondBlockLabel.n), false)
              CompareOp.LessThan -> Pair(JumpIfNegative(secondBlockLabel.n), true)
              CompareOp.GreaterThan -> Pair(JumpIfNegative(secondBlockLabel.n), false)
            }
          }
        }

        output.add(jumpToSecond)

        val (firstBlock, secondBlock) = if (secondBlockIsTrue) {
          Pair(expr.falseBody, expr.trueBody)
        } else {
          Pair(expr.trueBody, expr.falseBody)
        }
        firstBlock.forEach { visit(it, output, terminateLabel) }
        output.add(Jump(after.n))
        output.add(secondBlockLabel)
        secondBlock.forEach { visit(it, output, terminateLabel) }

        output.add(after)
      }

      Terminate -> output.add(Jump(terminateLabel.n))

      is Add -> {
        visit(expr.right, output, terminateLabel)
        output.add(CopyTo(tempSlot))

        visit(expr.left, output, terminateLabel)
        output.add(hrm.Add(Constant(tempSlot)))
      }
      is Subtract -> {
        visit(expr.right, output, terminateLabel)
        output.add(CopyTo(tempSlot))

        visit(expr.left, output, terminateLabel)
        output.add(Sub(Constant(tempSlot)))
      }

      is Inc -> output.add(BumpUp(Constant(variableMap[expr.variable]!!)))
      is Dec -> output.add(BumpDown(Constant(variableMap[expr.variable]!!)))
    }.let {}
  }
}