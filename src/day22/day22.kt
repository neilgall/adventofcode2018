package adventofcode2018.day22

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Colors

const val ESC = "\u001B"
const val NORMAL = ESC + "[0"
const val RED    = ESC + "[0;31m"
const val GREEN  = ESC + "[0;32m"
const val BLUE   = ESC + "[0;34m"
const val WHITE  = ESC + "[0;37m"

// Model

data class Pos(val x: Int, val y: Int) {
	val up: Pos get() = Pos(x, y-1)
	val down: Pos get() = Pos(x, y+1)
	val left: Pos get() = Pos(x-1, y)
	val right: Pos get() = Pos(x+1, y)
	fun neighbours(): Set<Pos> = setOf(up, down, left, right)

	override fun toString(): String = "$x,$y"
}

data class Model(val depth: Int, val target: Pos)

// 2D array helper

typealias Grid<T> = Array<Array<T>>

inline fun <reified T> makeGrid(width: Int, height: Int, empty: T) =
	Array<Array<T>>(height) { Array<T>(width) { empty } }

operator fun <T> Grid<T>.get(p: Pos): T = this[p.y][p.x]
operator fun <T> Grid<T>.set(p: Pos, t: T) { this[p.y][p.x] = t }

fun <T> Grid<T>.positions(): Sequence<Pos> =
	indices.asSequence().flatMap { y ->
		this[y].indices.asSequence().map { x -> Pos(x, y) }
	}

fun <T> Grid<T>.renderWith(f: (Pos, T) -> String): String =
	mapIndexed { y, row ->
		val prefix = y.toString().padStart(5, ' ')
		val line = row.mapIndexed { x, t -> f(Pos(x, y), t) }.joinToString("")
		"$WHITE$prefix: $line"
	}.joinToString("\n")


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

enum class Type(val risk: Int, val symbol: String) {
	ROCKY (0, "."),
	WET   (1, "="),
	NARROW(2, "|")
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
	val grid = makeGrid(limit.x+1, limit.y+1, Square(0, 0))

	init {
		(0..limit.y).forEach { y ->
			(0..limit.x).forEach { x ->
				val p = Pos(x, y)
				val geologicIndex = when {
					p == model.target -> 0
					y == 0 -> x * 16807L
					x == 0 -> y * 48271L
					else -> grid[p.left].erosionLevel * grid[p.up].erosionLevel
				}
				val erosionLevel = (geologicIndex + model.depth) % 20183L
				grid[p] = Square(geologicIndex, erosionLevel)
			}
		}
	}

	fun riskLevel(): Int =
		grid.positions().map { p -> grid[p].type.risk }.sum()

	override fun toString(): String =
		grid.renderWith { _, sq -> sq.type.symbol }
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

data class State(val prev: State?, val pos: Pos, val time: Int, val tool: Tool) {
	override fun toString(): String = "$time:${tool.symbol}:$pos"
}

val State.reversePath: Sequence<State> get() = sequence {
	var p: State? = this@reversePath
	while (p != null) {
		yield(p!!)
		p = p.prev
	}
}

fun Cave.vis(states: Grid<State>, unvisited: Set<Pos>, current: Pos) {
	val shortest = states[current].reversePath.map { s -> s.pos }.toList()

	println(grid.renderWith { pos, _ ->
		val color = when {
			pos == current -> RED
			unvisited.contains(pos) -> WHITE
			shortest.contains(pos) -> GREEN
			else -> BLUE
		}
		val state = when {
			states[pos].prev != null ->
				"${states[pos].time}${states[pos].tool.symbol}"
			
			pos == model.target ->
				" TGT "
			
			else -> 
				"  ${grid[pos].type.symbol}  "
		}
		"$color${state.padStart(5, ' ')}"
	})
	println(WHITE)
}

fun State.move(p: Pos) = State(this, p, time + 1, tool)
fun State.changeTool(t: Tool) = State(this, pos, time + 7, t)

fun Cave.dijkstra(start: Pos, end: Pos): State {
	val unvisited = grid.positions().toMutableSet()

	val noPath = State(null, Pos(0, 0), Int.MAX_VALUE, Tool.NEITHER)
	val states = makeGrid(limit.x+1, limit.y+1, noPath)

	fun valid(p: Pos) = 
		unvisited.contains(p) && 0 <= p.x && p.x <= limit.x && 0 <= p.y && p.y <= limit.y

	states[start] = State(null, start, 0, Tool.TORCH)

	while (unvisited.contains(end)) {

		val minTime = unvisited.map { p -> states[p].time }.min()
		unvisited.filter { p -> states[p].time == minTime }.forEach { current ->

			if (current == Pos(20,503)) vis(states, unvisited, current)

			val availableTools = grid[current].availableTools()
			val state = states[current]

			current.neighbours().filter(::valid).forEach { neighbour ->
				val neighbourTools = if (neighbour == end) setOf(Tool.TORCH) else grid[neighbour].availableTools()

				val neighbourState =
					if (neighbourTools.contains(state.tool))
						state.move(neighbour)
					else {
						val commonTools = neighbourTools.intersect(availableTools)
						if (commonTools.isEmpty())
							noPath
						else 
							state.changeTool(commonTools.first()).move(neighbour)
					}

				if (neighbourState.time < states[neighbour].time) {
					states[neighbour] = neighbourState
				}	
			}

			unvisited.remove(current)
		}
	}

	return states[end]
}

fun part2(input: Model): Int? {
	val paths = (3..4).map { scale -> 	
		val cave = Cave(input, Pos(input.target.x*scale, input.target.y*scale))
		val state = cave.dijkstra(Pos(0, 0), input.target)
		println("scale $scale target ${input.target} limit ${cave.limit} time ${state.time}")
		state.reversePath.toList().reversed()
	}

	paths[0].zip(paths[1]).forEach { (p3, p4) ->
		println("scale 3 $p3 scale 4 $p4 ${if (p3.pos == p4.pos) "" else "X"}")
	}

	return 0
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
	println("Part 2: ${part2(input)}")
}