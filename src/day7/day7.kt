package adventofcode2018.day7

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Parsing

typealias Name = String
data class Instruction(val before: Name, val after: Name) {
    override fun toString(): String = "${before}:${after}"
}

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

fun from(n: Name): (Instruction) -> Boolean = { i -> i.before == n }
fun to(n: Name): (Instruction) -> Boolean = { i -> i.after == n }

fun topologicalOrder(input: List<Instruction>): List<Name> {
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

// Part 2

typealias Time = Int
data class Work(val name: Name, val remaining: Time): Comparable<Work> {
    companion object {
        fun time(name: Name): Time = name[0] - 'A' + 61
    }

    constructor(name: Name): this(name, time(name))

    fun doWork(time: Time): Work = Work(name, remaining - time)

    fun inProgress(): Boolean = remaining < time(name)
    fun done(): Boolean = remaining <= 0

    fun tag(): String = "${if (inProgress()) 0 else 1}${name}"

    override fun compareTo(other: Work): Int = tag().compareTo(other.tag())
    override fun toString(): String = "${name}(${remaining})"
}

data class Result(val steps: List<Name>, val time: Time) {
    fun add(done: List<Work>, progress: Time) = Result(steps + done.map { w -> w.name }, time + progress)
    override fun toString(): String = "${steps}(${time})"
}

fun parallelTopologicalOrder(input: List<Instruction>, workers: Int): Result {
    tailrec fun sort(graph: List<Instruction>, stack: List<Work>, result: Result): Result =
        if (stack.isEmpty())
            result
        else {
            val active = stack.take(workers)
            val progress = active.minBy { w -> w.remaining }?.remaining ?: throw IllegalStateException()
            val advance = active.map { w -> w.doWork(progress) }
            val done = advance.filter(Work::done)

            val (edges, graph_) = done.fold(Pair(setOf<Instruction>(), graph)) { (e, g), step -> 
                val (e_, g_) = g.partition(from(step.name))
                Pair(e + e_, g_)
            }
            val next = edges.filter { e -> graph_.none(to(e.after)) }.map { e -> Work(e.after) }
            val stack_ = (stack.drop(workers) + advance.filterNot(Work::done) + next).sorted()
            val result_ = result.add(done, progress)

            println("stack ${stack} progress ${progress} done ${done} edges ${edges} next ${next} result ${result_}")

            sort(graph_, stack_, result_)
        }

    val initialWork = initials(input).map(::Work)
    return sort(input, initialWork, Result(listOf(), 0))
}

fun part2(input: List<Instruction>): Pair<String, Time> {
    val (order, time) = parallelTopologicalOrder(input, 5)
    return Pair(order.joinToString(""), time)
}

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    println("Part 1: ${part1(input)}")
    println("Part 2: ${part2(input)}")
}