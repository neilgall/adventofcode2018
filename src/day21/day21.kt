package adventofcode2018.day21

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Registers(
	val regs: IntArray = intArrayOf(0, 0, 0, 0, 0, 0),
	val ipBinding: Int = 0
) {
	operator fun get(r: Int): Int = regs[r]

	operator fun set(r: Int, v: Int): Unit { regs[r] = v }

	val ip: Int get() = regs[ipBinding]

	fun step(): Unit {
		regs[ipBinding] += 1
	}

	override fun toString(): String = "ip=$ip ($ipBinding) [${regs.map(Int::toString).joinToString(", ")}]"
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

data class Instruction(val opcode: OpCode, val arguments: Arguments)

data class Program(val ipBinding: Int, val instructions: List<Instruction>)

fun parse(input: String): Program {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
    val opcode = or(OpCode.values().map { opcode -> string(opcode.asm).retn(opcode) })
 	val arguments = sequence(integer, WHITESPACES.next(integer), WHITESPACES.next(integer), ::Arguments)
 	val operation: Parser<Instruction> = sequence(opcode, WHITESPACES.next(arguments), ::Instruction)
    val bindip = string("#ip ").next(integer)
 	val program = sequence(bindip, WHITESPACES.next(operation.sepBy(WHITESPACES)), ::Program)
 	return program.parse(input.trim())
}

// Execution

fun execute(program: Program, r0: Int = 0): Registers {
	val registers = Registers(ipBinding = program.ipBinding)
	registers[0] = r0

	while (program.instructions.indices.contains(registers.ip)) {
		val instr = program.instructions[registers.ip]
		// println("$registers $instr")
		instr.opcode.exec(registers, instr.arguments)
		registers.step()
	}

	return registers
}

// Reimplementation

fun execute(r0: Int) {
	println(r0)
	var r1 = 0
	do {
		var r4 = r1 or 0x10000
		while (256 > r4) {
			r1 = (0xc2e362 + (r1 and 0xff)) and 0xffffff
			r1 = (r1 * 0x1016b) and 0xffffff

			var r3 = 0
			while ((r3 + 1) * 256 <= r4)
				r3 = 1
			r4 = r3
		}
	} while (r1 != r0)
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	255.downTo(1).forEach { execute(it) }
}
