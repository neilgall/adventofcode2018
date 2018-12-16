package adventofcode2018.day16

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Registers(val regs: IntArray) {
	constructor(ints: List<Int>): this(IntArray(ints.size) { i -> ints[i] })

	fun copy(): Registers = Registers(regs.toList())

	override fun equals(other: Any?) =
		other is Registers && regs.size == other.regs.size && regs.zip(other.regs).all { (a,b) -> a == b }

	operator fun get(i: Int): Int = regs[i]
	operator fun set(i: Int, v: Int): Unit { regs[i] = v }

	override fun toString(): String = "[${regs.map(Int::toString).joinToString(", ")}]"
}

data class Arguments(val a: Int, val b: Int, val c: Int) {
	override fun toString(): String = "$a,$b,$c"
}

data class Instruction(val opcode: Int, val arguments: Arguments)

data class Sample(val before: Registers, val instruction: Instruction, val after: Registers)

data class Input(val samples: List<Sample>, val program: List<Instruction>)

// Parsing

fun parse(input: String): Input {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
 	val registers = integer.sepBy(string(", ")).between(string("["), string("]")).map(::Registers)
 	val before = string("Before: ").next(registers)
 	val after = string("After:  ").next(registers)
 	val arguments = sequence(integer, WHITESPACES.next(integer), WHITESPACES.next(integer), ::Arguments)
 	val instruction = sequence(integer, WHITESPACES.next(arguments), ::Instruction)
 	val sample = sequence(before, WHITESPACES.next(instruction), WHITESPACES.next(after), ::Sample)
 	val samples = sample.sepBy(WHITESPACES)
 	val program = instruction.sepBy(WHITESPACES)

 	return sequence(samples.followedBy(WHITESPACES), program, ::Input).parse(input.trim())
}

// Machine Simulation

data class Operation(val name: String, val run: (Registers, Arguments) -> Unit) {
	override fun toString(): String = name
}

val operations: List<Operation> = listOf(
	Operation("addr", { r, (a, b, c) -> r[c] = r[a] + r[b] }),
	Operation("addi", { r, (a, b, c) -> r[c] = r[a] +   b  }),

	Operation("mulr", { r, (a, b, c) -> r[c] = r[a] * r[b] }),
	Operation("muli", { r, (a, b, c) -> r[c] = r[a] *   b  }),

	Operation("banr", { r, (a, b, c) -> r[c] = r[a] and r[b] }),
	Operation("bani", { r, (a, b, c) -> r[c] = r[a] and   b  }),

	Operation("borr", { r, (a, b, c) -> r[c] = r[a] or r[b] }),
	Operation("bori", { r, (a, b, c) -> r[c] = r[a] or   b  }),

	Operation("setr", { r, (a, _, c) -> r[c] = r[a] }),
	Operation("seti", { r, (a, _, c) -> r[c] =   a  }),

	Operation("gtri", { r, (a, b, c) -> r[c] = if (r[a] >   b ) 1 else 0 }),
	Operation("gtir", { r, (a, b, c) -> r[c] = if (  a  > r[b]) 1 else 0 }),
	Operation("gtrr", { r, (a, b, c) -> r[c] = if (r[a] > r[b]) 1 else 0 }),

	Operation("eqri", { r, (a, b, c) -> r[c] = if (r[a] ==   b ) 1 else 0 }),
	Operation("eqir", { r, (a, b, c) -> r[c] = if (  a  == r[b]) 1 else 0 }),
	Operation("eqrr", { r, (a, b, c) -> r[c] = if (r[a] == r[b]) 1 else 0 })
)

// Part 1

class PureOperation(val name: String, val run: (Registers, Arguments) -> Registers) {
	constructor(op: Operation): this(op.name, { r, a -> 
		val r_ = r.copy()
		op.run(r_, a)
		// println("run $op($a): $r -> $r_")
		r_
	})

	override fun toString(): String = name
}

val pureOperations = operations.map(::PureOperation)

fun Sample.test(): Int =
	pureOperations.count { op -> op.run(before, instruction.arguments) == after }

fun part1(input: Input): Int =
	input.samples.map(Sample::test).count { it >= 3 }


fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
}
