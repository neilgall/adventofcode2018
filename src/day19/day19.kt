package adventofcode2018.day19

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Registers(val regs: IntArray = intArrayOf(0, 0, 0, 0, 0, 0), val ipBinding: Int) {
	constructor(ints: List<Int>, ipBinding: Int): this(IntArray(ints.size) { i -> ints[i] }, ipBinding)

	fun copy(): Registers = Registers(regs.toList(), ipBinding)

	override fun equals(other: Any?) =
		other is Registers && regs.size == other.regs.size && regs.zip(other.regs).all { (a,b) -> a == b }

	operator fun get(i: Int): Int = regs[i]
	operator fun set(i: Int, v: Int): Unit { regs[i] = v }

	override fun toString(): String = "ip=$ipBinding [${regs.map(Int::toString).joinToString(", ")}]"
}

data class Arguments(val a: Int, val b: Int, val c: Int) {
	override fun toString(): String = "$a,$b,$c"
}

enum class OpCode(val asm: String, val exec: (Registers, Arguments) -> Unit) {
	ADDR("addr", { r, (a, b, c) -> r[c] = r[a] + r[b] }),
	ADDI("addi", { r, (a, b, c) -> r[c] = r[a] +   b  }),

	MULR("mulr", { r, (a, b, c) -> r[c] = r[a] * r[b] }),
	MULI("muli", { r, (a, b, c) -> r[c] = r[a] *   b  }),

	BANR("banr", { r, (a, b, c) -> r[c] = r[a] and r[b] }),
	BANI("bani", { r, (a, b, c) -> r[c] = r[a] and   b  }),

	BORR("borr", { r, (a, b, c) -> r[c] = r[a] or r[b] }),
	BORI("bori", { r, (a, b, c) -> r[c] = r[a] or   b  }),

	SETR("setr", { r, (a, _, c) -> r[c] = r[a] }),
	SETI("seti", { r, (a, _, c) -> r[c] =   a  }),

	GTRI("gtri", { r, (a, b, c) -> r[c] = if (r[a] >   b ) 1 else 0 }),
	GTIR("gtir", { r, (a, b, c) -> r[c] = if (  a  > r[b]) 1 else 0 }),
	GTRR("gtrr", { r, (a, b, c) -> r[c] = if (r[a] > r[b]) 1 else 0 }),

	EQRI("eqri", { r, (a, b, c) -> r[c] = if (r[a] ==   b ) 1 else 0 }),
	EQIR("eqir", { r, (a, b, c) -> r[c] = if (  a  == r[b]) 1 else 0 }),
	EQRR("eqrr", { r, (a, b, c) -> r[c] = if (r[a] == r[b]) 1 else 0 })
}

sealed class Instruction {
	data class Operation(val opcode: OpCode, val arguments: Arguments): Instruction()
	data class ChangeIP(val register: Int): Instruction()
}

fun parse(input: String): List<Instruction> {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
    val opcode = or(OpCode.values().map { opcode -> string(opcode.asm).retn(opcode) })
 	val arguments = sequence(integer, WHITESPACES.next(integer), WHITESPACES.next(integer), ::Arguments)
 	val operation: Parser<Instruction> = sequence(opcode, WHITESPACES.next(arguments), Instruction::Operation)
    val changeip: Parser<Instruction> = string("#ip ").next(integer).map(Instruction::ChangeIP)
 	val program = or(operation, changeip).sepBy(WHITESPACES)
 	return program.parse(input.trim())
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println(input)
}