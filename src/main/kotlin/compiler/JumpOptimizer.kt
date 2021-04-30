package compiler

import hrm.HasTargetLabel
import hrm.Instruction
import hrm.Jump
import hrm.Label

class JumpOptimizer(
  program: List<Instruction>
) {
  private val program: MutableList<Instruction> = program.toMutableList()

  fun optimize(): List<Instruction> {
    var changed: Boolean
    do {
      changed = fixJumpsToJumps()
      changed = changed || removeJumpsToNext()
      changed = changed || removeUnreachableCode()
      changed = changed || removeUnusedLabels()
    } while (changed)

    return program
  }

  /**
   * Any jump to an unconditional jump J can have its target replaced with J's target.
   */
  private fun fixJumpsToJumps(): Boolean {
    var changed = false
    program.forEachIndexed { index, instr ->
      if (instr is HasTargetLabel) {
        val targetIndex = indexOfFirstNonLabelAfter(indexOf(instr.labelN))

        // Don't do this for self-jumps; that will cause an infinite loop
        if (targetIndex != null && targetIndex != index && program[targetIndex] is Jump) {
          val targetOfTarget = (program[targetIndex] as Jump).labelN
          program[index] = instr.withNewTarget(targetOfTarget)
          changed = true
        }
      }
    }

    return changed
  }

  private fun removeJumpsToNext(): Boolean {
    return removeMatching { index, instr ->
      if (instr is HasTargetLabel) {
        val targetIndex = indexOfFirstNonLabelAfter(indexOf(instr.labelN))
        val nextIndex = indexOfFirstNonLabelAfter(index)
        targetIndex == nextIndex
      } else {
        false
      }
    }
  }

  private fun removeUnreachableCode(): Boolean {
    var reachable = true
    val toBeRemoved = mutableSetOf<Int>()
    program.forEachIndexed { index, instr ->
      if (instr is Label) {
        reachable = true
      }

      if (!reachable) {
        toBeRemoved.add(index)
      }

      if (instr is Jump) {
        reachable = false
      }
    }

    toBeRemoved.reversed().forEach { program.removeAt(it) }
    return toBeRemoved.isNotEmpty()
  }

  private fun removeUnusedLabels(): Boolean {
    val usedLabels = program.filterIsInstance<HasTargetLabel>().map { it.labelN }.toSet()
    return removeMatching { _, instr -> instr is Label && instr.n !in usedLabels }
  }

  private fun removeMatching(predicate: (Int, Instruction) -> Boolean): Boolean {
    val indexes = program.mapIndexedNotNull { index, instr ->
      if (predicate(index, instr)) index else null
    }

    indexes.reversed().forEach { program.removeAt(it) }
    return indexes.isNotEmpty()
  }

  private fun indexOf(labelN: Char): Int {
    return program.indexOf(Label(labelN))
  }

  private fun indexOfFirstNonLabelAfter(index: Int): Int? {
    val result = program.drop(index + 1).indexOfFirst { it !is Label }
    return if (result == -1) {
      null
    } else {
      result + index + 1
    }
  }
}