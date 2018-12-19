# Day 17
Thirsty elves! As a Scotsman a lack of drinking water is an alien concept but we have to help where we can. I remember when this kind of programming challenge seemed impossibly hard but I've been looking forward to one all month. It's basically a depth-first search (no pun intended, although maybe it was by the Advent of Code authors?) with a slight twist: when the water reaches a level where it can flow horizontally it goes in _both_ directions, which amounts to breadth-first searching at those branches. You could be strict with the one-square-at-a-time modelling but it's stated that the water flow is infinite, so magically doubling the water volume at a horizontal branch (by searching both ways) makes no difference to the end result. That's the beauty of thinking of the data structure as a whole rather than the individual nodes in it.

First we need to get that data into the program. We make a data model for the veins of clay described in the input data:
```
sealed class Vein {
	data class Vertical(val x: Int, val y: IntRange): Vein()
	data class Horizontal(val y: Int, val x: IntRange): Vein()
}
```
... and we reach for our favourite parser combinator library!
```
fun parse(input: String): List<Vein> {
	val integer = INTEGER.map(String::toInt)
	val integerRange = sequence(integer, string("..").next(integer)) { x, y -> x..y }
	val verticalVein: Parser<Vein> = sequence(string("x=").next(integer), string(", y=").next(integerRange), Vein::Vertical)
	val horizontalVein: Parser<Vein> = sequence(string("y=").next(integer), string(", x=").next(integerRange), Vein::Horizontal)
	val vein = or(verticalVein, horizontalVein)
	return vein.sepBy(WHITESPACES).parse(input.trim())
}
```

## Part 1
I *really* struggled with this one, taking three attempts. At first I did a recursive search as hinted at above, but it ran out of memory and heap space. Back to the drawing board, I stuck with the recursion (adding the use of Kotlin's `tailrec`), building a set of mutually recursive functions representing the various states of flowing down, flowing across inside container, flowing up when there are walls on both sides. I got it to mostly work but couldn't get the right answer out. So I abandoned that and ended up with the solution here.

The space is represented as a 2D grid in which I fill in the veins of clay first, then fill in with flowing and standing water as the filling algorithm progresses. Flows can split so there is a set of current flow positions and the algorithm proceeds until this is empty. The first condition handled is easy - when we reach the bottom of the bounding box, the water drains out so we just remove that flow.
```
	val flows = mutableSetOf<Pos>(Pos(500, 0))
	
	while (!flows.isEmpty()) {
		val flow = flows.first()
		flows.remove(flow)

		if (flow.y == boundingBox.y.endInclusive) {
			this[flow] = Fill.FLOWING_WATER
			continue
		}
		...
```

We need to look at where the water is going and act appropriately.
```
		when (this[flow.down()]) {
```
If the space is empty the water just flows down. If it hits already flowing water the streams just merge so we abort the current one.
```
			Fill.EMPTY -> {
				this[flow] = Fill.FLOWING_WATER
				flows += flow.down()
			}

			Fill.FLOWING_WATER -> {
				this[flow] = Fill.FLOWING_WATER
			}
```
If the space has standing water or clay, it gets more complex. We scan horizontally both left and right looking for features. The interesting features are walls, which contain the water, and edges, over which it flows. We represent these with a really simple sum type.
```
sealed class Feature {
	abstract val pos: Pos
	data class Wall(override val pos: Pos): Feature()
	data class Edge(override val pos: Pos): Feature()
}
```

We're going to fill a horizontal row from the flow point to the feature on both sides. But if either side is an edge we'll fill with flowing water, and if both sides are walls we'll fill with still water then move the flow _upwards_ to fill up the container.
```
			Fill.CLAY, Fill.STILL_WATER -> {
				val featureLeft = findFeature(flow, Pos::left)
				val featureRight = findFeature(flow, Pos::right)
				val fillRange = flow.to(featureLeft.pos) + flow.to(featureRight.pos)

				if (featureLeft is Feature.Edge || featureRight is Feature.Edge) {
					fill(fillRange, Fill.FLOWING_WATER)
					if (featureLeft is Feature.Edge) flows += featureLeft.pos
					if (featureRight is Feature.Edge) flows += featureRight.pos
				} else {
					fill(fillRange, Fill.STILL_WATER)
					flows += flow.up()
				}
			}
```

The rest is just ancillary functions. Finding features is just a scan for the appropriate geometry:
```
fun Scan.findFeature(pos: Pos, dir: (Pos) -> Pos): Feature {
	var p = pos
	while (contains(dir(p))) {
		if (this[dir(p)] == Fill.CLAY)
			return Feature.Wall(p)
		else if (this[p.down()] == Fill.EMPTY || this[p.down()] == Fill.FLOWING_WATER)
			return Feature.Edge(p)
		p = dir(p)
	}
	throw IllegalStateException("Can't find a feature at row ${pos.y}")
}
```

Sequences came in handy for generating the positions for each side of a row, then concatenating them to get the full set of position to fill.
```
fun Pos.to(end: Pos): Sequence<Pos> {
	val dir = if (y == end.y) {
		if (x > end.x) Pos::left else Pos::right
	} else if (x == end.x) {
		if (y > end.y) Pos::up else Pos::down
	} else throw IllegalArgumentException("Positions must agree on one axis")
	var p = this
	return sequence {
		while (p != end) {
			yield(p)
			p = dir(p)
		}
		yield(end)
	}
}

```

Answering the problem questions was a simple query over the grid.
```
fun part1(input: Scan): Int {
	input.fill()
	return input.boundingBox.positions().count { p ->
		input[p] == Fill.STILL_WATER || input[p] == Fill.FLOWING_WATER 
	}
}
```
