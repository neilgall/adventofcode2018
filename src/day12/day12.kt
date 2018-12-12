package adventofcode2018.day12

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Model

data class Region(val value: Int) {
    constructor(states: List<Boolean>):
        this(states.fold(0) { v, b -> (v shl 1) or (if (b) 1 else 0) })
    override fun toString(): String =
        listOf(16,8,4,2,1).map { b -> if (value and b != 0) '#' else '.'}.joinToString("")
}

data class State(val plants: BooleanArray, val origin: Int) {
    constructor(plants: Collection<Boolean>, origin: Int):
        this(plants.toBooleanArray(), origin)

    fun sum(): Int = plants.mapIndexed { i, b -> if (b) i+origin else 0 }.sum()

    override fun toString(): String =
        "$origin:${plants.map { if (it) '#' else '.' }.joinToString("")}:${sum()}"
}

typealias Rules = Map<Region, Boolean>
data class Game(val initial: State, val rules: Rules)

// Parsing

fun parse(input: String): Game {
    val plant = or(isChar('.').retn(false), isChar('#').retn(true))

    val initial = string("initial state: ")
        .next(plant.many())
        .map { ps -> State(ps, 0) }

    val rule = sequence(
        plant.times(5).map(::Region),
        string(" => ").next(plant),
        { r, q -> r to q }
    )

    val parser: Parser<Game> = sequence(
        initial.followedBy(WHITESPACES),
        rule.sepBy(WHITESPACES).map { rs -> rs.toMap() },
        ::Game
    )
    return parser.parse(input)
}

// Game

val buffer: BooleanArray = BooleanArray(4) { false }

fun State.run(rules: Rules): State {
    val buffered: BooleanArray = buffer + plants + buffer

    val regions: Sequence<Region> = (0..(buffered.size-5))
        .asSequence()
        .map { i -> Region(buffered.slice(i..i+4)) }

    val output = regions
        .map { r -> rules.getOrDefault(r, false) }
        .toList()
        .dropLastWhile { !it }

    return when {
        output[0] -> State(output, origin-2)
        output[1] -> State(output.drop(1), origin-1)
        else ->      State(output.drop(2), origin)
    }
}

fun Game.run(iterations: Long): Int =
    (1..iterations).fold(initial) { state, _ -> state.run(rules) }.sum()

fun part1(game: Game): Int = game.run(20)

fun main(vararg args: String) {
    val game = parse(File(args[0]).readText())
    println(game.rules)
    println("Part 1: ${part1(game)}")
}