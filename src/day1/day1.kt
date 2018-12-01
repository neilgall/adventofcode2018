package adventofcode2018.day1

import java.io.File
import adventofcode2018.toolbox.*

fun main(args: Array<String>) {
    val input: List<Int> = File("input.txt").readLines().map(String::toInt)

    // part 1
    val result = input.sum()
    println("Part 1 result: ${result}")

    // part 2
    val repeatedInput = input.asSequence().repeat()
    val accumulatedInput = repeatedInput.scanl(0, Int::plus)
    val unconsumed = accumulatedInput.dropWhile(mutableSetOf<Int>()::add)
    println("Part 2 result: ${unconsumed.first()}")
}