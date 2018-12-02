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

