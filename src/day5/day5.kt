package adventofcode2018.day5

import java.io.File

fun reacts(x: Char, y: Char): Boolean =
    x != y && x.toLowerCase() == y.toLowerCase()

fun runReactions(s: List<Char>): Int =
    s.fold(listOf<Char>()) { stack: List<Char>, c: Char ->
        if (stack.isEmpty() || !reacts(stack.last(), c))
            stack + c
        else
            stack.dropLast(1)
    }.size

fun removeUnits(s: List<Char>, c: Char): List<Char> =
    s.filter { x -> x.toLowerCase() != c }

fun part1(s: CharArray): Int {
    return runReactions(s.toList())
}

fun part2(s: CharArray): Int? {
    val sl = s.toList()
    val sizes: List<Int> = ('a'..'z').map { c -> runReactions(removeUnits(sl, c)) }
    return sizes.min()
}

fun main(args: Array<String>) {
    val input = File(args[0]).readText().trim().toCharArray()
    println("Part 1: ${part1(input)}")
    println("Part 2: ${part2(input)}")
}
