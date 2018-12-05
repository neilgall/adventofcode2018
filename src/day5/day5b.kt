package adventofcode2018.day5b
// Couldn't get this one to work

import java.io.File

fun reacts(x: Char, y: Char): Boolean =
    x != y && x.toLowerCase() == y.toLowerCase()

fun reacts(s: CharArray, r: IntRange): Boolean =
    1 < r.start && r.endInclusive < s.size-1 && reacts(s[r.start-1], s[r.endInclusive+1])

fun IntRange.grow(): IntRange = start-1 .. endInclusive+1

fun List<IntRange>.removeOverlapping(): List<IntRange> =
    fold(listOf<IntRange>()) { result: List<IntRange>, curr: IntRange ->
        if (result.isEmpty())
            listOf(curr)
        else if (result.last().contains(curr.start)) 
            result
        else
            result + listOf(curr)
    }

fun List<IntRange>.mergeAdjacent(): List<IntRange> =
    fold(listOf<IntRange>()) { result: List<IntRange>, curr: IntRange ->
        if (result.isEmpty())
            listOf(curr)
        else if (result.last().endInclusive + 1 == curr.start)
            result.dropLast(1) + listOf(result.last().start .. curr.endInclusive)
        else
            result + listOf(curr)
    }

fun initialReactions(s: CharArray): List<IntRange> =
    s.zip(s.drop(1)).mapIndexedNotNull { pos: Int, (x: Char, y: Char) ->
        if (reacts(x, y)) pos..pos+1 else null
    }.removeOverlapping().mergeAdjacent()

fun growReactions(s: CharArray, reactions: List<IntRange>): List<IntRange> =
    reactions.map { r ->
        if (reacts(s, r)) r.grow() else r
    }.removeOverlapping().mergeAdjacent()

tailrec fun growUntilStable(s: CharArray, reactions: List<IntRange>): List<IntRange> {
    val newReactions = growReactions(s, reactions)
    return if (newReactions == reactions) reactions else growUntilStable(s, newReactions)
}

fun List<IntRange>.totalLength(): Int =
    map { r -> r.endInclusive - r.start + 1 }.sum()

fun main(args: Array<String>) {
    val input = File(args[0]).readText().trim().toCharArray()
    val initial = initialReactions(input)
    val stable = growUntilStable(input, initial)
    val remaining = input.size - stable.totalLength()
    println("Part 1: ${remaining}")
}