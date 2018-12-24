package adventofcode2018.day23

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Pos(val x: Int, val y: Int, val z: Int)

data class Nanobot(val pos: Pos, val radius: Int)

typealias Manhattan = Int

operator fun Pos.minus(other: Pos): Manhattan = 
	Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z)

// Parsing

fun parse(input: String): List<Nanobot> {
	val integer = or(
		string("-").next(INTEGER).map { s -> -s.toInt() },
		INTEGER.map(String::toInt)
	)

	val pos = sequence(string("pos=<").next(integer),
					   string(",").next(integer),
					   string(",").next(integer).followedBy(string(">")),
					   ::Pos)
	
	val nanobot = sequence(pos, string(", r=").next(integer), ::Nanobot)
	
	return nanobot.sepBy(WHITESPACES).parse(input.trim())
}

// Part 1

fun part1(nanobots: List<Nanobot>): Int {
	val strongest = nanobots.maxBy { n -> n.radius }!!
	return nanobots.count { n -> (strongest.pos - n.pos) <= strongest.radius }
}

// Part 2

fun Nanobot.positionsInRange(): Sequence<Pos> = 
	(-radius..radius).asSequence().flatMap { z ->
		val yr = radius-z
		(-yr..yr).asSequence().flatMap { y ->
			val xr = radius-z-y
			(-xr..xr).asSequence().map { x -> Pos(pos.x+x, pos.y+y, pos.z+z) }
		}
	}

fun part2(nanobots: List<Nanobot>): Int {
	val inRange = mutableMapOf<Pos, Int>()
	nanobots.forEach { n ->
		n.positionsInRange().forEach { pos ->
			inRange[pos] = (inRange[pos] ?: 0) + 1
		}
	}
	val mostNanobots = inRange.values.max()!!
	val positionsWithMostNanobots = inRange.filter { (_, v) -> v == mostNanobots }.keys

	val origin = Pos(0, 0, 0)
	return positionsWithMostNanobots.map { p -> p - origin }.min()!!
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
	println("Part 2: ${part2(input)}")
}
