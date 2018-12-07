package adventofcode2018.day7

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Parsing

typealias Name = String
data class Instruction(val before: Name, val after: Name)

fun parse(input: String): List<Instruction> {
    val instr = sequence(
        string("Step ").next(IDENTIFIER),
        string(" must be finished before step ").next(IDENTIFIER).followedBy(string(" can begin.")),
        ::Instruction
    )
    return instr.sepBy(WHITESPACES).parse(input)
}

// Part 1

fun allNames(input: List<Instruction>): Set<Name> =
    (input.map { it.before } + input.map { it.after }).toSet()

fun initials(input: List<Instruction>): Set<Name> =
    allNames(input) - (input.map { it.after }.toSet())

fun topologicalOrder(input: List<Instruction>): Sequence<Name> {
    fun from(n: Name): (Instruction) -> Boolean = { i -> i.before == n }
    fun to(n: Name): (Instruction) -> Boolean = { i -> i.after == n }

    var graph = input.toMutableList()
    val stack = initials(input).toMutableList()
    return generateSequence {
        when {
            stack.isEmpty() -> null
            else -> {
                val step = stack.removeAt(0)
                graph.filter(from(step)).forEach { edge ->
                    graph.remove(edge)
                    if (graph.none(to(edge.after))) {
                        stack.add(edge.after)
                    }
                }
                stack.sort()
                step
            }
        }
    }
}

fun part1(input: List<Instruction>): String {
    return topologicalOrder(input).joinToString("")
}

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    println("Part 1: ${part1(input)}")
}