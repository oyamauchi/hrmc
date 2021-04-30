import ast.Parser
import ast.lex
import compiler.Compiler
import compiler.JumpOptimizer
import hrm.IntValue
import hrm.LetterValue
import hrm.Machine
import hrm.Value
import hrm.render
import hrm.values
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class GameLevelTest(private val testCase: TestCase) {

  data class TestCase(
    val name: String,
    val program: String,
    val presets: Map<Int, Value>,
    val memorySize: Int,
    val inbox: List<Value>,
    val expectedOutbox: List<Value>
  ) {
    override fun toString() = name
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = listOf(
      TestCase(
        "Fibonacci Visitor",
        """
          while {
            limit = inbox()
            a = 0
            ++a
            outbox(b = a)
            while (b <= limit) {
              outbox(b)
              temp = a + b
              a = b
              b = temp
            }
          }
        """.trimIndent(),
        mapOf(9 to IntValue(0)),
        10,
        values(5, 18),
        values(1, 1, 2, 3, 5, 1, 1, 2, 3, 5, 8, 13)
      ),
      TestCase(
        "Vowel Incinerator",
        """
          while {
            curr = inbox()
            if (curr != 'A' &&
                curr != 'E' &&
                curr != 'I' &&
                curr != 'O' &&
                curr != 'U') {
              outbox(curr)
            }
          }
        """.trimIndent(),
        mapOf(
          0 to LetterValue('A'),
          1 to LetterValue('E'),
          2 to LetterValue('I'),
          3 to LetterValue('O'),
          4 to LetterValue('U'),
          5 to IntValue(0)
        ),
        10,
        "OLDAWAKETIME".map { LetterValue(it) },
        "LDWKTM".map { LetterValue(it) }
      ),
      TestCase(
        "Sorting Floor",
        """
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
  """.trimIndent(),
        mapOf(24 to IntValue(0)),
        25,
        values(6, 4, 10, 0, 'B', 'E', 'D', 'C', 'A', 'F', 0),
        values(4, 6, 10, 'A', 'B', 'C', 'D', 'E', 'F')
      )
    )
  }

  @Test
  fun test() {
    val tokens = lex(testCase.program)
    val tree = Parser(tokens).parse()
    val compiled = Compiler(testCase.presets, testCase.memorySize).compile(tree)
    val optimized = JumpOptimizer(compiled).optimize()
    println(render(optimized))
    val outbox = Machine(optimized, testCase.presets).execute(testCase.inbox)
    assertEquals(testCase.expectedOutbox, outbox)
  }
}
