import ast.Parser
import ast.lex
import compiler.Compiler
import compiler.JumpOptimizer
import hrm.IntValue
import hrm.Machine
import hrm.render
import hrm.values

fun main() {
  val program = """
    while {
      // The number of elements stored
      total = 0
      while ((curr = inbox()) != 0) {
        other = (index = total)
        --other
        while (other >= 0) {
          if (curr > *other) {
            *index = *other
          } else {
            break
          }
          --other
          --index
        }
        *index = curr
        ++total
      }

      while (total != 0) {
        --total
        outbox(*total)
      }
    }
  """.trimIndent()

  val presets = mapOf(24 to IntValue(0))

  val tokens = lex(program)
  val parser = Parser(tokens)
  val tree = parser.parse()

  println(tree)

  val compiler = Compiler(presets, 25)
  val compiled = compiler.compile(tree)

  val optimizer = JumpOptimizer(compiled)
  val optimized = optimizer.optimize()

  println(render(optimized))

  val machine = Machine(optimized, presets)
  val outbox = machine.execute(values(6, 4, 10, 0, 'B', 'E', 'D', 'C', 'A', 'F', 0))

  println(outbox)
}
