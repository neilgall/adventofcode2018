# Day 6

I'm starting to see what's going on here. The elves of 1518 have
somehow procured a future [book](https://en.wikipedia.org/wiki/The_Art_of_Computer_Programming)
of classic programming algorithms. Today there was a little distraction
in problem description around infinite spaces and equidistant points,
but it boils down to a pixel fill algorithm. If you ever worked on a
classic 1980s-90s style paint program you'll recognise it.

First let's make some geometric primitives:

```
data class Pos(val x: Int, val y: Int)

data class Box(val x: IntRange, val y: IntRange)

enum class Dir { UP, DOWN, LEFT, RIGHT }

typealias Manhattan = Int
```

For readable code we'll add some useful operations, using Kotlin's carefully
limited operator overloading where appropriate:

```
fun Box.contains(p: Pos): Boolean = x.contains(p.x) && y.contains(p.y)

operator fun Pos.minus(other: Pos): Manhattan =
    Math.abs(x - other.x) + Math.abs(y - other.y)

operator fun Pos.plus(dir: Dir) = when(dir) {
    Dir.UP -> Pos(x, y-1)
    Dir.DOWN -> Pos(x, y+1)
    Dir.LEFT -> Pos(x-1, y)
    Dir.RIGHT -> Pos(x+1, y)
}

fun around(p: Pos): Set<Pos> = enumValues<Dir>().map { dir -> p + dir }.toSet()
```

We need a data model for our coordinates, and a parser. I know it's just two integers
separated by a comma, but I hope by now you see what I mean by the One True Way of
parsing text:

```
typealias CoordinateID = Int
data class Coordinate(val id: CoordinateID, val pos: Pos)

fun parse(input: String): List<Coordinate> {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
    val pos: Parser<Pos> = sequence(integer.followedBy(string(", ")), integer, ::Pos)
    val positions: List<Pos> = pos.sepBy(WHITESPACES).parse(input)

    val ids: Sequence<Int> = generateSequence(0) { it + 1 }
    return positions.asSequence().zip(ids).map { (pos, id) -> Coordinate(id, pos) }.toList()
}
```

The last two lines make an infinite sequence of integers and zip it with the parsed
coordinates so each coordinate gets a "name".

The problem talks about an infinite space, but the insight you need to solve it is
that any area which reaches the edge of the "known" space is effectively infinite.
The known space is the extent of the input coordinates in every direction, so let's
compute that:

```
fun IntRange.extend(i: Int): IntRange = minOf(start, i) .. maxOf(endInclusive, i)

operator fun Box.plus(p: Pos): Box = Box(x.extend(p.x), y.extend(p.y))

fun spaceExtent(coords: Collection<Coordinate>): Box =
    coords.map { c -> c.pos }.fold(Box.EMPTY, Box::plus)
```

Breaking things down into tiny operations makes everything much easier to understand,
means you're composing ideas at the same level of abstraction and frankly leaves
fewer places for bugs to creep in.

We know the size of the space, now we need the area filling data model. A cell is
either unclaimed, has a coordinate on it, is claimed by a single coordinate or is
equidistant to two or more coordinates. It's a sum type:

```
sealed class Cell {
    object Unclaimed: Cell()
    data class Coordinate(val id: CoordinateID): Cell()
    data class Claimed(val id: CoordinateID, val distance: Manhattan): Cell()
    data class Equidistant(val distance: Manhattan): Cell()
}
```

And we represent the space with a simple two-dimensional array of these:

```
data class Space(val box: Box) {
    val cells: Array<Array<Cell>>

    init {
        val width = box.x.endInclusive - box.x.start + 1
        val height = box.y.endInclusive - box.y.start + 1
        cells = Array<Array<Cell>>(height) { Array<Cell>(width) { Cell.Unclaimed } }
    }
```

Now the fill algorithm. I've written this before, so I'm happy to say once it
compiled this actually gave the correct result first time. The only change I had
to make was to change it from stack-based recursion to a heap-based stack of 
pending operations to cope with the size of the main input data.

```
fun Space.fill(c: Coordinate) {
    val stack = mutableSetOf<Pos>()

    fun fill(p: Pos): Set<Pos> {
        if (!box.contains(p)) return setOf()

        val distance: Manhattan = p - c.pos
        val cell = this[p]
        val newCell = when (cell) {
            is Cell.Unclaimed -> Cell.Claimed(c.id, distance)
            is Cell.Claimed -> when {
                cell.id == c.id -> cell
                distance < cell.distance -> Cell.Claimed(c.id, distance)
                distance == cell.distance -> Cell.Equidistant(distance)
                else -> cell
            }
            is Cell.Equidistant -> when {
                distance < cell.distance -> Cell.Claimed(c.id, distance)
                else -> cell
            }
            is Cell.Coordinate -> cell
        }
        return if (newCell != cell) {
            this[p] = newCell
            around(p)
        } else {
            setOf()
        }
    }

    stack.addAll(around(c.pos))
    while (!stack.isEmpty()) {
        val p = stack.first()
        stack.addAll(fill(p))
        stack.remove(p)
    }
}
```

Things to note:
1. Recursive functions are often best implemented with a helper and a "starter" method.
2. In the original version the inner `fill()` helper called itself recursively. Now it
returns the next set of cells to fill, and the outer loop processes these.
3. The first check is that we've remained inside the bounding box. You could note here
that the affected ID has an infinite area but I feel that's conflating the fill and
area check parts of the problem. Merge them later if performance is an issue.
4. The `when` block looks at the current state of the cell and computes what to do.
If the cell is unclaimed or closer to the current coordinate, we claim it. If it's the
same distant it becomes `Equidistant`. Some of the cases leave it alone.
5. Finally if we've made a change to the cell, update the 2D array and continue to the
surrounding set of cells.

## Part 1

The question in part 1 is to determine the largest finite area. We need a sum type!

```
sealed class Area {
    object Infinite: Area()
    data class Finite(val size: Int): Area()
}
```

To calculate the area for a coordinate ID I took a very simple approach:
1. If any edge position claims the ID, the area will be infinite
2. Otherwise scan the whole space and count the claimed cells.

```
fun Space.areaForCoordinate(id: CoordinateID): Area {
    val claimed: (Cell) -> Boolean = { cell -> cell.claimedBy(id) }

    return if (topEdge().any(claimed) || bottomEdge().any(claimed) || leftEdge().any(claimed) || rightEdge().any(claimed))
        Area.Infinite
    else
        Area.Finite(cells.map { row -> row.count(claimed) }.sum())
}
```

This is not the most efficient form of the algorithm by any means. It will check the
same cell many times during a pass. It runs in 1.5 seconds on my Thinkpad though.

## Part 2

Part 2 was a bit disappointing as it didn't really build on the first much. I refactored
the `Space` class to be generic in its content, so I could make a space of manhattan
distances. Filling it was easy:

```
fun Space<Manhattan>.fillDistances(coords: Collection<Coordinate>) {
    box.y.forEach { y ->
        box.x.forEach { x -> 
            val pos = Pos(x, y)
            this[pos] = coords.map { c -> c.pos - pos }.sum()
        }
    }
}
```

Then the size of the area under the threshold is `space.count { d -> d < max }`