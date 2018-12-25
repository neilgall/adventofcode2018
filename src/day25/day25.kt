package adventofcode2018.day25

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model
data class Point(val w: Int, val x: Int, val y: Int, val z: Int)

// Parser
fun parse(input: String): List<Point> {
	val integer = or(
		INTEGER.map(String::toInt),
		isChar('-').next(INTEGER).map { s -> -s.toInt() }
	)

	val point = sequence(
		integer.followedBy(isChar(',')),
		integer.followedBy(isChar(',')),
		integer.followedBy(isChar(',')),
		integer,
		::Point
	)

	return point.sepBy(WHITESPACES).parse(input.trim())
}

// Part 1
typealias Manhattan = Int

fun Point.distance(p: Point): Manhattan =
	Math.abs(w - p.w) + Math.abs(x - p.x) + Math.abs(y - p.y) + Math.abs(z - p.z)

data class Constellation(val points: Set<Point>) {
	constructor(p: Point): this(setOf(p))
}

fun Constellation.shouldContain(point: Point): Boolean = 
	points.any { p -> p.distance(point) <= 3 }

operator fun Constellation.plus(p: Point): Constellation =
	Constellation(points + p)

operator fun Constellation.plus(c: Constellation): Constellation = 
	Constellation(points + c.points)

fun Collection<Constellation>.merge(p: Point): Constellation =
	fold(Constellation(p), Constellation::plus)

fun part1(input: Collection<Point>): Int {
	val constellations = input.fold(setOf<Constellation>()) { cs, p ->
		val (join, dontJoin) = cs.partition { c -> c.shouldContain(p) }
		(dontJoin + join.merge(p)).toSet()
	}
	return constellations.size
}

fun part1Test(expect: Int, input: String) {
	val result = part1(parse(input))
	println("expect $expect result $result pass ${result == expect}")
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")

	part1Test(2, """
		 0,0,0,0
		 3,0,0,0
		 0,3,0,0
		 0,0,3,0
		 0,0,0,3
		 0,0,0,6
		 9,0,0,0
		12,0,0,0""")
	part1Test(4, """
		-1,2,2,0
		0,0,2,-2
		0,0,0,-2
		-1,2,0,0
		-2,-2,-2,2
		3,0,2,-1
		-1,3,2,2
		-1,0,-1,0
		0,2,1,-2
		3,0,0,0""")
	part1Test(3, """
		1,-1,0,1
		2,0,-1,0
		3,2,-1,0
		0,0,3,1
		0,0,-1,-1
		2,3,-2,0
		-2,2,0,0
		2,-2,0,-1
		1,-1,0,-1
		3,2,0,2""")
	part1Test(8, """
		1,-1,-1,-2
		-2,-2,0,1
		0,2,1,3
		-2,3,-2,1
		0,2,3,-2
		-1,-1,1,-2
		0,-2,-1,0
		-2,2,3,-1
		1,2,2,0
		-1,-2,0,-2""")
}
