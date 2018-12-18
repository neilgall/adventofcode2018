package adventofcode2018.day17

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Pos(val x: Int, val y: Int)

fun Pos.up(): Pos = Pos(x, y-1)
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

	fun grow(n: Int): Box = Box( (x.start-n)..(x.endInclusive+n), (y.start-n)..(y.endInclusive+n) )
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

enum class Fill { EMPTY, WATER, FLOWING, CLAY }
enum class Flow { BLOCKED, MERGED, FLOWING, DRAINING }

fun Scan.veinAt(p: Pos): Boolean = veins.any { v -> v.isAt(p) }

fun Scan.fill(): Set<Pos> {

	data class State(val scan: Scan, val filled: MutableMap<Pos, Fill>) {
		var Pos.fill: Fill
			get() = when {
				scan.veinAt(this) -> Fill.CLAY
				else -> filled[this] ?: Fill.EMPTY
			}
			set(f) {
				filled[this] = f
			}

		fun reachedBottom(p: Pos) = p.y == scan.boundingBox.y.endInclusive

		tailrec fun fillDown(pos: Pos): Flow {
			// draw("fill down $pos", pos)
			pos.fill = Fill.FLOWING
			return if (reachedBottom(pos))
				Flow.DRAINING
			else {
				when (pos.down().fill) {
					Fill.EMPTY -> fillDown(pos.down())
					Fill.CLAY, Fill.WATER -> fillUp(pos)
					Fill.FLOWING -> Flow.MERGED
				}
			}
		}

		tailrec fun fillUp(pos: Pos): Flow {
			// draw("fill up $pos", pos)
			val flowing = maxOf(fillAcross(pos, Pos::left), fillAcross(pos, Pos::right))
			pos.fill = Fill.WATER
			return when (flowing) {
				Flow.BLOCKED, Flow.MERGED -> fillUp(pos.up())
				else -> {
					pos.fill = Fill.FLOWING
					transformRow(pos, Pos::left, Fill.WATER, Fill.FLOWING)
					transformRow(pos, Pos::right, Fill.WATER, Fill.FLOWING)
					flowing
				}
			}
		}

		tailrec fun fillAcross(pos: Pos, move: (Pos) -> Pos): Flow {
			// draw("fill across $pos", pos)
			val next = move(pos)
			return when (next.fill) {
				Fill.EMPTY -> {
					next.fill = Fill.WATER
					when (next.down().fill) {
						Fill.WATER, Fill.CLAY -> fillAcross(next, move)
						Fill.EMPTY -> fillDown(next)
						Fill.FLOWING -> Flow.FLOWING
					}
				}

				Fill.FLOWING ->
					if (next.down().fill == Fill.FLOWING)
						Flow.MERGED
					else {
						next.fill = Fill.EMPTY
						fillAcross(pos, move)
					}

				Fill.WATER -> Flow.MERGED
				else -> Flow.BLOCKED
			}
		}

		fun transformRow(pos: Pos, move: (Pos) -> Pos, old: Fill, new: Fill) {
			// draw("transformRow $pos $old $new", pos)
			var p = move(pos)
			while (p.fill == old) {
				p.fill = new
				p = move(p)
			}
		}

		fun draw(s: String, p: Pos? = null) {
			val box = scan.boundingBox.grow(5)
			val grid = Array<CharArray>(box.y.endInclusive+1) { CharArray(box.x.endInclusive-box.x.start+1) { ' ' } }
			fun set(p: Pos, c: Char) { if (box.x.contains(p.x) && box.y.contains(p.y)) grid[p.y-box.y.start][p.x-box.x.start] = c }

			scan.veins.forEach { v -> when(v) {
				is Vein.Vertical -> v.y.forEach { y_ -> set(Pos(v.x, y_), '#') }
				is Vein.Horizontal -> v.x.forEach { x_ -> set(Pos(x_, v.y), '#') }
			}}

			filled.forEach { (p, f) -> set(p, if (f == Fill.FLOWING) '|' else '~') }
			p?.let { set(it, '@') }
			
			// val drawGrid = if (p == null) grid.toList() else grid.slice(0..p.y+10)

			println(s)
			println(grid.mapIndexed { i, row -> "${(box.y.start+i).toString().padStart(4, ' ')}: ${row.joinToString("")}" }.joinToString("\n"))
		}
	}

	val state = State(this, mutableMapOf())
	state.fillDown(Pos(500, 0))
	state.draw("end")
	return state.filled.filter { (p, f) -> boundingBox.y.contains(p.y) && f != Fill.EMPTY }.keys.toSet()
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println(input.fill().size)
}
