package adventofcode2018.day17

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Pos(val x: Int, val y: Int)

fun Pos.down(): Pos = Pos(x, y+1)
fun Pos.left(): Pos = Pos(x-1, y)
fun Pos.right(): Pos = Pos(x+1, y)
fun Pos.below(p: Pos): Boolean = y > p.y

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

fun Vein.isAt(p: Pos) = when(this) {
	is Vein.Vertical   -> x == p.x && y.contains(p.y)
	is Vein.Horizontal -> y == p.y && x.contains(p.x)
}

data class Scan(val veins: List<Vein>) {
	val boundingBox: Box = veins.boundingBox()
}

// Parsing

fun parse(input: String): Scan {
	val integer = INTEGER.map(String::toInt)
	val integerRange = sequence(integer, string("..").next(integer)) { x, y -> x..y }
	val verticalVein: Parser<Vein> = sequence(string("x=").next(integer), string(", y=").next(integerRange), Vein::Vertical)
	val horizontalVein: Parser<Vein> = sequence(string("y=").next(integer), string(", x=").next(integerRange), Vein::Horizontal)
	val vein = or(verticalVein, horizontalVein)
	val veins = vein.sepBy(WHITESPACES).map(::Scan)
	return veins.parse(input.trim())
}

// Part 1

fun Scan.vis(filled: Set<Pos>, flows: Set<Pos>) {
	val grid = Array<CharArray>(boundingBox.y.endInclusive-boundingBox.y.start+1) { CharArray(boundingBox.x.endInclusive-boundingBox.x.start+1) { '.' } }
	fun set(p: Pos, c: Char) { if (boundingBox.x.contains(p.x) && boundingBox.y.contains(p.y)) grid[p.y-boundingBox.y.start][p.x-boundingBox.x.start] = c }

	veins.forEach { v -> when(v) {
		is Vein.Vertical -> v.y.forEach { y_ -> set(Pos(v.x, y_), '#') }
		is Vein.Horizontal -> v.x.forEach { x_ -> set(Pos(x_, v.y), '#') }
	}}
	filled.forEach { p -> set(p, '~') }
	flows.forEach { p -> set(p, '@') }
	println(grid.map { it.joinToString("") }.joinToString("\n"))
}

fun Scan.veinAt(p: Pos): Boolean = veins.any { v -> v.isAt(p) }

typealias Path = List<Pos>
typealias Filled = Set<Pos>

fun Path.goDown(): Path = this + last().down()
fun Path.goLeft(): Path = this + last().left()
fun Path.goRight(): Path = this + last().right()

fun Scan.fill(): Set<Pos> {
	fun reachedBottom(p: Pos): Boolean = p.y == boundingBox.y.endInclusive

	fun fillFrom(path: Path, filled: Filled): Filled {
		val end = path.last()

		fun empty(p: Pos, f: Filled) = !f.contains(p) && !veinAt(p)

		fun fillDown(): Pair<Path, Filled> {
			val down: Pos = end.down()
			return if (empty(down, filled))
				Pair(path + down, fillFrom(path + down, filled + end))
			else
				Pair(path, filled)
		}

		fun fillAcross(path: Path, filled: Filled): Set<Pos> {
			val branchFilled = filled + path.last()
			val branches = listOf(path.goLeft(), path.goRight())
				.filter { p -> empty(p.last(), filled) }

			return branches.fold(branchFilled) { f, p -> f + fillFrom(p, branchFilled) }
		}

		// vis(filled, setOf(end))

		return if (reachedBottom(end))
			filled + end
		else {
			val (path_, filled_) = fillDown()
			if (filled_.any(::reachedBottom))
				filled_
			else
				fillAcross(path_, filled_)
		}
	}

	val start = Pos(500, 0)
	return fillFrom(listOf(start), setOf()) - start
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println(input.fill().size)
}
