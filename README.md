# hrmc: Human Resource Machine compiler

![Java CI with Gradle](https://github.com/oyamauchi/hrmc/workflows/Java%20CI%20with%20Gradle/badge.svg)

This is a compiler targeting the architecture used in the game
[Human Resource Machine](https://tomorrowcorporation.com/humanresourcemachine)
by Tomorrow Corporation.

## Build & Run

Requires Java 8 or later.

```
./gradlew fatJar
java -jar build/libs/hrmc-1.0-SNAPSHOT-all.jar <args>
```

Run with `--help` to see usage.

The output (minus the stuff printed by `--print-tree` and `--execute`) can be
copy-pasted into Human Resource Machine.

## Source Language

The source language is small, constrained by the design of the target machine.
Its syntax broadly resembles C.

- Statements
  - The top-level program is a sequence of statements.
  - Control-flow constructs and `outbox` are statements.
  - A brace-enclosed list of statements is also considered to be a single
    statement.
  - A single expression, followed by a semicolon, is a statement.
  - Most statements are terminated by semicolons. The exceptions are `if`,
    `while`, and brace-enclosed statement lists.
- Expressions
  - Expressions have values that can be used as part of statements and other
    expressions.
  - Assignments, arithmetic, and constants are expressions.
- Variables
  - Variables are not declared; they are created by assigning to them.
  - Variable names may contain alphabetic characters and underscores.
  - Assignment is done with an equals sign; e.g. `a = inbox()`. An assignment is
    an expression; its value is the value of the RHS, so chained assignment is
    possible, such as `a = b = c`. This is right-associative; first `c` is
    assigned to `b`, then to `a`.
  - Variables can be dereferenced using a star before the name. This represents
    an access to the memory location at the index contained in the variable.
  - Variable names and dereferenced variable names are _lvalues_, the only valid
    targets of assignments.
- Constants
  - Constants can be integers or letters. Integers are written in decimal, and
    cannot be negative. Letters are single characters enclosed in single quotes.
  - As in the game, you can't reference arbitrary constants. To use a constant,
    it must be in the constant pool when the program is compiled, which
    corresponds to being preset in memory in a level of the game.
  - The exception is the `==` and `!=` comparison operators where one operand is
    zero; these can be used without `0` being in the constant pool.
- Control flow
  - The only looping construct is `while`. Its condition is optional; if the
    condition is omitted, the loop is infinite.
  - `break` and `continue` can be used inside a loop.
  - `return` terminates the program. (There's no operand.)
  - `if` looks much like in C.
  - Conditions
    - A condition can be logical AND (`&&`), logical OR (`||`), or a
      comparision. The logical operators evaluate left-to-right, and are
      short-circuiting. They're left-associative, and `&&` binds tighter than
      `||`.
    - Comparisons must be two expressions separated by one of the comparison
      operators `==`, `!=`, `<`, `>`, `<=`, or `>=`.
    - Conditions are not valid expressions outside of the conditions of `if` and
      `while` statements. E.g. you can't do `a = (b == c)`.
    - Evaluation order within comparisons is undefined.
- Arithmetic
  - `+` and `-` operators are supported, and are left-associative.
  - `++` and `--` are supported as prefix operators only, with the same
    semantics as C. The operand must be an lvalue.
  - The semantics of addition and subtraction are as defined in the architecture
    section.
  - Evaluation order of arithmetic expressions is undefined.
- Inbox and outbox
  - Read from the inbox with `inbox()`. This is an expression.
  - Write to the outbox with `outbox(expr)`. This is a statement.

## Compilation

There is no IR; the parse tree is transformed directly into HRM code.

The output HRM code is not highly optimized, and tends to contain some obvious
inefficiencies. The most glaring problem is redundant loads: the compiler does
not track which value it currently has in the register, and it emits a load for
every read from a variable.

The only real "optimization" applied is a set of very basic transformations on
jump instructions, cleaning up some of the most obvious nonsense (like jumps to
the next instruction).

For most game levels, the most straightforward source code does not produce
output that beats the game's optimization challenges. There are some exceptions,
though, and several more levels where a small amount of manual tweaking of the
output can beat the challenges.

## Target Architecture

The machine has these components:

- A program.
- A single register that holds one value.
- An instruction pointer.
- A zero-based indexed array of memory cells. Each cell holds one value.
  Different levels of the game have different amounts of memory available.
- An inbox, a queue of values.
- An outbox, another queue of values.

Execution starts with the instruction pointer pointing at the first instruction
in the program, and the register empty. The memory generally starts out empty,
but some levels of the game have values pre-populated.

Execution terminates either:

- When an `Inbox` instruction is executed while the inbox is empty.
- When the instruction pointer goes past the end of the program.

### Values

There are two types of value: integer and letter.

- An integer can be added to or subtracted from an integer.
- A letter can be subtracted from a letter; the result is an integer, the
  numerical difference between the letters' positions in the alphabet. Adding
  letters is an error.
- Mixed-type arithmetic is an error.

### Memory operands

Most instructions have a memory operand; these are denoted by `[mem]` below.
There are two types of memory operand:

- A constant integer. This refers to the memory cell at that index.

- A dereference of a constant integer. This will read the memory cell at that
  index, and use the resulting value as the address that the instruction
  reads/writes. If the constant integer refers to an empty cell, or a cell
  containing a letter, it's an error.

### Instruction set

It's an error to use the value in the register (i.e. outbox it, write it to
memory, do arithmetic on it) while the register is empty. It's also an error to
read an empty memory cell.

After executing any non-jump instruction, or a conditional jump that is not
taken, the instruction pointer moves to the next instruction. If that puts the
pointer past the end of the program, execution terminates.

- `Inbox` reads a single value from the inbox into the register. If the inbox is
  empty, execution terminates.

- `Outbox` writes the value in the register to the outbox, and clears the
  register.

- `CopyTo [mem]` writes the value in the register to memory.

- `CopyFrom [mem]` reads a value in memory into the register.

- `Add [mem]` reads a value from memory, adds it to the value in the register,
  and stores the result in the register.

- `Sub [mem]` reads a value from memory, subtracts it from the value in the
  register (so `register - memory`) and stores the result in the register.

- `BumpUp [mem]` reads a value from memory, adds 1 to it, and stores the result
  in both the same memory cell and the register.

- `BumpDown [mem]` reads a value from memory, subtracts 1 from it, and stores
  the result in both the same memory cell and the register.

- `Jump [label]` unconditionally moves the instruction pointer to its label.

- `JumpIfZero [label]` moves the instruction pointer to its label if the value
  in the register is zero. If the value in the register is a letter, it's not an
  error; the jump is not taken.

- `JumpIfNegative [label]` moves the instruction pointer to its label if the
  value in the register is less than zero. If the value in the register is a
  letter, it's not an error; the jump is not taken.
