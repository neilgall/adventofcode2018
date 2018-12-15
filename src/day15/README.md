# Day 15
Goblins fighting elves. We're getting into serious business now. This puzzle seems quite involved, with a bunch of searching and sorting and a shortest-path finding algorithm. Let's start as usual by building a model and parsing the input.

## Model
There's a lot in common with some of the earlier problems. I'm going to try a slightly different approach from [Day 13](../day13) and model the open positions (i.e. the inverse of the walls) as a set of positions, as that's a useful starting point for the Dijkstra shortest path algorithm.
```
data class Pos(val x: Int, val y: Int)

enum class Dir { UP, LEFT, RIGHT, DOWN }

enum class CreatureType { ELF, GOBLIN }

data class Creature(val id: Int, val type: CreatureType, val pos: Pos, val hitPoints: Int=200, val attackPower: Int=3)

data class Cave(val size: Size, val openPositions: Set<Pos> = setOf(), val creatures: Map<ID, Creature> = mapOf())
```

## Parsing
Simply a matter of turning the 2D array of characters into a model of the things found.
```
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
	var current: Cave = this@timeline
	while (true) {
		val next = current.turn().validate()
		if (next != current) { 
			yield(next)
			current = next
		} else {
			break
		}
	}
}
```

Let's start with a basic structure for turns then flesh it out from there. This is really similar to the minecart-crashing scenario from two days ago. It was not clear in today's puzzle description whether actions should all apply at the end of a round, or as they happen. So I wrote the code in a way that each creature returns an `Action` from its turn, and I could apply them in different ways to experiment.

There's still an issue here as while I got the right answers in the end there's a cosmological constant fudge in my solution (well, a subtraction of one) and it only gives the correct result for some of the example inputs.

```
fun Cave.turn(): Cave {
	val scanOrder: List<Creature> = creatures.values.sortedWith(readingOrder { it.pos })

	val newCreatures = scanOrder.fold(creatures) { creatures_, c ->
		val action = c.takeTurn(openPositions, creatures_.values)
		action.applyTo(creatures_)
	}

	return copy(creatures = newCreatures)
}
```

The update is tricky as we need to deal with creatures that have possibly died, and also remove creatures which die during an attack.
```
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
```

The meat comes in each creature's turn. If it can't attack it moves. After it has moved if it can attack it does so. The orderings defined above come in useful for selecting the target to attack and the best target to move towards.

Moving is an application of Dijkstra's shortest path algorithm, uses in the `moveTowards()` inner function. For the product of valid start positions (which are neighbours as a creature can only move one step) and potential targets we calculate the shortest path. Then take the shortest of those, if present, and move to the associated start position.

```
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
```

Dijkstra's algorithm is straightforward. At this point I guess I should admit I once worked at TomTom and this stuff was bread and butter for me in those days.
1. Start with a set of unvisited nodes, and a current node at the start node with a distance of 0
2. While there are unvisited nodes:
	a. Go to all unvisited unblocked neighbours of the current node and update the distance to (current+1) if that is less than that node's current value
	b. Remove the current node from the unvisited set
	c. Make the unvisited node with the shortest distance the current node
3. At the end, if there is a distance recorded on the end node, that is the shortest distance from start to end
```
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
```