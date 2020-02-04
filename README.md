# hrmc: Human Resource Machine compiler

This is a compiler backend targeting the architecture used in the game
[Human Resource Machine](https://tomorrowcorporation.com/humanresourcemachine)
by Tomorrow Corporation.

## Architecture

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
- After the last instruction in the program is executed, as long as that
  instruction is not a jump, or is a conditional jump that is not taken.

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
