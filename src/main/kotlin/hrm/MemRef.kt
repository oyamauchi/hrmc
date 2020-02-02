package hrm


sealed class MemRef

data class Constant(
  val index: Int
) : MemRef()

data class Dereference(
  val index: Int
) : MemRef()
