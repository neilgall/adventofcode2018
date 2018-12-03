package adventofcode2018.day2

import java.io.File

fun repeatCounts(s: String): Set<Int> =
    s.groupBy { it }.values.map { it.size }.toSet()

fun difference(s1: String, s2: String): Int = 
    s1.zip(s2, { x, y -> if (x == y) 0 else 1 }).sum()

fun common(s1: String, s2: String): String =
    s1.zip(s2, { x, y -> if (x == y) x.toString() else "" }).joinToString(separator="")

fun <T> pairs(xs: Collection<T>): List<Pair<T, T>> = when {
    xs.isEmpty() -> listOf() 
    else -> {
        val head = xs.first()
        val tail = xs.drop(1)
        (tail.map { head to it }) + pairs(tail)
    }
}

fun main(args: Array<String>) {
    val input = File(args[0]).readLines().map(String::trim)

    // Part 1
    val counts = input.map(::repeatCounts)
    val numPairs = counts.filter { s -> s.contains(2) }.size
    val numTriples = counts.filter { s -> s.contains(3) }.size
    println("Part 1 checksum: ${numPairs * numTriples}")

    // Part 2
    val differentByOnePairs = pairs(input).filter { (x, y) -> difference(x, y) == 1 }
    println(differentByOnePairs.map { (x, y) -> common(x, y) })
}
