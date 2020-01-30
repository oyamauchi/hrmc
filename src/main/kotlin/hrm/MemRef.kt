package hrm


sealed class MemRef {
  abstract fun read(memory: Array<Any?>): Any?
  abstract fun write(memory: Array<Any?>, value: Any?)
}

data class Constant(
  val index: Int
) : MemRef() {
  override fun read(memory: Array<Any?>) = memory[index]
  override fun write(memory: Array<Any?>, value: Any?) {
    memory[index] = value
  }
}

data class Dereference(
  val index: Int
) : MemRef() {
  override fun read(memory: Array<Any?>) = memory[memory[index] as Int]
  override fun write(memory: Array<Any?>, value: Any?) {
    memory[memory[index] as Int] = value
  }
}
