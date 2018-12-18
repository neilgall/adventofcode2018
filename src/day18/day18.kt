package adventofcode2018.day18

import java.io.File

// Model

enum class Acre(val symbol: Char) {
	OPEN('.'),
	TREES('|'),
	LUMBERYARD('#')
}

data class Pos(val x: Int, val y: Int)

val Pos.neighbours: Sequence<Pos> get() = sequence {
	(-1..1).flatMap { dy ->
		(-1..1).map { dx -> 
			if (dy != 0 || dx != 0) yield(Pos(x+dx,y+dy))
		}
	}
}

class Landscape(val width: Int, val height: Int) {
	val acres: Array<Array<Acre>> = Array<Array<Acre>>(height) { Array<Acre>(width) { Acre.OPEN }}

	val positions: Sequence<Pos> get() = sequence {
		(0..height-1).flatMap { y -> (0..width-1).map { x -> yield(Pos(x, y)) }}
	}

	fun validPos(p: Pos): Boolean = 0 <= p.y && p.y < height && 0 <= p.x && p.x < width

	fun neighbourCount(p: Pos, a: Acre): Int =
		p.neighbours.count { n -> this[n] == a }

	fun totalCount(a: Acre): Int = positions.count { pos -> this[pos] == a }

	fun resourceValue(): Int = totalCount(Acre.TREES) * totalCount(Acre.LUMBERYARD)

	override fun equals(other: Any?) =
		(other is Landscape) && positions.all { p -> this[p] == other[p] }

	override fun hashCode(): Int =
		positions.fold(31) { h, p -> (h * 31) xor this[p].ordinal }

	override fun toString(): String =
		acres.map { row -> row.map { it.symbol }.joinToString("") }.joinToString("\n")
}

operator fun Landscape.get(p: Pos): Acre = if (validPos(p)) acres[p.y][p.x] else Acre.OPEN
operator fun Landscape.set(p: Pos, a: Acre) { if (validPos(p)) acres[p.y][p.x] = a }


// Parsing

fun parse(input: String): Landscape {
	val rows = input.trim().lines()
	val cols: Int = rows.map { r -> r.length }.max()!!
	val landscape = Landscape(cols, rows.size)
	(0..rows.size-1).forEach { y ->
		rows[y].forEachIndexed { x, c -> 
			landscape[Pos(x, y)] = when(c) {
				'.' -> Acre.OPEN
				'|' -> Acre.TREES
				'#' -> Acre.LUMBERYARD
				else -> throw IllegalArgumentException()
			}
		}
	}
	return landscape
}

// Simulation

fun Landscape.tick(): Landscape {
	val next = Landscape(width, height)
	positions.forEach { pos ->
		val neighbours = pos.neighbours.map(this::get).toList()
		next[pos] = when (this[pos]) {
			Acre.OPEN -> 
				if (neighbourCount(pos, Acre.TREES) >= 3) 
					Acre.TREES
				else
					Acre.OPEN

			Acre.TREES ->
				if (neighbourCount(pos, Acre.LUMBERYARD) >= 3)
					Acre.LUMBERYARD
				else
					Acre.TREES

			Acre.LUMBERYARD ->
				if (neighbourCount(pos, Acre.LUMBERYARD) >= 1 && neighbourCount(pos, Acre.TREES) >= 1)
					Acre.LUMBERYARD
				else
					Acre.OPEN
		}
	}
	return next
}

fun Landscape.timeline(): Sequence<Landscape> = sequence {
	var next = this@timeline
	while (true) {
		yield(next)
		next = next.tick()
	}
}

// Part 1

fun part1(input: Landscape): Int =
	input.timeline().drop(10).first().resourceValue()

// Part 2

val indices: Sequence<Int> get() = sequence {
	var i = 0
	while (true) {
		yield (i)
		i += 1
	}
}

inline fun <reified T> findLoop(ts: Sequence<T>): Pair<Int, Int> {
	val seen = mutableMapOf<T, Int>()
	val firstRepeat = ts.zip(indices).dropWhile { (t, i) -> 
		if (seen.containsKey(t))
			false
		else {
			seen[t] = i
			true
		}
	}.first()

	return Pair(seen[firstRepeat.first]!!, firstRepeat.second)
}

fun part2(input: Landscape): Int {
	val (loopStart, loopEnd) = findLoop(input.timeline())
	val iterations = 1000000000
	val loops = (iterations - loopStart) / (loopEnd - loopStart)
	val lastLoop = (iterations - loopStart) % loops
	val lastLoopTimeline = input.timeline().drop(loopStart)
	val lastIteration = lastLoopTimeline.drop(lastLoop).first()
	return lastIteration.resourceValue()
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())

	println("Part 1: ${part1(input)}")
	println("Part 2: ${part2(input)}")
}
