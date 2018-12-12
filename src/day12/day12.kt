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

data class State(val plants: BooleanArray, val origin: Long) {
    constructor(plants: Collection<Boolean>, origin: Long):
        this(plants.toBooleanArray(), origin)

    val sum: Long get() = plants.mapIndexed { i, b -> if (b) i+origin else 0 }.sum()
    val str: String get() = plants.map { if (it) '#' else '.' }.joinToString("")

    override fun toString(): String = "$origin:${str}:${sum}"
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

    val leadingEmpties = output.takeWhile { !it }.count()
    return State(output.dropWhile { !it }, origin - 2 + leadingEmpties)
}

fun State.run(iterations: Long, rules: Rules): State =
    (1..iterations).fold(this) { state, _ -> state.run(rules) }

fun part1(game: Game): Long = game.initial.run(20, game.rules).sum

fun part2(game: Game): Long {
    data class StateInfo(val index: Int, val origin: Long)
    val states = mutableMapOf<String, StateInfo>()
    var state = game.initial
    var count = 0
    while (!states.containsKey(state.str)) {
        states[state.str] = StateInfo(count, state.origin)
        state = state.run(game.rules)
        count += 1
    }

    val total: Long = 50_000_000_000
    val loopStart = states[state.str]!!
    val loopSize = count - loopStart.index
    val loops = (total - loopStart.index) / loopSize
    val originInc = state.origin - loopStart.origin

    println("$total $loopStart $loopSize $loops $originInc")

    val lastLoopStartState = State(state.plants, loopStart.origin + loops * originInc)
    val lastState = if (loopSize == 1) lastLoopStartState else {
        val lastLoopLength = total % loopSize
        lastLoopStartState.run(lastLoopLength, game.rules)
    }

    return lastState.sum
}

fun main(vararg args: String) {
    val game = parse(File(args[0]).readText())
    println(game.rules)
    println("Part 1: ${part1(game)}")
    println("Part 2: ${part2(game)}")
}