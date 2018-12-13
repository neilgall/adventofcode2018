# Day 13
Half way, and we have a puzzle that just needs cranked through. There's no trick
that particularly helps - just read the input into a big 2D array, turn the cart
symbols into little models of the carts that can maintain the necessary information
about the next turn, etc., and run the ticks.

It can be done without a bunch of ugly and error-prone mutable state though. First a model:

```
data class Pos(val x: Int, val y: Int)

enum class Dir { LEFT, RIGHT, UP, DOWN }

enum class Turn { LEFT, STRAIGHT, RIGHT }

data class Plan(val dir: Dir, val turn: Turn)

data class Cart(val id: Int, val pos: Pos, val plan: Plan, val collided: Boolean = false)

typealias Track = Char
typealias Tracks = List<CharArray>

data class Model(val tracks: Tracks, val carts: List<Cart>)
```

## Loading the input data
The input text lines are just mapped to `CharArray`s. Then I use a pair of
`foldIndexed()` calls run over the input on both axes and find the locations
of the carts. A starting model for each cart is built here.
```
fun load(lines: List<String>): Model {
    val tracks: Tracks = lines.map(String::toCharArray)

    val carts: List<Cart> = tracks.foldIndexed(listOf<Cart>()) { y, carts, row ->
        row.foldIndexed(carts) { x, carts_, c ->
            if (!("^v<>".contains(c)))
                carts_
            else
                carts_ + Cart(carts_.size, Pos(x, y), Plan(dirFor(c), Turn.LEFT))
        }
    }
```

Finally I replace the cart positions with track characters. I could have left them
in place and given the original characters the same semantics, I guess. No big deal.
```
    carts.forEach { c ->
        tracks[c.pos.y][c.pos.x] = (if (c.plan.dir == Dir.LEFT || c.plan.dir == Dir.RIGHT) '-' else '|')
    }

    return Model(tracks, carts)
}
```

## Part 1
Cart movement is broken into a bunch of little functions, each of which should
be pretty easy to understand.

```
fun Dir.turn(t: Turn): Dir = when(t) {
    Turn.STRAIGHT -> this
    Turn.LEFT -> when (this) {
        Dir.UP -> Dir.LEFT
        Dir.LEFT -> Dir.DOWN
        Dir.DOWN -> Dir.RIGHT
        Dir.RIGHT -> Dir.UP
    }
    Turn.RIGHT -> when (this) {
        Dir.UP -> Dir.RIGHT
        Dir.RIGHT -> Dir.DOWN
        Dir.DOWN -> Dir.LEFT
        Dir.LEFT -> Dir.UP
    }
}

fun Turn.next(): Turn = when(this) {
    Turn.LEFT -> Turn.STRAIGHT
    Turn.STRAIGHT -> Turn.RIGHT
    Turn.RIGHT -> Turn.LEFT
}

fun Pos.apply(plan: Plan): Pos = when(plan.dir) {
    Dir.UP -> Pos(x, y-1)
    Dir.DOWN -> Pos(x, y+1)
    Dir.LEFT -> Pos(x-1, y)
    Dir.RIGHT -> Pos(x+1, y)
}
```
The interesting one is `Plan.adjust()` which takes the current track character at
a cart's position and builds a new plan for the cart. On intersections it applies
turning logic, on curves it changes direction, etc. In some cases the plan doesn't
change.
```
fun Plan.adjust(t: Track): Plan = when(t) {
    '+' -> Plan(dir.turn(turn), turn.next())
    '/' -> when (dir) {
        Dir.UP -> Plan(Dir.RIGHT, turn)
        Dir.RIGHT -> Plan(Dir.UP, turn)
        Dir.DOWN -> Plan(Dir.LEFT, turn)
        Dir.LEFT -> Plan(Dir.DOWN, turn)
    }
    '\\' -> when (dir) {
        Dir.UP -> Plan(Dir.LEFT, turn)
        Dir.LEFT -> Plan(Dir.UP, turn)
        Dir.DOWN -> Plan(Dir.RIGHT, turn)
        Dir.RIGHT -> Plan(Dir.DOWN, turn)
    }
    '-' -> this
    '|' -> this
    else -> throw IllegalStateException()
}
```
Moving a cart is a matter of determining the new plan based on the current track
character, then executing the plan.
```
fun Cart.move(tracks: Tracks): Cart {
    val newPlan = plan.adjust(tracks[pos.y][pos.x])
    return Cart(id, pos.apply(newPlan), newPlan)
}
```

Scanning for the order to move the carts makes use of a nice Kotlin library
feature of chainable comparators:
```
val scanOrder: Comparator<Cart> =
    Comparator<Cart> { 
        c1, c2 -> c1.pos.y - c2.pos.y
    }.then(Comparator<Cart> {
        c1, c2 -> c1.pos.x - c2.pos.x
    })
```

Each tick is implemented by moving the carts in scan order and checking for
collisions. One tricky part of this is that the carts move one at a time, giving
a bunch of inter-tick mini-states. So the initial state of the fold is the
previous state's carts, and on each iteration of the fold one cart is replaced
by its updated version. This ensures the correct collision comparisons are made.

A whole new model is built each time.
```
fun Cart.checkCollisions(carts: Collection<Cart>): Cart =
    if (carts.any { c -> c.collided })
        this // only want the first collision
    else if (carts.none { c -> c.pos == pos })
        this
    else
        this.copy(collided=true)

fun Model.tick(): Model {
    val newCarts = cartsInScanOrder.fold(carts) { carts_, c ->
        val otherCarts = carts_.filter { it != c }
        otherCarts + c.move(tracks).checkCollisions(otherCarts)
    }
    return Model(tracks, newCarts)
}
```

Running the simulation makes use of an infinite sequence of states I call the timeline:

```
fun timeline(initial: Model): Sequence<Model> {
    var model = initial
    return sequence {
        while (true) {
            yield(model)
            model = model.tick()
        }
    }
}
```
Just drop states from this sequence until a collision is detected, then find the
crashed cart:
```
fun Model.hasCollisions(): Boolean = carts.any { it.collided }

fun part1(input: Model): Pos {
    val endState = timeline(input).dropWhile { m -> !m.hasCollisions() }.first()
    return endState.cartsInScanOrder.filter { c -> c.collided }.first().pos
}
```

## Part 2
A proper part 2 today, which introduced a new tricky problem. Removing the carts
the instant they crash introduces some more complexity to those inter-tick mini-states.
We're folding over the carts in scan order, but our fold accumulator is the updated
list of carts. These can become out of sync as a collision can happen before the
scan order reaches a certain cart, removing it from the simulation. I solved this
is a kind-of hacky way by skipping any cart in the fold that has been removed
from the list.

```
fun Model.tick(): Model {
    val newCarts = cartsInScanOrder.fold(carts) { carts_, c ->
        if (!carts_.contains(c)) // removed by collision
            carts_
        else {
            val c_ = c.move(tracks)
            val otherCarts = carts_.filter { it != c }
            val crash = c_.collision(otherCarts)
            if (crash == null)
                otherCarts + c_
            else
                otherCarts.filterNot { it == crash }
        }
    }
    return Model(tracks, newCarts)
}
```

The part 2 simulation is similar to part 1. Drop states until the end criteria is
met, then extract the information required from the next state.
```
fun part2(input: Model): Pos {
    val endState = timeline(input).dropWhile { m -> m.carts.size > 1 }.first()
    return endState.carts.first().pos
}
```
