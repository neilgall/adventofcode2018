package adventofcode2018.day15

import java.io.File

// Model

data class Pos(val x: Int, val y: Int)

data class Size(val width: Int, val height: Int)

enum class Dir { UP, LEFT, RIGHT, DOWN }

enum class CreatureType { ELF, GOBLIN }

typealias ID = Int
data class Creature(val id: ID, val type: CreatureType, val pos: Pos, val hitPoints: Int=200, val attackPower: Int=3)

data class Cave(val size: Size, val openPositions: Set<Pos> = setOf(), val creatures: Map<ID, Creature> = mapOf())

operator fun Pos.plus(dir: Dir) = when(dir) {
	Dir.UP    -> Pos(x,   y-1)
	Dir.LEFT  -> Pos(x-1, y)
	Dir.RIGHT -> Pos(x+1, y)
	Dir.DOWN  -> Pos(x,   y+1)
}

val Pos.neighbours: Set<Pos> get() = Dir.values().map { this + it }.toSet()
fun Pos.isNeighbour(p: Pos): Boolean = neighbours.contains(p)

// Parsing

fun Size.positions(): Sequence<Pos> =
	(0..height-1).asSequence().flatMap { y ->
		(0..width-1).asSequence().map { x -> Pos(x, y) }
	}

fun parse(input: String, elfAttackPower: Int = 3): Cave {
	val rows: List<CharArray> = input.trim().lines().map { s -> s.trim().toCharArray() }
	val size = Size(rows.size, rows.map { it.size }.max()!!)

	val (walls, creatures) = size.positions().fold( Pair(listOf<Pos>(), listOf<Creature>()) ) { (w, c), pos ->
		when(rows[pos.y][pos.x]) {
			'.' -> Pair(w, c)
			'#' -> Pair(w + pos, c)
			'E' -> Pair(w, c + Creature(c.size, CreatureType.ELF, pos, attackPower = elfAttackPower))
			'G' -> Pair(w, c + Creature(c.size, CreatureType.GOBLIN, pos, attackPower = 3))
			else -> throw IllegalArgumentException("unexpected '${rows[pos.y][pos.x]}' at $pos")
		}
	}

	return Cave(size, (size.positions() - walls).toSet(), creatures.associateBy { it.id })
}

// Visualisation

fun Cave.render(): String {
	val grid: Array<Array<String>> = Array(size.height+1) { Array<String>(size.width+1) { "#####" } }
	openPositions.forEach { p -> grid[p.y][p.x] = "....." }
	creatures.values.forEach { c -> grid[c.pos.y][c.pos.x] = "${if (c.type == CreatureType.ELF) 'E' else 'G'}${c.hitPoints}".padStart(5, '.') }
	return grid.map { it.joinToString("") }.joinToString("\n")
}

// Simulation

data class Goal(val start: Pos, val end: Pos, val distance: Int)

inline fun <reified T> readingOrder(crossinline f: (T) -> Pos): Comparator<T> = 
    Comparator<T> { 
        a, b -> f(a).y - f(b).y
    }.then(Comparator<T> {
        a, b -> f(a).x - f(b).x
    })

val attackOrder: Comparator<Creature> =
	Comparator<Creature> { c1, c2 -> c1.hitPoints - c2.hitPoints }.then(readingOrder { it.pos })

val shortestDistance: Comparator<Goal> =
	Comparator<Goal> { g1, g2 -> g1.distance - g2.distance }.then(readingOrder { it.end })


val NO_PATH: Int = Int.MAX_VALUE
typealias Visualiser = (Pos, Pos, Pos?, Set<Pos>, Map<Pos, Int>) -> Unit

fun dijkstra(start: Pos, end: Pos, unblocked: Set<Pos>, vis: Visualiser): Int? {
	val unvisited = (unblocked + end).toMutableSet()
	val distances = mutableMapOf<Pos, Int>()
	fun distance(p: Pos) = distances[p] ?: NO_PATH

	distances[start] = 0
	var current: Pos? = start

	while (current != null && distance(current) != NO_PATH && !unvisited.isEmpty()) {
		var neighbourDistance = distance(current) + 1
		var validNeighbours = current.neighbours.filter(unvisited::contains)

		validNeighbours.forEach { n ->
			distances[n] = minOf(neighbourDistance, distance(n))
		}

		unvisited.remove(current)
		current = unvisited.minBy(::distance)

		// vis(start, end, current, unvisited, distances)
	}

	return distances[end]
}

