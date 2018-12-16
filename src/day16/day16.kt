package adventofcode2018.day16

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

typealias Registers = List<Int>

data class Instruction(val opcode: Int, val a: Int, val b: Int, val c: Int)

data class Sample(val before: Registers, val instruction: Instruction, val after: Registers)

data class Input(val samples: List<Sample>, val program: List<Instruction>)


// Parsing

fun parse(input: String): Input {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
 	val registers = integer.sepBy(string(", ")).between(string("["), string("]"))
 	val before = string("Before: ").next(registers)
 	val after = string("After:  ").next(registers)
 	val instruction = sequence(
 		integer,
 		WHITESPACES.next(integer),
 		WHITESPACES.next(integer),
 		WHITESPACES.next(integer),
 		::Instruction
 	)
 	val sample = sequence(
 		before,
 		WHITESPACES.next(instruction),
 		WHITESPACES.next(after),
 		::Sample
 	)
 	val samples = sample.sepBy(WHITESPACES)
 	val program = instruction.sepBy(WHITESPACES)

 	return sequence(samples.followedBy(WHITESPACES), program, ::Input).parse(input.trim())
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println(input)
}