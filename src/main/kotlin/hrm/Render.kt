package hrm

fun render(program: List<Instruction>, variables: Map<String, Int>? = null): String {
  val builder = StringBuilder()
  builder.append("-- HUMAN RESOURCE MACHINE PROGRAM --\n")

  variables?.entries?.sortedBy { it.value }?.forEach {
    builder.append("-- Slot ${it.value}: ${it.key}\n")
  }

  builder.append('\n')

  program.forEach {
    builder.append(it.toString())
    builder.append('\n')
  }

  return builder.toString()
}
