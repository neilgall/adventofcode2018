package adventofcode2018.day22

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Pos(val x: Int, val y: Int) {
	val up: Pos get() = Pos(x, y-1)
	val down: Pos get() = Pos(x, y+1)
	val left: Pos get() = Pos(x-1, y)
	val right: Pos get() = Pos(x+1, y)
}

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

data class Cave(val model: Model, val limit: Pos = model.target) {
	val grid = Array<Array<Square>>(limit.y+1) { Array<Square>(limit.x+1) { Square(0, 0) }}

	init {
		(0..limit.y).forEach { y ->
			(0..limit.x).forEach { x ->
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

	operator fun get(p: Pos) = grid[p.y][p.x]

	fun positions(): Sequence<Pos> =
		grid.indices.asSequence().flatMap { y ->
			grid[y].indices.asSequence().map { x -> Pos(x, y) }
		}

	fun riskLevel(): Int =
		grid.asSequence().flatMap { row -> 
			row.asSequence().map { sq -> sq.type.risk }
		}.sum()

	override fun toString(): String =
		grid.map { row -> row.map { sq -> sq.type.symbol }.joinToString("") }.joinToString("\n")
}

fun part1(input: Model): Int {
	val cave = Cave(input)
	return cave.riskLevel()
}

// Part 2

enum class Tool { 
	NEITHER,
	TORCH,
	CLIMBING_GEAR
}

fun Square.canUseTool(tool: Tool): Boolean = when(type) {
	Type.ROCKY  -> tool == Tool.CLIMBING_GEAR || tool == Tool.TORCH
	Type.WET    -> tool == Tool.CLIMBING_GEAR || tool == Tool.NEITHER
	Type.NARROW -> tool == Tool.TORCH || tool == Tool.NEITHER
}

data class State(val time: Int, val tool: Tool)

fun Cave.dijkstra(start: Pos, end: Pos): State {
	val unvisited = positions().toMutableSet()
	val statesForPosition = mutableMapOf<Pos, State>()

	fun stateAt(p: Pos) = statesForPosition[p] ?: State(Int.MAX_VALUE, Tool.TORCH)
	fun valid(p: Pos) = 0 <= p.x && p.x < limit.x && 0 <= p.y && p.y < limit.y

	statesForPosition[start] = State(0, Tool.TORCH)
	var current: Pos? = start

	while (current != null && !unvisited.isEmpty()) {
		val state = stateAt(current)
		listOf(current.up, current.down, current.left, current.right).filter(::valid).forEach { neighbour ->
			val square = get(neighbour)
			val neighbourState = when {
				neighbour == end && state.tool == Tool.TORCH -> State(state.time + 1, state.tool)
				neighbour == end && state.tool != Tool.TORCH -> State(state.time + 8, Tool.TORCH)
				square.canUseTool(state.tool) -> State(state.time + 1, state.tool)
				square.canUseTool(Tool.NEITHER) -> State(state.time + 8, Tool.NEITHER)
				square.canUseTool(Tool.TORCH) -> State(state.time + 8, Tool.TORCH)
				square.canUseTool(Tool.CLIMBING_GEAR) -> State(state.time + 8, Tool.CLIMBING_GEAR)
				else -> State(Int.MAX_VALUE, Tool.TORCH)
			}
			if (neighbourState.time < stateAt(neighbour).time) {
				statesForPosition[neighbour] = neighbourState
			}
		}

		unvisited.remove(current)
		current = unvisited.minBy { p -> stateAt(p).time }
	}

	return stateAt(end)
}

fun part2(input: Model): State {
	val cave = Cave(input, Pos(input.target.y*2, input.target.x*2))
	return cave.dijkstra(Pos(0, 0), input.target)
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
	println("Part 2: ${part2(input)}")
}