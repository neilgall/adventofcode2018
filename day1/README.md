# Day 1

Simple stuff to get going. Read integers one per line from a file. That's a one-liner in Kotlin.

```
val input: List<Int> = File(path).readLines().map(String::toInt)
```

## Part 1
I love the storytelling that goes with the AoC problem descriptions, but this one
boils down to starting at zero and add each number in the input to an accumulator.
Sounds like summing the list.

```
val total = input.fold(0, Int::plus)
```
or, after I checked the docs:
```
val total = input.sum()
```

## Part 2
Slightly more tricky. A few things jump out at me:
* You might have to read the input data multiple times. Lazy infinite sequence?
* We want to find the second instance of some value. Simple set insertion will do.
* The value we want to find is somewhere in the running sum of all the inputs so far.
Haskell Lists have `scanl` which is like `foldl` but yields the accumulator at each
step rather than just at the end.
* The value we want to find is the first one which is already in the set.

I Googled how to build a lazy infinite sequence in Kotlin. Pretty simple - just
generate an infinite sequence of the original collection then flatten the structure.
```
fun <T> Sequence<T>.repeat() = generateSequence { asIterable() }.flatten()
```

`scanl` I implemented myself. It's kind of a combination of map and fold:
```
fun <T, U> Sequence<T>.scanl(initial: U, f: (U, T) -> U): Sequence<U> {
    var acc: U = initial
    return map { x -> acc = f(acc, x); acc }
}
```
That'll be a useful addition to the Kotlin bag of tricks. Shame it's not in the
standard library.

Once you have these parts the solution is pretty simple.
- make an infinitely repeating sequence of the input
- make a sequence of the running totals using `scanl`
- create an empty set and remove items from the start of the totals as long as
they can be added to the set
- the next item in the total sequence is the first one which repeats

```
val repeatedInput = input.asSequence().repeat()
val accumulatedInput = repeatedInput.scanl(0, Int::plus)
val unconsumed = accumulatedInput.dropWhile(mutableSetOf<Int>()::add)
val result = unconsumed.first()
```

Looking forward to tomorrow...