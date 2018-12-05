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

fun main(args: Array<String>) {
    val input = File(args[0]).readText().trim().toCharArray()
    val result = runUntilStable(input.toList())
    println("Part 1: ${result.size}")
}
