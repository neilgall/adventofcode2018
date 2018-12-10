package adventofcode2018.day10

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// data model

fun IntRange.extend(i: Int) =
    if (this == IntRange.EMPTY) IntRange(i, i) else IntRange(minOf(start, i), maxOf(i, endInclusive))

data class Position(val x: Int, val y: Int)

data class Velocity(val x: Int, val y: Int)

data class Box(val x: IntRange, val y: IntRange) {
    companion object {
        val EMPTY = Box(IntRange.EMPTY, IntRange.EMPTY)
    }
    val height: Int = y.endInclusive - y.start + 1
    val width: Int = x.endInclusive - x.start + 1
    val area: Long = width.toLong() * height.toLong()
}

data class Point(val position: Position, val velocity: Velocity)

typealias Time = Int

fun Point.integrate(t: Time): Position =
    Position(x = position.x + velocity.x * t,
             y = position.y + velocity.y * t)

operator fun Box.plus(p: Position): Box = Box(x.extend(p.x), y.extend(p.y))

fun Collection<Position>.boundingBox(): Box = fold(Box.EMPTY, Box::plus)

// parsing

fun parse(input: String): List<Point> {
    val integer: Parser<Int> = or(INTEGER.map(String::toInt), string("-").next(INTEGER).map { s -> -s.toInt() })
    val spaces = WHITESPACES.optional(null)

    fun <T> intPair(name: String, cons: (Int, Int) -> T): Parser<T> =
        sequence(
            string("${name}=<").followedBy(spaces).next(integer),
            string(",").followedBy(spaces).next(integer).followedBy(string(">")),
            cons
        ).followedBy(spaces)
    val position = intPair("position", ::Position)
    val velocity = intPair("velocity", ::Velocity)
    val point = sequence(position, velocity, ::Point)

    return point.many().parse(input.trim())
}

// rendering 

fun Collection<Position>.render(): String {
    val box: Box = boundingBox()
    var cells = Array<Array<Char>>(box.height) { Array<Char>(box.width) { '.' }}
    forEach { p ->
        cells[p.y - box.y.start][p.x - box.x.start] = '#'
    }
    return cells.map { row -> row.joinToString("") }.joinToString("\n")
}

fun solve(input: Collection<Point>): Pair<Int, String> {
    val sizes: List<Pair<Int, Box>> = (1..100000).map { t -> Pair(t, input.map { p -> p.integrate(t) }.boundingBox()) }
    val time: Int = sizes.minBy { (_, box) -> box.area }?.first ?: throw IllegalStateException()
    val image = input.map { p -> p.integrate(time) }.render()
    return Pair(time, image)
}

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    val solution = solve(input)
    println(solution.second)
    println(solution.first)
}