package adventofcode2018.day2

import java.io.File

fun repeatCounts(s: String): Set<Int> =
    s.groupBy { it }.values.map { it.size }.toSet()

fun main(args: Array<String>) {
    val file = if (args.isEmpty()) "input.txt" else args[0]
    val input = File(file).readLines().map(String::trim)

    // Part 1
    val counts = input.map(::repeatCounts)
    val numPairs = counts.filter { s -> s.contains(2) }.size
    val numTriples = counts.filter { s -> s.contains(3) }.size
    println("Part 1 checksum: ${numPairs * numTriples}")
}
