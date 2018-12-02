# Day 2
Ahh, a classic. Sometimes I think Advent of Code should be Advent of Sorting and Searching.

## Part 1
There's a naive implementation involving iterating the input data imperatively and building
a bunch of complex maps but that's hard to get right and in six months' that kind of code
becomes undecipherable. Let's peruse the library docs...

`kotlin.String` has [`groupBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/group-by.html)
which does a bunch of the work for us. If we group by the letters of the word themselves, we'll
get a map whose values are lists, whose lengths are the counts of repeated letters.

```
fun repeatCounts(s: String): Set<Int> = s.groupBy { it }.values.map { it.size }.toSet()
```

Map this over the input and we'll have a set of counts for each input string. Filter the
sets for those containing 2 and 3 and we have the checksum factors.

```
val counts = input.map(::repeatCounts)
val numPairs = counts.filter { s -> s.contains(2) }.size
val numTriples = counts.filter { s -> s.contains(3) }.size
```

## Part 2
Hmmm, finding pairs matching some difference criteria in a large set sounds like an O(N²)
algorithm. My first thought is whether there's a trick to speed it up. Is there a
way to sort the input data so the interesting differences occur in adjacent pairs?
If so then finding them becomes a linear scan.

Nothing jumped out at me after a few minutes thinking about it so let's try the
brute force way. Get all pairs of input strings and filter the pairs that differ
by only one character.

First a string difference function. When working with pairs reach for a `zip` function.
`CharSequence` has a particularly useful [zip](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/zip.html)
that takes a transformation function so we can do two steps in one:
```
fun difference(s1: String, s2: String): Int = 
    s1.zip(s2, { (x, y) -> if (x == y) then 0 else 1 }).sum()
```

For each character in the same position count a 1 if they're different and
a zero if they're the same. Add them up. Kotlin destructuring of `Pair<Char, Char>`
comes in handy in the lambda.

Then we need to find all the pairs. Simple with classic list recursion, and of
course you write this function generically to reduce the scope for mistakes:
```
fun <T> pairs(xs: Collection<T>): List<Pair<T, T>> = when {
    xs.isEmpty() -> listOf() 
    else -> {
        val head = xs.first()
        val tail = xs.drop(1)
        (tail.map { head to it }) + pairs(tail)
    }
}
```

Finally we choose only those pairs who have a difference of exactly 1:
```
val differentByOnePairs = pairs(input).filter { (x, y) -> difference(x, y) == 1 }
```

Finally we find the common letters from a surviving pair. This is a minor variation
on the `difference` function we wrote above:
```
fun common(s1: String, s2: String): String =
    s1.zip(s2, { x, y -> if (x == y) x.toString() else "" }).joinToString(separator="")
```

And the solution is obtained by running this on every selected pair:
```
differentByOnePairs.map { (x, y) -> common(x, y) }
```

Some minor tidy up would be possible using `different` and `common` functions which
took `Pair<String,String>` as a single argument, but I quite like keeping it explicit
and those functions might be more generally useful with a simpler signature.

Nice one today. See you tomorrow.
