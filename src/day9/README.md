# Day 9
Circular data structures always come up in Advent of Code. You can obviously
implement it with a linear array or list and modulo arithmetic but I think
I'm going to have some fun with Kotlin today. Let's implement a real circular
data structure:
```
class Circle<T> {
    private var value: T
    private var prev: Circle<T>
    private var next: Circle<T>

    constructor(value: T, prev: Circle<T>? = null, next: Circle<T>? = null) {
        this.value = value
        this.prev = prev ?: this
        this.next = next ?: this
    }
}
```

An interesting property of this data structure is that a reference to any node
is a reference to the whole structure. So we don't need to separately keep
track of the overall structure and the current node. Which implies some
interesting possible operatins on it:
```
operator fun plus(n: Int): Circle<T> = when {
    n == 0 -> this
    n < 0  -> minus(-n)
    n > 0  -> next.plus(n-1)
}

operator fun minus(n: Int): Circle<T> = when {
    n == 0 -> this
    n < 0  -> plus(-n)
    n > 0  -> prev.minus(n-1)
}
```

That is we can say `circle + 2` to get the node 2 positions clockwise around
the circle, or `circle - 7` to get the node 7 positions counter-clockwise. It
doesn't matter if this loops around one or more times past the starting point
as it is a genuinely circular data structure.

The puzzle involves inserting and removing nodes, so let's implement those:
```
fun insertClockwise(value: T): Circle<T> {
    val node = Circle(value, this, next)
    next.prev = node
    this.next = node
    return node
}

fun insertAnticlockwise(value: T): Circle<T> {
    val node = Circle(value, prev, this)
    prev.next = node
    this.prev = node
    return node
}

fun remove(): Circle<T> {
    if (prev == this && next == this)
        throw IllegalStateException("can't remove the last node in a circle")
    prev.next = next
    next.prev = prev
    return next
}
```

Simple stuff, hard to get wrong. Just one corner case around removing the last
node as we don't have a representation of an empty circle. The game doesn't
require it anyway as we always start with the 0 marble.

Finally we need to see the content of the nodes in the circle. Kotlin's subscript
syntax makes sense for this:
```
operator fun get(n: Int): T = when {
    n == 0 -> value
    else   -> plus(n).get(0)
}
```

When I started implementing the game I realised the players play in a round-robin
fashion, and instead of keeping their scores in a `Map` or `Array` I could also
use a Circle! Insert the correct number of zeros at the start of the game, then
just use `player += 1` to move to the next player at each step. I also had to add
a subscript setter operation analagous to the getter, so the scores could be updated.

One final thing was needed to allow Circle to be used for the players: I had to be
able to find the highest score. A simple approach is to allow extraction of a certain
number of values:
```
fun take(n: Int): List<T> = when {
    n == 0 -> listOf()
    n > 0  -> listOf(value) + next.take(n-1)
    n < 0  -> prev.take(n+1) + listOf(value)
}
```

This data structure made implementing the game really easy:
```
fun game(marbles: Int, players: Int): Int {
    var circle = Circle(0)
    var player = (2..players).fold(Circle(0)) { c, _ -> c.insertClockwise(0) }

    for (marble in 1..marbles) {
        when {
            marble % 23 == 0 -> {
                player[0] += marble + circle[-7]
                circle = (circle - 7).remove()
            }
            else -> {
                circle = (circle + 1).insertClockwise(marble)
            }
        }
        player += 1
    }

    return player.take(players).maxBy { it } ?: throw IllegalStateException()
}
```

There is likely a numerical solution to this puzzle - there certainly looks like
there's some kind of binary pattern in the example - but even part 2 in my case
was only seven million nodes. I had to up the default JVM heap size but it's still
nothing to a modern computer, even this half-decade old Thinkpad.
