package adventofcode2018.day22

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Pos(val x: Int, val y: Int)

data class Model(val depth: Int, val target: Pos)

// Parsing

fun parse(input: String): Model {
	val integer = INTEGER.map(String::toInt)
	val depth = string("depth: ").next(integer)
	val coord = sequence(integer, string(",").next(integer), ::Pos)
	val target = string("target: ").next(coord)
	val model = sequence(depth, WHITESPACES.next(target), ::Model)
	return model.parse(input.trim())
}

// Part 1

enum class Type(val risk: Int, val symbol: Char) {
	ROCKY(0, '.'),
	WET(1, '='),
	NARROW(2, '|')
}

data class Square(val geologicIndex: Long, val erosionLevel: Long) {
	val type: Type = when(erosionLevel % 3) {
		0L -> Type.ROCKY
		1L -> Type.WET
		2L -> Type.NARROW
		else -> throw IllegalStateException("unexpected $erosionLevel")
	}
}

data class Cave(val model: Model) {
	val grid = Array<Array<Square>>(model.target.y+1) { Array<Square>(model.target.x+1) { Square(0, 0) }}

	init {
		(0..model.target.y).forEach { y ->
			(0..model.target.x).forEach { x ->
				val geologicIndex = when {
					Pos(x, y) == model.target -> 0
					y == 0 -> x * 16807L
					x == 0 -> y * 48271L
					else -> grid[y][x-1].erosionLevel * grid[y-1][x].erosionLevel
				}
				val erosionLevel = (geologicIndex + model.depth) % 20183L
				grid[y][x] = Square(geologicIndex, erosionLevel)
			}
		}
	}

	override fun toString(): String =
		grid.map { row -> row.map { sq -> sq.type.symbol }.joinToString("") }.joinToString("\n")

	fun riskLevel(): Int =
		grid.asSequence().flatMap { row -> 
			row.asSequence().map { sq -> sq.type.risk }
		}.sum()
}

fun part1(input: Model): Int {
	val cave = Cave(input)
	println(cave)
	return cave.riskLevel()
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
}