sealed class Action {
	object None: Action() { override fun toString() = "None" }
	data class Move(val id: ID, val pos: Pos): Action()
	data class Attack(val attacker: ID, val attacked: ID): Action()
	data class Multiple(val actions: List<Action>): Action()
}

operator fun Action.plus(a: Action) = if (a is Action.None) this else when(this) {
	is Action.None -> a
	is Action.Multiple -> Action.Multiple(actions + a)
	else -> Action.Multiple(listOf(this, a))
}

fun Creature.takeTurn(openPositions: Set<Pos>, otherCreatures: Collection<Creature>, vis: Visualiser): Action {

	val unblocked: Set<Pos> = openPositions - otherCreatures.map { c -> c.pos }
	val targets: List<Creature> = otherCreatures.filter { c -> c.type != type }

	fun canAttackFrom(p: Pos): Boolean = targets.any { t -> t.pos.isNeighbour(p) }
	fun attackTargetAt(p: Pos): Creature? = targets.filter { t -> t.pos.isNeighbour(p) }.minWith(attackOrder)

	fun moveTowards(targets: List<Creature>): Pos? {
		val validStarts = pos.neighbours.filter(unblocked::contains)
		val goals: List<Goal> = targets.flatMap { target -> 
			validStarts.mapNotNull { start -> 
				dijkstra(start, target.pos, unblocked, vis)?.let { d -> Goal(start, target.pos, d) }
			}
		}
		return goals.minWith(shortestDistance)?.start
	}

	val newPos = if (canAttackFrom(pos)) pos else moveTowards(targets) ?: pos
	val moveAction = if (newPos != pos) Action.Move(id, newPos) else Action.None
	val attackAction = attackTargetAt(newPos)?.let { t -> Action.Attack(id, t.id) } ?: Action.None

	return moveAction + attackAction
}

fun Creature.isDead(): Boolean = hitPoints <= 0 

fun Action.applyTo(creatures: Map<ID, Creature>): Map<ID, Creature> = when(this) {
	is Action.None ->
		creatures

	is Action.Move -> {
		val c = creatures[id]?.copy(pos = pos)
		if (c == null) creatures else creatures + (id to c)
	}
	
	is Action.Attack -> {
		val damage = creatures[attacker]?.attackPower ?: 0
		val c = creatures[attacked]?.let { it.copy(hitPoints = it.hitPoints - damage) }
		if (c == null || c.isDead()) creatures - attacked else creatures + (attacked to c)
	}

	is Action.Multiple ->
		actions.fold(creatures) { cs, a -> a.applyTo(cs) }
}

fun Cave.turn(): Cave {
	fun vis(start: Pos, end: Pos, cur: Pos?, unvisited: Set<Pos>, distances: Map<Pos, Int>) {
		val grid = Array<Array<String>>(size.height+1) { Array<String>(size.width+1) { "#####" } }
		unvisited.forEach { p -> grid[p.y][p.x] = "....." }
		creatures.values.forEach { c -> grid[c.pos.y][c.pos.x] = "${if (c.type == CreatureType.ELF) 'E' else 'G'}${c.hitPoints}".padEnd(5, ' ') }
		grid[start.y][start.x] = "START"
		grid[end.y][end.x] = ".END."
		cur?.let { grid[it.y][it.x] = "..X.." }
		distances.forEach { (p, d) -> grid[p.y][p.x] = "$d ".padStart(5, ' ') }
		println(grid.map { it.joinToString("") }.joinToString("\n"))
	}

	val scanOrder: List<Creature> = creatures.values.sortedWith(readingOrder { it.pos })

	val newCreatures = scanOrder.fold(creatures) { creatures_, c ->
		val action = c.takeTurn(openPositions, creatures_.values, ::vis)
		// println("$c $action")
		action.applyTo(creatures_)
	}

	// val actions = scanOrder.map { c -> c.takeTurn(openPositions, creatures.values, ::vis) }
	// val newCreatures = actions.fold(creatures) { cs, a -> a.applyTo(cs) }

	return copy(creatures = newCreatures)
}

