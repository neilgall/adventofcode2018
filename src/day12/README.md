# Day 12
Ahhh, a one-dimensional Conway's [Game of Life](https://en.wikipedia.org/wiki/Conway's_Game_of_Life).

Part 1 is easy enough - the tricky part is that the space is unbounded. I modelled
the row of plants as a boolean array where the first and last are always true (i.e.
the left and rightmost plants) and the position of the leftmost plant is stored
separately. Thus if the row grows to the right the array gets longer and the origin
stays the same, and if it grows to the left the array also gets longer but the
origin decreases.

```
data class State(val plants: BooleanArray, val origin: Long)
```

At first I translated the rules into a map of `BooleanArray` to `Boolean` but the
lookups seemed to have trouble so I converted the five bits of a rule's left hand
side to an integer and used that as the key:

```
data class Region(val value: Int) {
    constructor(states: List<Boolean>):
        this(states.fold(0) { v, b -> (v shl 1) or (if (b) 1 else 0) })
    override fun toString(): String =
        listOf(16,8,4,2,1).map { b -> if (value and b != 0) '#' else '.'}.joinToString("")
}
```
The use of `&&` and `||` for logic and `and` and `or` for boolean operations in
Kotlin seems the wrong way round. Yes you want to bring C / Java programmers with you when
you introduce a new language, but in this case I'd have gone for Python's readability.

## Parsing

Parsing the input data of course uses JParsec. It's just so much easier not to have
to deal with questions like "did I find all the parts?" or "does the rule have exactly
5 positions on the left" etc. It either parses or it throws an error and tells you
the line and column where the parsing failed.

```
fun parse(input: String): Game {
    val plant = or(isChar('.').retn(false), isChar('#').retn(true))

    val initial = string("initial state: ")
        .next(plant.many())
        .map { ps -> State(ps, 0) }

    val rule = sequence(
        plant.times(5).map(::Region),
        string(" => ").next(plant),
        { r, q -> r to q }
    )

    val parser: Parser<Game> = sequence(
        initial.followedBy(WHITESPACES),
        rule.sepBy(WHITESPACES).map { rs -> rs.toMap() },
        ::Game
    )
    return parser.parse(input)
}
```

# Part 1

Running one generation of the game involves:
1. Pad the state with 'false' at the start and the end to aid region matches.
2. Split the state into sets of five positions, called regions.
3. Look up each region in the rules to get the value at that position for the next state
4. Trim empty space off the ends and calculate the new origin
5. Also, do all this in a `Sequence` so the memory footprint is sequential rather than all at once.

```
val buffer: BooleanArray = BooleanArray(4) { false }

fun State.run(rules: Rules): State {
    val buffered: BooleanArray = buffer + plants + buffer

    val regions: Sequence<Region> = (0..(buffered.size-5))
        .asSequence()
        .map { i -> Region(buffered.slice(i..i+4)) }

    val output = regions
        .map { r -> rules.getOrDefault(r, false) }
        .toList()
        .dropLastWhile { !it }

    val leadingEmpties = output.takeWhile { !it }.count()
    return State(output.dropWhile { !it }, origin - 2 + leadingEmpties)
}
```

Part 1 is just doing that 20 times.


# Part 2
At first you read part 2 and think "oh, just up the iteration count to 50 billion".

No.

The secret is that the rules inevitably lead to a repeating pattern. Work out where
the loop is, work out how many loops occur in 50 billion iterations, then you only
need to run the loop once. The tricky part in this puzzle is that the same pattern
of plants may occur with a different origin. Conway's original Game of Life is famous
for its walker structures that move across the 2D space, replicating themselves
perfectly.

I built a map keyed by a string representation of the plant pattern. When the current
state already exists in the map, we have detected a loop. The value stored in the map
describes the start of the loop. Then it's just a matter of determining the number
of loops, the length of the last incomplete loop (if present), assemble the state at
the end of the final loop and finish the sequence. All while avoiding 50 billion
opportunities for an off-by-one error.

```
    val total: Long = 50_000_000_000
    val loopStart = states[state.str]!!
    val loopSize = count - loopStart.index
    val loops = (total - loopStart.index) / loopSize
    val originInc = state.origin - loopStart.origin

    val lastLoopStartState = State(state.plants, loopStart.origin + loops * originInc)
    val lastState = if (loopSize == 1) lastLoopStartState else {
        val lastLoopLength = total % loopSize
        lastLoopStartState.run(lastLoopLength, game.rules)
    }
```

In my case the loop was only one iteration long, but I suspect that's not true for
everyone. My `lastState` calculation's else branch is therefore never taken so I
don't have full confidence that's the correct logic.
