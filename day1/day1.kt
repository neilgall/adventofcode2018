package adventofcode2018

import java.io.File

fun <T> Sequence<T>.repeat() = generateSequence { asIterable() }.flatten()

fun <T, U> Sequence<T>.scanl(initial: U, f: (U, T) -> U): Sequence<U> {
    var acc: U = initial
    return map { x -> acc = f(acc, x); acc }
}

fun main(args: Array<String>) {
    val input: List<Int> = File("day1/input.txt").readLines().map(String::toInt)

    // part 1
    val result = input.fold(0, Int::plus)
    println("Part 1 result: ${result}")

    // part 2
    val repeatedInput = input.asSequence().repeat()
    val accumulatedInput = repeatedInput.scanl(0, Int::plus)
    val unconsumed = accumulatedInput.dropWhile(mutableSetOf<Int>()::add)
    println("Part 2 result: ${unconsumed.first()}")
}