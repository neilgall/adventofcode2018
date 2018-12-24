# Day 23
We're closing in on Christmas Day. Yesterday's was tough and if I'm honest I'm getting tired. So it was nice to find a straightforward part 1 at least.

## Model
Simple stuff, we've done this many times already.
```
data class Pos(val x: Int, val y: Int, val z: Int)

data class Nanobot(val pos: Pos, val radius: Int)

typealias Manhattan = Int

operator fun Pos.minus(other: Pos): Manhattan = 
	Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z)
```

## Parser
Parser combinators are also bread and butter. I've used them on nearly 2 out of 3 days this year, and at this point I'm writing them without much reference to the documentation.
```
fun parse(input: String): List<Nanobot> {
	val integer = or(
		string("-").next(INTEGER).map { s -> -s.toInt() },
		INTEGER.map(String::toInt)
	)

	val pos = sequence(string("pos=<").next(integer),
					   string(",").next(integer),
					   string(",").next(integer).followedBy(string(">")),
					   ::Pos)
	
	val nanobot = sequence(pos, string(", r=").next(integer), ::Nanobot)
	
	return nanobot.sepBy(WHITESPACES).parse(input.trim())
}
```

## Part 1
Easy stuff.
```
fun part1(nanobots: List<Nanobot>): Int {
	val strongest = nanobots.maxBy { n -> n.radius }!!
	return nanobots.count { n -> (strongest.pos - n.pos) <= strongest.radius }
}
```

## Part 2
Oh wow, another doozy. I'll get back to you on this one!

