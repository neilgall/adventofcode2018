package adventofcode2018.day5

import java.io.File

fun reacts(x: Char, y: Char): Boolean =
    x != y && x.toLowerCase() == y.toLowerCase()

fun runReactions(s: List<Char>): List<Char> =
    s.fold(listOf<Char>()) { result, c ->
        if (result.isEmpty())
            listOf(c)
        else if (reacts(result.last(), c))
            result.dropLast(1)
        else
            result + c
    }

tailrec fun runUntilStable(s: List<Char>): List<Char> {
    val newS = runReactions(s)
    return if (newS == s) newS else runUntilStable(newS)
}

fun removeUnits(s: List<Char>, c: Char): List<Char> =
    s.filter { x -> x.toLowerCase() != c }

fun part1(s: CharArray): Int {
    return runUntilStable(s.toList()).size
}

fun part2(s: CharArray): Int? {
    val sl = s.toList()
    val sizes: List<Int> = ('a'..'z').map { c -> runUntilStable(removeUnits(sl, c)).size }
    return sizes.min()
}

fun main(args: Array<String>) {
    val input = File(args[0]).readText().trim().toCharArray()
    println("Part 1: ${part1(input)}")
    println("Part 2: ${part2(input)}")
}
