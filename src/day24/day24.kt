package adventofcode2018.day24

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners
import org.jparsec.Terminals
import org.jparsec.Token

// Model

typealias AttackType = String
typealias HitPoints = Int
typealias Initiative = Int

data class Attack(
	val damage: HitPoints,
	val type: AttackType
)

data class ArmyUnit(
	val hitPoints: HitPoints,
	val attack: Attack,
	val initiative: Initiative,
	val weaknesses: Set<AttackType>,
	val immunities: Set<AttackType>
)

data class ArmyGroup(
	val count: Int,
	val unit: ArmyUnit
)

data class Battle(
	val immuneSystem: Set<ArmyGroup>,
	val infection: Set<ArmyGroup>
)

// Parser

fun parse(input: String): Battle {
	// lexer
	val keywords = listOf(
		"an", "at", "attack",
		"damage", "does",
		"each",
		"hit",
		"immune", "infection", "initiative",
		"points",
		"system",
		"that", "to",
		"units",
		"weak", "with"
	)

	val terminals = Terminals
		.operators("(", ")", ";", ",", ":")
		.words(Scanners.IDENTIFIER)
		.caseInsensitiveKeywords(keywords)
		.build()

	val tokenizer = or(Terminals.IntegerLiteral.TOKENIZER, terminals.tokenizer())
	val tokenDelimiter = Scanners.WHITESPACES.skipMany()

	fun token(s: String): Parser<Token> = terminals.token(s)
	fun tokens(s: String) = sequence(s.split(" ").map(::token))

	// parser
	val integer = Terminals.IntegerLiteral.PARSER.map(String::toInt)

	data class WeaknessesAndImmunities(val weaknesses: Collection<AttackType>, val immunities: Collection<AttackType>)

	val commaList = Terminals.Identifier.PARSER.sepBy1(token(","))
	val weaknesses = tokens("weak to").next(commaList)
	val immunities = tokens("immune to").next(commaList)

	val weaknessesAndImmunities: Parser<WeaknessesAndImmunities> = or(
		sequence(weaknesses, token(";").next(immunities), { w, i -> WeaknessesAndImmunities(w, i) }),
		sequence(immunities, token(";").next(weaknesses), { i, w -> WeaknessesAndImmunities(w, i) }),
		weaknesses.map { w -> WeaknessesAndImmunities(w, setOf()) },
		immunities.map { i -> WeaknessesAndImmunities(setOf(), i) }
	)

	val attack: Parser<Attack> = sequence(
		integer,
		Terminals.Identifier.PARSER.followedBy(token("damage")),
		::Attack
	)

	val unit: Parser<ArmyUnit> = sequence(
		integer,
		tokens("hit points (").next(weaknessesAndImmunities),
		tokens(") with an attack that does").next(attack),
		tokens("at initiative").next(integer),
		{ hitPoints, wi, attack, initiative ->
			ArmyUnit(hitPoints, attack, initiative, wi.weaknesses.toSet(), wi.immunities.toSet())
		}
	)


	val group: Parser<ArmyGroup> = sequence(
		integer,
		tokens("units each with").next(unit), 
		::ArmyGroup
	)

	fun battleSide(name: String): Parser<List<ArmyGroup>> =
		tokens(name)
		.next(token(":"))
		.next(group.many1())

	val battle: Parser<Battle> = sequence(
		battleSide("immune system"),
		battleSide("infection"),
		{ a, b -> Battle(a.toSet(), b.toSet()) }
	)

	return battle.from(tokenizer, tokenDelimiter).parse(input.trim())
}

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println(input)
}
