# Day 19
Another machine simulation, building on [Day 16](../src/day16)'s. While that allows for a more complex problem to be stated with fewer new details to outline, the problem is sufficiently different that I'm not sure how much code I'll be able to reuse.

We don't need to decipher the opcodes this time so I modelled them with an enum with attached names and execution functions.
```
enum class OpCode(val asm: String, val exec: (Registers, Arguments) -> Unit) {
	ADDR("addr", { r, (a, b, c) -> r[c] = r[a] + r[b] }),
	ADDI("addi", { r, (a, b, c) -> r[c] = r[a] +   b  }),
	...
}
```

The program itself is then just the Instruction Pointer register binding followed by a list of instructions.
```
data class Instruction(val opcode: OpCode, val arguments: Arguments)

data class Program(val ipBinding: Int, val instructions: List<Instruction>)
```

And the parser is straightforward as ever:
```
fun parse(input: String): Program {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
    val opcode = or(OpCode.values().map { opcode -> string(opcode.asm).retn(opcode) })
 	val arguments = sequence(integer, WHITESPACES.next(integer), WHITESPACES.next(integer), ::Arguments)
 	val operation: Parser<Instruction> = sequence(opcode, WHITESPACES.next(arguments), ::Instruction)
    val bindip = string("#ip ").next(integer)
 	val program = sequence(bindip, WHITESPACES.next(operation.sepBy(WHITESPACES)), ::Program)
 	return program.parse(input.trim())
}
```

Executing the program is just a loop running until the IP is out of range.
```
fun execute(program: Program): Registers {
	val registers = Registers(ipBinding = program.ipBinding)

	while (program.instructions.indices.contains(registers.ip)) {
		val instr = program.instructions[registers.ip]
		instr.opcode.exec(registers, instr.arguments)
		registers.step()
	}

	return registers
}
```

And part 1 is trivial:

```
fun part1(program: Program) = execute(program)[0]
```

## Part 2
The second part was altogether a different problem. It runs _for a very very long time_, so you can't get the answer just by executing it. You have to decipher the code and work out what it's doing, then compute the result without actually running it. To cut a long story short, it initialises R3 with a huge number, then uses an extremely inefficient algorithm to find numbers which are factors of that number, adding the factors up as it goes.

In analysing the code I noted that the section which computes the very large number ends with the final instruction which is a jump to the main loop. I dropped that instruction and executed the program to get the big number.
```
fun part2(program: Program): Int {
	val modifiedProgram = Program(program.ipBinding, program.instructions.dropLast(1))
	val numberToFactor = execute(modifiedProgram, r0=1)[3]
```

Then I just reimplemented the algorithm in Kotlin with a more efficient method:
```
	println("numberToFactor: $numberToFactor")
	val sumOfDivisions = (1..numberToFactor).asSequence().filter { i -> numberToFactor % i == 0 }.sum()
	return sumOfDivisions
}
```
