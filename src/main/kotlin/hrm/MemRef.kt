package hrm


sealed class MemRef

data class FixedAddr(
  val index: Int
) : MemRef() {
  override fun toString() = "$index"
}

data class Dereference(
  val index: Int
) : MemRef() {
  override fun toString() = "[$index]"
}
