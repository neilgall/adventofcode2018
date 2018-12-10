# Day 10
This one is quite interesting. Integrating moving points over time is straightforward
enough but how do we detect the moment a message appears? I'm going to assume it's the
point the bounding box of all the points is smallest.

First we'll reuse and extend some of the geometric primitives from [Day 6](../src/day6).
```
data class Position(val x: Int, val y: Int)
data class Velocity(val x: Int, val y: Int)
data class Box(val x: IntRange, val y: IntRange)
data class Point(val position: Position, val velocity: Velocity)
typealias Time = Int

operator fun Box.plus(p: Position): Box = Box(x.extend(p.x), y.extend(p.y))

fun Collection<Position>.boundingBox(): Box = fold(Box.EMPTY, Box::plus)
```
And we'll add a function to get a point's position at any time `t`:
```
fun Point.integrate(t: Time): Position =
    Position(x = position.x + velocity.x * t,
             y = position.y + velocity.y * t)
```

We'll solve the problem by iterating over a large period of time and computing the
bounding box for the points at each time. Then take the smallest of these bounding
boxes:
```
val sizes: List<Pair<Int, Box>> = (1..100000).map { t -> Pair(t, input.map { p -> p.integrate(t) }.boundingBox()) }
val time: Int = sizes.minBy { (_, box) -> box.area }?.first
```

Then we'll render the points into beautiful ASCII art at that time by building a 2D array
of characters and filling in the ones at the right positions:
```
fun Collection<Position>.render(): String {
    val box: Box = boundingBox()
    var cells = Array<Array<Char>>(box.height) { Array<Char>(box.width) { '.' }}
    forEach { p ->
        cells[p.y - box.y.start][p.x - box.x.start] = '#'
    }
    return cells.map { row -> row.joinToString("") }.joinToString("\n")
}
```

Part 2 was either disappointing or a relief, depending on your viewpoint, as my part 1 
solution already had the answer.