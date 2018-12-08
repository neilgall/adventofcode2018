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

fun topologicalOrder(input: List<Instruction>): List<Name> {
    fun from(n: Name): (Instruction) -> Boolean = { i -> i.before == n }
    fun to(n: Name): (Instruction) -> Boolean = { i -> i.after == n }

    data class Graph(val edges: List<Instruction>) {
        fun removeFrom(name: Name): Pair<Graph, List<Instruction>> {
            val (remove, retain) = edges.partition(from(name))
            return Pair(Graph(retain), remove)
        }
        fun noneTo(name: Name): Boolean = edges.none(to(name))
    }

    tailrec fun sort(graph: List<Instruction>, stack: List<Name>, result: List<Name>): List<Name> =
        if (stack.isEmpty())
            result
        else {
            val step = stack.first()
            val (edges, graph_) = graph.partition(from(step))
            val next = edges.filter { e -> graph_.none(to(e.after)) }.map { e -> e.after }
            val stack_ = (stack.drop(1) + next).sorted()
            sort(graph_, stack_, result + step)            
        }

    return sort(input, initials(input).toList(), listOf())
}

fun part1(input: List<Instruction>): String {
    return topologicalOrder(input).joinToString("")
}

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    println("Part 1: ${part1(input)}")
}