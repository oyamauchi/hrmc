package hrm


sealed class MemRef

data class Constant(
  val index: Int
) : MemRef() {
  override fun toString() = "$index"
}

data class Dereference(
  val index: Int
) : MemRef() {
  override fun toString() = "[$index]"
}
