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
        val targetIndex = indexOf(instr.labelN)
        if (targetIndex + 1 < program.size && program[targetIndex + 1] is Jump) {
          val targetOfTarget = (program[targetIndex + 1] as Jump).labelN
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
        val targetIndex = indexOf(instr.labelN)
        targetIndex == index + 1
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

  private fun indexOf(labelN: Int): Int {
    return program.indexOf(Label(labelN))
  }
}