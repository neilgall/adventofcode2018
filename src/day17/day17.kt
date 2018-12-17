package adventofcode2018.day17

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

sealed class Vein {
	data class Vertical(val x: Int, val y: IntRange): Vein()
	data class Horizontal(val y: Int, val x: IntRange): Vein()
}

fun IntRange.extend(i: Int) =
	if (this == IntRange.EMPTY) IntRange(i, i) else IntRange(minOf(i, start), maxOf(i, endInclusive))

fun IntRange.extend(ir: IntRange) =
	if (this == IntRange.EMPTY) ir else IntRange(minOf(ir.start, start), maxOf(ir.endInclusive, endInclusive))

data class Box(val x: IntRange, val y: IntRange) {
	companion object { val EMPTY = Box(IntRange.EMPTY, IntRange.EMPTY) }
}

fun Collection<Vein>.boundingBox(): Box = fold(Box.EMPTY) { box, v -> when(v) {
	is Vein.Vertical   -> Box(box.x.extend(v.x), box.y.extend(v.y))
	is Vein.Horizontal -> Box(box.x.extend(v.x), box.y.extend(v.y))
}}

// Parsing

fun parse(input: String): List<Vein> {
	val integer = INTEGER.map(String::toInt)
	val integerRange = sequence(integer, string("..").next(integer)) { x, y -> x..y }
	val verticalVein: Parser<Vein> = sequence(string("x=").next(integer), string(", y=").next(integerRange), Vein::Vertical)
	val horizontalVein: Parser<Vein> = sequence(string("y=").next(integer), string(", x=").next(integerRange), Vein::Horizontal)
	val vein = or(verticalVein, horizontalVein)
	return vein.sepBy(WHITESPACES).parse(input.trim())
}

// Part 1

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println(input)
	println(input.boundingBox())
}