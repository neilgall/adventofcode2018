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

enum class ArmyGroupType { IMMUNE_SYSTEM, INFECTION }

data class ArmyGroup(
	val id: Int,
	val type: ArmyGroupType,
	val count: Int,
	val unit: ArmyUnit
) { override fun toString() = "$type $id: ${count} <hp=${unit.hitPoints} attack=${unit.attack.damage}>" }

// Parser

fun parse(input: String): Set<ArmyGroup> {
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
	).between(token("("), token(")"))

	val weaknessesAndImmunitiesOrEmpty =
		weaknessesAndImmunities.optional(WeaknessesAndImmunities(setOf(), setOf()))

	val attack: Parser<Attack> = sequence(
		integer,
		Terminals.Identifier.PARSER.followedBy(token("damage")),
		::Attack
	)

	val unit: Parser<ArmyUnit> = sequence(
		integer,
		tokens("hit points").next(weaknessesAndImmunitiesOrEmpty),
		tokens("with an attack that does").next(attack),
		tokens("at initiative").next(integer),
		{ hitPoints, wi, attack_, initiative ->
			ArmyUnit(hitPoints, attack_, initiative, wi.weaknesses.toSet(), wi.immunities.toSet())
		}
	)

	fun group(type: ArmyGroupType): Parser<ArmyGroup> = sequence(
		integer,
		tokens("units each with").next(unit), 
		{ count, units -> ArmyGroup(0, type, count, units) }
	)

	fun battleSide(name: String, type: ArmyGroupType): Parser<List<ArmyGroup>> =
		tokens(name)
		.next(token(":"))
		.next(group(type).many1())

	val battle: Parser<Set<ArmyGroup>> = sequence(
		battleSide("immune system", ArmyGroupType.IMMUNE_SYSTEM),
		battleSide("infection", ArmyGroupType.INFECTION),
		{ a, b -> (a + b).mapIndexed { i, g -> g.copy(id = i) }.toSet() }
	)

	return battle.from(tokenizer, tokenDelimiter).parse(input.trim())
}

// Battle

val ArmyGroup.effectivePower: HitPoints get() = count * unit.attack.damage

fun ArmyGroup.damageFrom(attack: Attack): HitPoints = when {
	unit.immunities.contains(attack.type) -> 0
	unit.weaknesses.contains(attack.type) -> attack.damage * 2
	else -> attack.damage
}

val initiativeOrder = Comparator<ArmyGroup> {
	x, y -> y.unit.initiative - x.unit.initiative	
}

val attackingOrder = Comparator<ArmyGroup> {
	x, y -> y.effectivePower - x.effectivePower
}.then(initiativeOrder)

fun defendingOrder(attack: Attack) = Comparator<ArmyGroup> {
	x, y -> x.damageFrom(attack) - y.damageFrom(attack)
}.then(attackingOrder.reversed())

typealias TargetSelection = Map<Int, Int>

fun selectTargets(groups: Set<ArmyGroup>): TargetSelection {
	data class SelectionBuilder(val unselected: Set<ArmyGroup>, val selected: TargetSelection) {
		fun add(attack: ArmyGroup, defend: ArmyGroup) = 
			SelectionBuilder(unselected - defend, selected + Pair(attack.id, defend.id))
	}

	return groups.sortedWith(attackingOrder).fold(SelectionBuilder(groups, mapOf())) { selection, attackingGroup ->
		val opposingGroups = selection.unselected.filter { g -> g.type != attackingGroup.type }
		val defendingGroup = opposingGroups.maxWith(defendingOrder(attackingGroup.unit.attack))
		if (defendingGroup == null) 
			selection
		else 
			selection.add(attackingGroup, defendingGroup)
	}.selected
}

fun Set<ArmyGroup>.find(id: Int): ArmyGroup? = find { g -> g.id == id }

operator fun ArmyGroup.minus(killed: Int): ArmyGroup = copy(count = count - killed)

fun fight(groups: Set<ArmyGroup>): Set<ArmyGroup> {
	val targetSelection = selectTargets(groups)
	val orderedAttackIds = targetSelection.keys
								.mapNotNull(groups::find)
								.sortedWith(initiativeOrder)
								.map { g -> Pair(g.id, targetSelection[g.id]!!) }

	return orderedAttackIds.fold(groups) { groups_, (attackingGroupId, defendingGroupId) ->
		val attackingGroup = groups_.find(attackingGroupId)
		val defendingGroup = groups_.find(defendingGroupId)
		if (attackingGroup == null || defendingGroup == null)
			groups_
		else {
			val otherGroups = groups_.filterNot { g -> g.id == defendingGroupId }.toSet()
			val damage = defendingGroup.damageFrom(attackingGroup.unit.attack) * attackingGroup.count
			val unitsKilled = minOf(damage / defendingGroup.unit.hitPoints, defendingGroup.count)
			if (unitsKilled == defendingGroup.count)
				otherGroups
			else
				otherGroups + (defendingGroup - unitsKilled)
		}
	}
}

fun battle(groups: Set<ArmyGroup>): Sequence<Set<ArmyGroup>> = sequence {
	var remaining = groups
	while (remaining.map { g -> g.type }.toSet().size == 2) {
		remaining = fight(remaining)
		yield(remaining)
	}
}

fun part1(groups: Set<ArmyGroup>): Int =
	battle(groups).last().map { g -> g.count }.sum()

fun main(vararg args: String) {
	val input = parse(File(args[0]).readText())
	println("Part 1: ${part1(input)}")
}
