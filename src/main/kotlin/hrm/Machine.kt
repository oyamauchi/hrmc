package hrm

import java.util.*

class Machine(
  private val program: List<Instruction>
) {
  init {
    val labels = program.filterIsInstance<Label>()
    val numbers = labels.map { it.n }.toSet()
    if (labels.size != numbers.size) {
      throw RuntimeException("All labels must have different numbers")
    }
  }

  fun execute(inbox: Queue<Any>): List<Any> {
    val outbox = mutableListOf<Any>()

    val memory = arrayOfNulls<Any?>(100)

    var ip = 0
    var register: Any? = null

    loop@ while (true) {
      val instr = program[ip]
      var jumped = false

      when (instr) {
        is Inbox -> if (inbox.isEmpty()) {
          break@loop
        } else {
          register = inbox.poll()
        }

        is Outbox -> register?.let { outbox.add(it) } ?: error("Tried to outbox nothing")

        is CopyFrom -> register = instr.source.read(memory) ?: error("Tried to read nothing")

        is CopyTo -> instr.dest.write(memory, register)

        is Add -> {
          val a = register as? Int ?: error("Cannot add non-int $register")
          val b = instr.addend.read(memory) as? Int ?: error("Cannot add non-int ${instr.addend}")
          register = a + b
        }

        is Sub -> {
          val a = register as? Int ?: error("Cannot subtract non-int $register")
          val b = instr.subtrahend.read(memory) as? Int ?: error("Cannot subtract non-int ${instr.subtrahend}")
          register = a - b
        }

        is Label -> {}

        is Jump -> {
          ip = indexOf(instr.dest)
          jumped = true
        }

        is JumpIfZero -> if (register == 0) {
          ip = indexOf(instr.dest)
          jumped = true
        }

        is JumpIfNegative -> if (register < 0) {
          ip = indexOf(instr.dest)
          jumped = true
        }
      }

      if (!jumped) {
        ip++
        if (ip > program.size) {
          break@loop
        }
      }
    }

    return outbox
  }

  private fun indexOf(label: Label): Int {
    val index = program.indexOf(label)
    if (index < 0) {
      throw RuntimeException("Unknown label $label")
    }
    return index
  }

  private fun error(message: String): Nothing {
    throw RuntimeException(message)
  }
}
