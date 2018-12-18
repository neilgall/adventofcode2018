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

fun IntRange.intersect(ir: IntRange) = IntRange(maxOf(start, ir.start), minOf(endInclusive, ir.endInclusive))

data class Box(val x: IntRange, val y: IntRange) {
	companion object { val EMPTY = Box(IntRange.EMPTY, IntRange.EMPTY) }

	val height: Int get() = y.endInclusive - y.start + 1
	val width: Int get() = x.endInclusive - x.start + 1

	fun positions(): Sequence<Pos> =
		y.asSequence().flatMap { y -> 
			x.asSequence().map { x -> Pos(x, y) }
		}

	fun grow(w: Int, h: Int): Box = Box( (x.start-w)..(x.endInclusive+w), (y.start-h)..(y.endInclusive+h) )

	fun intersect(b: Box): Box = Box(x.intersect(b.x), y.intersect(b.y))
}

operator fun Box.plus(p: Pos) = Box(x.extend(p.x), y.extend(p.y))

val Collection<Vein>.boundingBox: Box get() = fold(Box.EMPTY) { box, v -> when(v) {
	is Vein.Vertical   -> Box(box.x.extend(v.x), box.y.extend(v.y))
	is Vein.Horizontal -> Box(box.x.extend(v.x), box.y.extend(v.y))
}}

fun Vein.isAt(p: Pos) = when(this) {
	is Vein.Vertical   -> x == p.x && y.contains(p.y)
	is Vein.Horizontal -> y == p.y && x.contains(p.x)
}

enum class Fill(val symbol: Char) { 
	EMPTY('.'),
	CLAY('#'),
	STILL_WATER('~'),
	FLOWING_WATER('|')
}

data class Scan(val boundingBox: Box) {
	val grid: Array<Array<Fill>> = Array<Array<Fill>>(boundingBox.y.endInclusive+1) { Array<Fill>(boundingBox.width) { Fill.EMPTY }}

	constructor(veins: Collection<Vein>): this(veins.boundingBox.grow(2, 0)) {
		veins.forEach { vein ->
			when(vein) {
				is Vein.Horizontal -> vein.x.forEach { x -> this[Pos(x, vein.y)] = Fill.CLAY }
				is Vein.Vertical   -> vein.y.forEach { y -> this[Pos(vein.x, y)] = Fill.CLAY }
			}
		}
	}

	fun render(box: Box): String {
		val renderBox = box.intersect(boundingBox)
		return renderBox.y.map { y ->
			val prefix = y.toString().padStart(5, ' ')
			val row = renderBox.x.map { x -> this[Pos(x, y)].symbol }.joinToString("")
			"$prefix: $row"
		}.joinToString("\n")
	}

	override fun toString(): String = render(boundingBox)
}

operator fun Scan.contains(p: Pos): Boolean = boundingBox.x.contains(p.x) && 0 <= p.y && p.y <= boundingBox.y.endInclusive
operator fun Scan.get(p: Pos): Fill = grid[p.y][p.x-boundingBox.x.start]
operator fun Scan.set(p: Pos, f: Fill) { grid[p.y][p.x-boundingBox.x.start] = f }


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

sealed class Feature {
	abstract val pos: Pos
	data class Wall(override val pos: Pos): Feature()
	data class Edge(override val pos: Pos): Feature()
}

fun Scan.findFeature(pos: Pos, dir: (Pos) -> Pos): Feature {
	var p = pos
	while (contains(dir(p))) {
		if (this[dir(p)] == Fill.CLAY)
			return Feature.Wall(p)
		else if (this[p.down()] == Fill.EMPTY || this[p.down()] == Fill.FLOWING_WATER)
			return Feature.Edge(p)
		p = dir(p)
	}
	throw IllegalStateException("Can't find a feature at row ${pos.y}")
}

fun Pos.to(end: Pos): Sequence<Pos> {
	val dir = if (y == end.y) {
		if (x > end.x) Pos::left else Pos::right
	} else if (x == end.x) {
		if (y > end.y) Pos::up else Pos::down
	} else throw IllegalArgumentException("Positions must agree on one axis")
	var p = this
	return sequence {
		while (p != end) {
			yield(p)
			p = dir(p)
		}
		yield(end)
	}
}


fun Scan.fill(s: Sequence<Pos>, f: Fill) = s.forEach { p -> this[p] = f }

fun Scan.fill() {
	val flows = mutableSetOf<Pos>(Pos(500, 0))
	
	while (!flows.isEmpty()) {
		// val box = flows.fold(Box.EMPTY, Box::plus).grow(30, 10)
		// print("\u001Bc")
		// println(render(box))
		// Thread.sleep(33)

		val flow = flows.first()
		flows.remove(flow)

		if (flow.y == boundingBox.y.endInclusive) {
			this[flow] = Fill.FLOWING_WATER
			continue
		}
	
		when (this[flow.down()]) {
			Fill.EMPTY -> {
				this[flow] = Fill.FLOWING_WATER
				flows += flow.down()
			}

			Fill.CLAY, Fill.STILL_WATER -> {
				val featureLeft = findFeature(flow, Pos::left)
				val featureRight = findFeature(flow, Pos::right)
				val fillRange = flow.to(featureLeft.pos) + flow.to(featureRight.pos)

				if (featureLeft is Feature.Edge || featureRight is Feature.Edge) {
					fill(fillRange, Fill.FLOWING_WATER)
					if (featureLeft is Feature.Edge) flows += featureLeft.pos
					if (featureRight is Feature.Edge) flows += featureRight.pos
				} else {
					fill(fillRange, Fill.STILL_WATER)
					flows += flow.up()
				}
			}

			Fill.FLOWING_WATER -> {
				this[flow] = Fill.FLOWING_WATER
				// stop here as we merge streams
			}
		}
	}
}

fun part1(input: Scan): Int {
	input.fill()
	println(input)
	return input.boundingBox.positions().count { p ->
		input[p] == Fill.STILL_WATER || input[p] == Fill.FLOWING_WATER 
	}
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
}
