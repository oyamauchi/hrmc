package hrm

/**
 * An implementation of the HRM architecture, as best I can figure it out.
 */
class Machine(
  private val program: List<Instruction>,
  private val presets: Map<Int, Value> = emptyMap()
) {
  companion object {
    const val MAX_STEPS = 10000
  }

  init {
    val labels = program.filterIsInstance<Label>()
    val numbers = labels.map { it.n }.toSet()

    if (labels.size != numbers.size) {
      error("All labels must have different numbers")
    }

    val targetedNumbers = program.filterIsInstance<HasTargetLabel>().map { it.labelN }
    if (!numbers.containsAll(targetedNumbers)) {
      error("All jumps must be to labels that exist")
    }
  }

  fun execute(inboxIterable: Iterable<Value>): List<Value> {
    val inbox = inboxIterable.iterator()
    val outbox = mutableListOf<Value>()

    // The amount of available memory can vary; this is more lenient than the game.
    val memory = arrayOfNulls<Value?>(100)
    presets.forEach { (index, value) -> memory[index] = value }

    var counter = 0
    var ip = 0
    var register: Value? = null

    while (true) {
      // The game doesn't seem to have this, but might as well
      if (counter++ > MAX_STEPS) {
        error("Exceeded $MAX_STEPS steps. Infinite loop?")
      }

      val instr = program[ip]
      var jumped = false

      when (instr) {
        is Inbox -> if (inbox.hasNext()) {
          register = inbox.next()
        } else {
          return outbox
        }

        is Outbox -> {
          register?.let { outbox.add(it) } ?: error("Tried to outbox nothing")
          register = null
        }

        is CopyFrom -> register = read(memory, instr.source) ?: error("Tried to read empty cell")

        is CopyTo -> register?.let { write(memory, instr.dest, it) }
          ?: error("Tried to write empty register")

        is Add -> {
          val a = register ?: error("Tried to add to empty register")
          val b = read(memory, instr.addend) ?: error("Cannot add empty ${instr.addend}")
          register = a + b
        }

        is Sub -> {
          val a = register
            ?: error("Tried to subtract from empty register")
          val b = read(memory, instr.subtrahend)
            ?: error("Cannot subtract empty ${instr.subtrahend}")
          register = a - b
        }

        is BumpUp -> {
          val original = read(memory, instr.operand)
            ?: error("Cannot bump empty ${instr.operand}")
          val bumped = original + IntValue(1)
          write(memory, instr.operand, bumped)
          register = bumped
        }

        is BumpDown -> {
          val original = read(memory, instr.operand)
            ?: error("Cannot bump empty ${instr.operand}")
          val bumped = original - IntValue(1)
          write(memory, instr.operand, bumped)
          register = bumped
        }

        is Label -> Unit

        is Jump -> {
          ip = indexOf(instr.labelN)
          jumped = true
        }

        // It's valid to conditional-jump with a letter value in register.
        // The condition is never true in that case.
        is JumpIfZero -> {
          val regNow = register ?: error("Can't conditional-jump with empty register")
          if (regNow is IntValue && regNow.n == 0) {
            ip = indexOf(instr.labelN)
            jumped = true
          } else Unit
        }

        is JumpIfNegative -> {
          val regNow = register ?: error("Can't conditional-jump with empty register")
          if (regNow is IntValue && regNow.n < 0) {
            ip = indexOf(instr.labelN)
            jumped = true
          } else Unit
        }
      }.let {}  // Force compilation error if when not exhaustive

      if (!jumped) {
        ip++
        if (ip >= program.size) {
          return outbox
        }
      }
    }
  }

  private fun read(memory: Array<Value?>, address: MemRef): Value? {
    return when (address) {
      is FixedAddr -> memory[address.index]
      is Dereference -> {
        val addressInMemory = memory[address.index] as? IntValue
          ?: error("Cannot dereference empty or non-int cell $address")
        memory[addressInMemory.n]
      }
    }
  }

  private fun write(memory: Array<Value?>, address: MemRef, value: Value) {
    when (address) {
      is FixedAddr -> memory[address.index] = value
      is Dereference -> {
        val addressInMemory = memory[address.index] as? IntValue
          ?: error("Cannot dereference empty or non-int cell $address")
        memory[addressInMemory.n] = value
      }
    }
  }

  private fun indexOf(labelN: Int): Int {
    val index = program.indexOf(Label(labelN))
    if (index < 0) {
      throw RuntimeException("Unknown label $labelN")
    }
    return index
  }

  private fun error(message: String): Nothing {
    throw MachineException(message)
  }
}
