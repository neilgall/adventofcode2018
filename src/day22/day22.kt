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
	fun neighbours(): Set<Pos> = setOf(up, down, left, right)
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

enum class Tool(val symbol: Char) { 
	NEITHER('N'),
	TORCH('T'),
	CLIMBING_GEAR('C')
}

fun Square.availableTools(): Set<Tool> = when(type) {
	Type.ROCKY  -> setOf(Tool.CLIMBING_GEAR, Tool.TORCH)
	Type.WET    -> setOf(Tool.CLIMBING_GEAR, Tool.NEITHER)
	Type.NARROW -> setOf(Tool.TORCH, Tool.NEITHER)
}

data class State(val time: Int, val tool: Tool)

fun Cave.vis(states: Map<Pos, State>, unvisited: Set<Pos>, current: Pos) {
	println(grid.mapIndexed { y, row ->
		row.mapIndexed { x, sq -> 
			val pos = Pos(x, y)
			val vis = if (unvisited.contains(pos)) ' ' else '\''
			val state = when {
				pos == current ->
					"  X  "
				states.containsKey(pos) -> {
					val t = if (states[pos]!!.time == Int.MAX_VALUE) "##" else "${states[pos]!!.time}"
					"$t${states[pos]!!.tool.symbol}"
				}
				pos == model.target ->
					" TGT "
				else -> 
					"  ${this[pos].type.symbol}  "
			}
			"$vis$state".padStart(6, ' ')
		}.joinToString("")
	}.joinToString("\n"))
	println()
}

fun Cave.dijkstra(start: Pos, end: Pos): State {
	val unvisited = positions().toMutableSet()
	val statesForPosition = mutableMapOf<Pos, State>()
	val noPath = State(Int.MAX_VALUE, Tool.NEITHER)

	fun stateAt(p: Pos) = statesForPosition[p] ?: noPath
	fun valid(p: Pos) = 0 <= p.x && p.x <= limit.x && 0 <= p.y && p.y <= limit.y

	statesForPosition[start] = State(0, Tool.TORCH)
	var current: Pos? = start

	while (current != null && unvisited.contains(end)) {
		// vis(statesForPosition, unvisited, current)

		val state = stateAt(current)
		val availableTools = get(current).availableTools()

		current.neighbours().filter(::valid).forEach { neighbour ->
			val neighbourTools = if (neighbour == end) setOf(Tool.TORCH) else get(neighbour).availableTools()

			val neighbourState = when {
				neighbourTools.contains(state.tool) ->
					State(state.time + 1, state.tool)

				!neighbourTools.intersect(availableTools).isEmpty() -> 
					State(state.time + 8, neighbourTools.intersect(availableTools).first())

				else ->
					noPath
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

fun part2(input: Model): Int? =
	(1..10).map { scale -> 	
		val cave = Cave(input, Pos(input.target.x*scale, input.target.y*scale))
		val state = cave.dijkstra(Pos(0, 0), input.target)
		println("scale $scale state $state")
		state.time
	}.min()


fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
	println("Part 2: ${part2(input)}")
}