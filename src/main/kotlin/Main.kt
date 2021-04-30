import ast.Parser
import ast.lex
import compiler.Compiler
import compiler.JumpOptimizer
import hrm.IntValue
import hrm.LetterValue
import hrm.Machine
import hrm.Value
import hrm.render
import java.io.File
import kotlin.system.exitProcess

object Main {
  private val INT_REGEX = "0|-?[1-9]\\d*".toRegex()
  private val CHAR_REGEX = "[A-Z]".toRegex()

  private const val MEMORYSIZE_PREFIX = "// MEMORYSIZE "
  private const val PRESETS_PREFIX = "// PRESETS "

  private fun parseValue(input: String): Value {
    return when {
      // This doesn't enforce the game's limits on the value of an int
      INT_REGEX.matches(input) -> IntValue(input.toInt())
      CHAR_REGEX.matches(input) -> LetterValue(input[0])
      else -> throw RuntimeException("Invalid value $input")
    }
  }

  private fun parsePresets(input: String): Map<Int, Value> {
    return input
      .split(',')
      .map { it.split('=') }
      .map { it[0].toInt() to parseValue(it[1]) }
      .toMap()
  }

  private data class Args(
    val programText: String,
    val presets: Map<Int, Value>,
    val memorySize: Int,
    val inbox: List<Value>?,
    val printTree: Boolean
  )

  private fun parseArgs(args: Array<String>): Args? {
    var programFile: String? = null
    var inbox: List<Value>? = null
    var printTree = false

    var index = 0
    while (index < args.size) {
      when (args[index]) {
        "--execute" -> {
          index++
          inbox = args[index].split(',').map { parseValue(it) }
        }
        "--print-tree" -> printTree = true
        "--help", "-h" -> {
          println(
            """
  Human Resource Machine Compiler

  Usage: <jar> [--help|-h] [--execute VALUES] [--print-tree] PROGRAM-FILE

  --help, -h         Print this message.
  --print-tree       Print the parse tree.
  --execute VALUES   Execute the program with VALUES as the inbox, and print the outbox.

  PROGRAM-FILE should be a path to a file containing a hrmc program. It must be UTF-8
  encoded. The first two lines may be comments that start with "MEMORYSIZE" or "PRESETS".
  If they do, the rest of the line specifies the memory size or preset constants,
  respectively, to compile with. Memory size is an int. Preset constants are specified as
  a comma-separated list of equals-separated pairs; the first element is the memory index
  and the second is the value.

  Values (in presets, and in --execute) are ints, or single uppercase characters.
            """.trimIndent()
          )
          return null
        }
        else -> if (programFile == null) {
          programFile = args[index]
        } else {
          throw RuntimeException("Unrecognized argument ${args[index]}")
        }
      }
      index++
    }

    if (programFile == null) {
      throw RuntimeException("No program file specified")
    }

    val programText = File(programFile).readText(Charsets.UTF_8)
    var presets = emptyMap<Int, Value>()
    var memorySize = 0

    programText.split('\n', limit = 3).forEach {
      if (it.startsWith(MEMORYSIZE_PREFIX)) {
        memorySize = it.drop(MEMORYSIZE_PREFIX.length).trim().toInt()
      } else if (it.startsWith(PRESETS_PREFIX)) {
        presets = parsePresets(it.drop(PRESETS_PREFIX.length).trim())
      }
    }

    if (memorySize == 0) {
      throw RuntimeException("Must specify memory size")
    }

    return Args(programText, presets, memorySize, inbox, printTree)
  }

  @JvmStatic
  fun main(argsArray: Array<String>) {
    val args = parseArgs(argsArray) ?: return

    val tree = try {
      val tokens = lex(args.programText)
      val parser = Parser(tokens)
      parser.parse()
    } catch (exc: RuntimeException) {
      println(exc.message)
      exitProcess(1)
    }

    if (args.printTree) {
      println(tree)
    }

    val compiler = Compiler(args.presets, args.memorySize)
    val compiled = compiler.compile(tree)

    val optimizer = JumpOptimizer(compiled)
    val optimized = optimizer.optimize()

    println(render(optimized))

    if (args.inbox != null) {
      val machine = Machine(optimized, args.presets)
      println(machine.execute(args.inbox))
    }
  }
}