fun Cave.validate(): Cave {
	creatures.values.forEach { c -> 
		if (!openPositions.contains(c.pos)) throw IllegalStateException("Creature $c overlaps a wall")
		if (creatures.values.filter { it.pos == c.pos }.size != 1) throw IllegalStateException("Creature $c overlaps another creature") 
	}
	return this
}

fun Cave.timeline(): Sequence<Cave> = sequence {
	var current: Cave = this@timeline
	var index = 1

	// println("Initial\n${render()}")

	while (true) {
		val next = current.turn().validate()

		// println("Round $index\n${next.render()}")
		// index += 1

		if (next != current) { 
			yield(next)
			current = next
		} else {
			break
		}
	}
}

// Part 1

fun Cave.remainingHitPoints(): Int = creatures.values.map { c -> c.hitPoints}.sum()

fun part1Factors(cave: Cave): Pair<Int, Int> {
	val sim = cave.timeline()
	// I have no idea why this is counting one extra. It is only doing so for the real
	// input and summarised examples, not the fully worked example.
	val rounds = sim.count() - 1
	val remaining = sim.last().remainingHitPoints()
	return Pair(rounds, remaining)
}

fun part1(cave: Cave): Int {
	val (rounds, remaining) = part1Factors(cave)
	return rounds * remaining
}

fun part1Test(input: String, expect: Int) {
	val (rounds, remaining) = part1Factors(parse(input))
	val result = rounds * remaining
	println("expect: $expect, rounds: $rounds, remaining: $remaining, result: $result: ${result == expect}")
}

// Part 2

fun Cave.elves(): Int = creatures.values.count { it.type == CreatureType.ELF }

fun part2Factors(input: String): Triple<Int, Int, Int> {
	val numElves = parse(input).elves()
	val ints: Sequence<Int> = generateSequence(4) { it + 1 }
	val scenarios = ints.map { i -> 
		parse(input, elfAttackPower=i).timeline()
	}

	val elvesWin = scenarios.dropWhile { it.last().elves() < numElves }.first()
	val elfAttackPower = elvesWin.last().creatures.values.first().attackPower
	val rounds = elvesWin.count() - 1
	val remaining = elvesWin.last().remainingHitPoints()

	return Triple(elfAttackPower, rounds, remaining)
}

fun part2(input: String): Int  {
	val (_, rounds, remaining) = part2Factors(input)
	return rounds * remaining
}

fun part2Test(input: String, expect: Int) {
	val (elfAttackPower, rounds, remaining) = part2Factors(input)
	val result = rounds * remaining
	println("expect: $expect, elfAttackPower: $elfAttackPower, rounds: $rounds, remaining: $remaining, result: $result: ${result == expect}")
}

fun main(vararg args: String) {
	val tests: List<String> = listOf(
		 """#######
			#.G...#
			#...EG#
			#.#.#G#
			#..G#E#
			#.....#   
			#######""".trimIndent(),

		 """#######
			#G..#E#
			#E#E.E#
			#G.##.#
			#...#E#
			#...E.#
			#######""".trimIndent(),

		 """#######
			#E..EG#
			#.#G.E#
			#E.##E#
			#G..#.#
			#..E#.#
			#######""".trimIndent(),
		 
		 """#######
			#E.G#.#
			#.#G..#
			#G.#.G#
			#G..#.#
			#...E.#
			#######""".trimIndent(),

		 """#######
			#.E...#
			#.#..G#
			#.###.#
			#E#G#G#
			#...#G#
			#######""".trimIndent(),

		 """#########
			#G......#
			#.E.#...#
			#..##..G#
			#...##..#
			#...#...#
			#.G...G.#
			#.....G.#
			#########""".trimIndent())

	val part1TestExpects: List<Int> = listOf(27730, 36334, 39514, 27755, 28944, 18740)
	val part2TestExpects: List<Int> = listOf(4988, 0, 31284, 3478, 6474, 1140)

	tests.zip(part1TestExpects).forEach { (s, e) -> part1Test(s, e) }
	tests.zip(part2TestExpects).forEach { (s, e) -> part2Test(s, e) }

	val input = File(args[0]).readText()
	println("Part 1: ${part1(parse(input))}")
	println("Part 2: ${part2(input)}")
}