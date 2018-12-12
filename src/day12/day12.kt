package adventofcode2018.day12

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

data class State(val plants: BooleanArray)
data class Rule(val input: BooleanArray, val output: Boolean)
data class Game(val initial: State, val rules: List<Rule>)

fun parse(input: String): Game {
    val plant = or(isChar('.').retn(false), isChar('#').retn(true))
    val plants = plant.many().map { it.toBooleanArray() }

    val initial = string("initial state: ").next(plants).map(::State)
    val rule = sequence(plants, string(" => ").next(plant), ::Rule)

    val parser: Parser<Game> = sequence(
        initial.followedBy(WHITESPACES),
        rule.sepBy(WHITESPACES),
        ::Game
    )
    return parser.parse(input)
}

fun main(vararg args: String) {
    val game = parse(File(args[0]).readText())
    println(game.initial)
    println(game.rules)
}