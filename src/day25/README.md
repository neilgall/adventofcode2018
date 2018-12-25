# Day 25
Last year's Christmas Day was a little bonus star with no actual puzzle to solve so I was expecting the same. But no there's real work to do today, although thankfully it's pretty straightforward.

## Parsing
```kotlin
data class Point(val w: Int, val x: Int, val y: Int, val z: Int)

fun parse(input: String): List<Point> {
	val integer = or(
		INTEGER.map(String::toInt),
		isChar('-').next(INTEGER).map { s -> -s.toInt() }
	)

	val point = sequence(
		integer.followedBy(isChar(',')),
		integer.followedBy(isChar(',')),
		integer.followedBy(isChar(',')),
		integer,
		::Point
	)

	return point.sepBy(WHITESPACES).parse(input.trim())
}
```

## Part1
Manhattan distance. We've done this before:
```kotlin
typealias Manhattan = Int

fun Point.distance(p: Point): Manhattan =
	Math.abs(w - p.w) + Math.abs(x - p.x) + Math.abs(y - p.y) + Math.abs(z - p.z)
```

You could model the constellations as a set of set of points but type inference gets a bit hairy in Kotlin so we'll make a wrapper. The inline classes feature coming in the future will make this possible with no runtime overhead. I'll add a few syntactic conveniences such as adding points to constellations and merging them.

```kotlin
data class Constellation(val points: Set<Point>) {
	constructor(p: Point): this(setOf(p))
}

fun Constellation.shouldContain(point: Point): Boolean = 
	points.any { p -> p.distance(point) <= 3 }

operator fun Constellation.plus(p: Point): Constellation =
	Constellation(points + p)

operator fun Constellation.plus(c: Constellation): Constellation = 
	Constellation(points + c.points)

fun Collection<Constellation>.merge(p: Point): Constellation =
	fold(Constellation(p), Constellation::plus)
```

With all that done, solving part 1 is a fold over the input points. For each point, partition the existing constellations into the ones it should join and the ones it should not. Merge the ones that the point should join and leave the others alone.
```kotlin
fun part1(input: Collection<Point>): Int {
	val constellations = input.fold(setOf<Constellation>()) { cs, p ->
		val (join, dontJoin) = cs.partition { c -> c.shouldContain(p) }
		(dontJoin + join.merge(p)).toSet()
	}
	return constellations.size
}
```
