package hrm

sealed class Value {
  abstract operator fun plus(other: Value): Value
  abstract operator fun minus(other: Value): Value
}

data class IntValue(
  val n: Int
) : Value() {
  override fun plus(other: Value): Value {
    if (other is IntValue) {
      return IntValue(n + other.n)
    } else {
      throw MachineException("Can't add a letter to an int")
    }
  }

  override fun minus(other: Value): Value {
    if (other is IntValue) {
      return IntValue(n - other.n)
    } else {
      throw MachineException("Can't subtract a letter from an int")
    }
  }
}

data class LetterValue(
  val c: Char
) : Value() {
  override fun plus(other: Value): Value {
    throw MachineException("Can't add to a letter")
  }

  override fun minus(other: Value): Value {
    if (other is LetterValue) {
      return IntValue(c - other.c)
    } else {
      throw MachineException("Can't subtract an int from a letter")
    }
  }
}
