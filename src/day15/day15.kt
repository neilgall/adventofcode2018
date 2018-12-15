package adventofcode2018.day15

import java.io.File

// Model

data class Pos(val x: Int, val y: Int)

enum class Dir { UP, LEFT, RIGHT, DOWN }

enum class CreatureType { ELF, GOBLIN }

typealias ID = Int
data class Creature(val id: ID, val type: CreatureType, val pos: Pos, val hitPoints: Int=200, val attack: Int=3)

data class Cave(val walls: Set<Pos> = setOf(), val creatures: Map<ID, Creature> = mapOf())

operator fun Pos.plus(dir: Dir) = when(dir) {
	Dir.UP    -> Pos(x,   y-1)
	Dir.LEFT  -> Pos(x-1, y)
	Dir.RIGHT -> Pos(x+1, y)
	Dir.DOWN  -> Pos(x,   y+1)
}

val Pos.neighbours: Set<Pos> get() = Dir.values().map { this + it }.toSet()
fun Pos.isNeighbour(p: Pos): Boolean = neighbours.contains(p)

// Parsing

fun Cave.addWall(x: Int, y: Int): Cave =
	Cave(walls + Pos(x, y), creatures)

fun Cave.addCreature(type: CreatureType, x: Int, y: Int): Cave =
	Cave(walls, creatures + (creatures.size to Creature(creatures.size, type, Pos(x, y))))

fun parse(lines: List<String>): Cave =
	lines.foldIndexed(Cave()) { y, cave, row ->
		row.foldIndexed(cave) { x, cave_, c ->
			when(c) {
				'.' -> cave_
				'#' -> cave_.addWall(x, y)
				'E' -> cave_.addCreature(CreatureType.ELF, x, y)
				'G' -> cave_.addCreature(CreatureType.GOBLIN, x, y)
				else -> throw IllegalArgumentException("unexpected '$c' at $x,$y")
			}
		}
	}

// Simulation

val readingOrder: Comparator<Creature> = 
    Comparator<Creature> { 
        c1, c2 -> c1.pos.y - c2.pos.y
    }.then(Comparator<Creature> {
        c1, c2 -> c1.pos.x - c2.pos.x
    })

val attackOrder: Comparator<Creature> =
	Comparator<Creature> {
		c1, c2 -> c1.hitPoints - c2.hitPoints
	}.then(readingOrder)

sealed class Action {
	object None: Action()
	data class Move(val id: ID, val dir: Dir): Action()
	data class ReceiveAttack(val id: ID, val damage: Int): Action()
}

fun Creature.takeTurn(walls: Set<Pos>, creatures: Set<Creature>): Action {

	fun moveTowards(targets: List<Creature>): Action {
		return Action.None
	}

	fun attack(target: Creature): Action {
		return Action.None
	}

	val targets: List<Creature> = creatures.filter { c -> c.type != type }

	val attackTargets: List<Creature> = targets.filter { c -> c.pos.isNeighbour(pos) }

	return if (attackTargets.isEmpty())
		moveTowards(targets)
	else
		attack(attackTargets.sortedWith(attackOrder).first())
}

fun Creature.move(dir: Dir): Creature = copy(pos = pos + dir)

fun Creature.receiveAttack(damage: Int): Creature? {
	val attacked = copy(hitPoints = hitPoints - damage)
	return if (attacked.isDead()) null else attacked
}

fun Creature.isDead(): Boolean = hitPoints <= 0 

fun Action.applyTo(creatures: Map<ID, Creature>): Map<ID, Creature> {
	val update: Pair<ID, Creature>? = when(this) {
		is Action.None ->
			null

		is Action.Move -> {
			val c = creatures[id]?.move(dir)
			if (c == null) null else (id to c)
		}
		
		is Action.ReceiveAttack -> {
			val c = creatures[id]?.receiveAttack(damage)
			if (c == null) null else (id to c)
		}
	}
	return if (update == null) creatures else creatures + update
}

fun Cave.turn(): Cave {
	fun others(c: Creature) = creatures.values.filterNot { it.id == c.id }.toSet()

	val actions: List<Action> = creatures.values.sortedWith(readingOrder).map { c ->
		c.takeTurn(walls, others(c))
	}

	return Cave(walls, actions.fold(creatures) { cs, a -> a.applyTo(cs) })
}

fun Cave.timeline(): Sequence<Cave> = sequence {
	while (true) {
		val next = turn()
		if (next != this@timeline) 
			yield(next)
		else
			break
	}
}


fun main(vararg args: String) {
	val input = parse(File(args[0]).readLines())
	println(input.timeline().count())
}