# Day 15
Goblins fighting elves. We're getting into serious business now. This puzzle seems quite involved, with a bunch of searching and sorting and a shortest-path finding algorithm. Let's start as usual by building a model and parsing the input.

## Model
There's a lot in common with some of the earlier problems. I'm going to try a slightly different approach from [Day 13](../day13) and model the walls as a set of positions rather than a 2D array.
```
data class Pos(val x: Int, val y: Int)

enum class Dir { UP, LEFT, RIGHT, DOWN }

enum class CreatureType { ELF, GOBLIN }

data class Creature(val id: Int, val type: CreatureType, val pos: Pos, val hitPoints: Int=200, val attack: Int=3)

data class Cave(val walls: Set<Pos>, val creatures: List<Creature>)
```

## Parsing
Simply a matter of turning the 2D array of characters into a model of the things found.
```
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
```

## Simulation
Let's make use of some nice Kotlin features. Ordering:
```
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
```

We'll model the timeline not as an infinite sequence but one that runs until the simulation is stable. Note that `this` inside the `sequence` lambda refers to the `SequenceScope` so we have to qualify it with the label of the outer scope, which is the function name. This is a nice feature of Kotlin and a massive improvement in readability over the outer-class `this` from Java:
```
fun Cave.timeline(): Sequence<Cave> = sequence {
	while (true) {
		val next = turn()
		if (next != this@timeline) 
			yield(next)
		else
			break
	}
}

```

Let's start with a basic structure for turns then flesh it out from there. This is really similar to the minecart-crashing scenario from two days ago. Keeping track of what state had changed and what should remain the start-of-turn state was tricky there so we're going to take a different approach. We'll ask each creature to take a turn and it will return an action. Then we'll apply all the actions to the cave state.

```
fun Cave.turn(): Cave {
	fun others(c: Creature) = creatures.values.filterNot { it.id == c.id }.toSet()

	val actions: List<Action> = creatures.values.sortedWith(readingOrder).map { c ->
		c.takeTurn(walls, others(c))
	}

	return Cave(walls, actions.fold(creatures) { cs, a -> a.applyTo(cs) })
}
```

The update is tricky as we need to deal with creatures that have possibly died, and also remove creatures which die during an attack.
```
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
```

The skeleton for a creature's turn looks like this:
```
fun Creature.takeTurn(walls: Set<Pos>, creatures: Set<Creature>): Action {

	fun move(targets: List<Creature>): Action {
		// TODO
		return Action.None
	}

	fun attack(target: Creature): Action {
		// TODO
		return Action.None
	}

	val targets: List<Creature> = creatures.filter { c -> c.type != type }

	val attackTargets: List<Creature> = targets.filter { c -> c.pos.isNeighbour(pos) }

	return if (attackTargets.isEmpty())
		move(targets)
	else
		attack(attackTargets.sortedWith(attackOrder).first())
}
```
