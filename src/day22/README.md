Ah, a proper coding puzzle again after the last couple of days of drudge-work.

Firstly a data model.
```kotlin
data class Pos(val x: Int, val y: Int) {
	val up: Pos get() = Pos(x, y-1)
	val down: Pos get() = Pos(x, y+1)
	val left: Pos get() = Pos(x-1, y)
	val right: Pos get() = Pos(x+1, y)
	fun neighbours(): Set<Pos> = setOf(up, down, left, right)
}
data class Model(val depth: Int, val target: Pos)
```

Some might suggest the input data format is so simple you can just use string splits and regular expressions. But if I want to make one point in this Advent Of Code it's that parser combinators *always* result in simpler, better code.
```kotlin
fun parse(input: String): Model {
	val integer = INTEGER.map(String::toInt)
	val depth = string("depth: ").next(integer)
	val coord = sequence(integer, string(",").next(integer), ::Pos)
	val target = string("target: ").next(coord)
	val model = sequence(depth, WHITESPACES.next(target), ::Model)
	return model.parse(input.trim())
}
```

Previous solutions with 2D arrays have resulted in slightly messy code. Let's factor out the 2D array stuff.
```kotlin
typealias Grid<T> = Array<Array<T>>

inline fun <reified T> makeGrid(width: Int, height: Int, empty: T) =
	Array<Array<T>>(height) { Array<T>(width) { empty } }

operator fun <T> Grid<T>.get(p: Pos): T = this[p.y][p.x]
operator fun <T> Grid<T>.set(p: Pos, t: T) { this[p.y][p.x] = t }

fun <T> Grid<T>.positions(): Sequence<Pos> =
	indices.asSequence().flatMap { y ->
		this[y].indices.asSequence().map { x -> Pos(x, y) }
	}
```

There's one simple insight to part 1, which is that the calculated value for each position depends on the positions above and to the left. So the whole grid can be calculated in a right-then-down or down-then-right order. Let's do the whole thing in an object initialiser:
```kotlin
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
}
```

The descriptions of erosion level, region type and risk level could all be collapsed since there is a 1:1 relationship. But in my experience of building software, if the customer felt the need to describe these things separately, then model them independently. It's lower cost to have each representation in the code and live with the simple mappings than have to mentally remember the equivalences. And it pays off immensely when the requirements change subtly in the future.
```kotlin
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
	}
}
```

## Part 1
Part 1 is answered simply by computing the sum risk level for the entire cave.
```kotlin
fun part1(input: Model): Int = Cave(input).riskLevel()
```

## Part 2
A meaty part 2 today, which introduces a whole new algorithmic requirement. We have to find the shortest path from the start to the trapped friend. Like Day 15 this can be solved with an application of Dijkstra's algorithm. The difference here is that the cost of moving from region to region varies depending on the equipped tool, and indeed in some cases transitions are not possible due to incompatible tools.

First we'll model the tools, the mappings to region types:
```kotlin
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
```

Dijkstra's algorithm needs a "current best" value for each position in the graph. This is a product of time and equipped tool. We'll also make some helpers for deriving new states:
```kotlin
data class State(val time: Int, val tool: Tool)

fun State.move(p: Pos) = State(time + 1, tool)
fun State.changeTool(t: Tool) = State(time + 7, t)
```

The Dijkstra algorithm follows the same basic structure as before:
1. Start with all positions unvisited and the current position at the start
2. While we haven't visited the end position:
	- Calculate the cost of moving to all the neighbours of the current position
	- If this is less than the current cost for that neighbour, update it
	- Move to the unvisited position with the current least cost
3. The minimum cost is left in the end position

```kotlin
un Cave.dijkstra(start: Pos, end: Pos): State {
	val unvisited = grid.positions().toMutableSet()

	val noPath = State(Int.MAX_VALUE, Tool.NEITHER)
	val states = makeGrid(limit.x+1, limit.y+1, noPath)

	fun valid(p: Pos) = 
		unvisited.contains(p) && 0 <= p.x && p.x <= limit.x && 0 <= p.y && p.y <= limit.y

	states[start] = State(0, Tool.TORCH)

	while (unvisited.contains(end)) {

		val minTime = unvisited.map { p -> states[p].time }.min()
		unvisited.filter { p -> states[p].time == minTime }.forEach { current ->
			val availableTools = grid[current].availableTools()
			val state = states[current]

			current.neighbours().filter(::valid).forEach { neighbour ->
				// we'll come to updating neighbours in a bit
			}

			unvisited.remove(current)
		}
	}

	return states[end]
}```

The difference today is that the cost of moving to a neighbour position is considerably more complex and must take the transition of tools into account. Firstly, we must match the tools available at the current position with the tools available at the neighbour position. Then:

1. If we can move to the neighbour without changing tool the cost is 1 minute.
2. Or if we can move to the neighbour after changing tool the cost is 7 minutes to change tool then 1 minute to move.
3. Or if no tool change is possible to reach the neighbour, the move is impossible. 
```kotlin
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
```
The final aspect of part 2 is that the optimal route may extend beyond the target position. We have no idea how far, but changing tools takes eight times longer than just moving, so the furthest beyond the direct route to the target we might go _without_ changing tools is approximately eight times more. Fudging it a bit and waiting a good old time for the answer:
```kotlin
fun part2(input: Model): Int? =
	(1..8).map { scale -> 	
		val cave = Cave(input, Pos(input.target.x*scale, input.target.y*scale))
		val state = cave.dijkstra(Pos(0, 0), input.target)
		state.time
	}.min()
}
```

Sadly this doesn't get me an answer that Advent of Code is happy with. I am seeing a longer path for scale=4 than scale=3, which is strange, and points to the path being dependent on the order that the positions are visited rather than the intrinsic properties of the positions. I'll keep working on this one.
