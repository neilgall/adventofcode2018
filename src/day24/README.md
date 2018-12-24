# Day 24
Just reading the description I feel fully vindicated in my advocacy of parser combinators all month. Just parsing this one is going to be fun. I've been tempted a couple of times to build a full-blown lexer/parser combination but have got by so far with simple character parsers. Today let's do the full thing.

The difference is that the lexer stage differentiates interesting parts of the input from delimiters and unwanted information. In a real language you often filter out comments here. The result is a sequence of _tokens_ rather than characters, and you write the parser in terms of a grammar of tokens.

First a simple model. We'll start with some type aliases. I don't like to see native or primitive types in real data models. Then the structure is much as the input format.
```kotlin
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
```

## Lexer
The lexer matches all interesting words and symbols in the input and ignores the rest. This is really useful as there are irregular line splits and other whitespace differences in the text. The lexer gets rid of all that.
```kotlin
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
```

I like to add a helper function for matching tokens in the parser stage. And since we have runs of tokens in the input I'm going to make an extra one which breaks a string into tokens and matches them in sequence. This allows me to write a parser like `tokens("these words with any whitespace or line breaks")` in the parser, and it'll do just what that says.
```kotlin
fun token(s: String): Parser<Token> = terminals.token(s)
fun tokens(s: String) = sequence(s.split(" ").map(::token))
```

## Parser
The parser is slightly different from before. We're no longer consuming characters but tokens produced by the lexer. e.g. the word `damage` is a single token now, not six separate characters to be matched in sequence.

`Terminals.IntegerLiteral.TOKENIZER` is one kind of token. Its corresponding parser is:
```kotlin
val integer = Terminals.IntegerLiteral.PARSER.map(String::toInt)
```

Let's start in the middle of the parser, at army units. The text says things like "17 units each with", and I'm going to define the rest after that as the unit description. So it's just the sequence of things in the descripton. The major complication is the weaknessess and immunities group in parenthesis, so I factored that out.
```kotlin
	val unit: Parser<ArmyUnit> = sequence(
		integer,
		tokens("hit points").next(weaknessesAndImmunitiesOrEmpty),
		tokens("with an attack that does").next(attack),
		tokens("at initiative").next(integer),
		{ hitPoints, wi, attack, initiative ->
			ArmyUnit(hitPoints, attack, initiative, wi.weaknesses.toSet(), wi.immunities.toSet())
		}
	)
```

Looking at the input text, the weaknesses and immunities can be in either order, or one can be missing, or the whole group might be ommitted. Good luck dealing with that with your hand-rolled parsers, but it's easy stuff for combinators. There are a few ways you could do it - the "weak to" and "immune to" tokens could qualify the following lists, then these would be sorted into their appropriate places when building the overall structure. I chose to keep the data structure simple and handle the various cases at the parser level, since there are only a few:
```kotlin
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
```
There's a lot going on here. `Terminals.Identifier` is one of our token types - its a word not predefined by the grammar, such as a variable name in a programming language. A `commaList` is one or more of these words separated by comma tokens. Remember the whitespace is dealt with in the lexer.

Weaknesses and immunities are just the identifying prefixes followed by comma lists.

The overall structure has four options:
1. Weaknesses followed by a semicolon then immunites
2. Immunities followed by a semicolon then weaknesses
3. Just weaknesses
4. Just immunities

In each case we build the data structure out of the parts available, using an empty set for anything missing. And finally at the very end the parser is augmented with `between(token("("), token(")"))` which matches (and skips) the parenthesis surrounding the group.

What if the whole section is omitted however?
```kotlin
val weaknessesAndImmunitiesOrEmpty =
	weaknessesAndImmunities.optional(WeaknessesAndImmunities(setOf(), setOf()))
}
```
`.optional()` turns a parser into one which matches its grammar, or nothing at all. If nothing is matched the provided default is the result of the parse.

Building up the rest of the data structure is relatively easy, just more combinations of parsers all the way up to the full grammar:
```kotlin
val attack: Parser<Attack> = sequence(
	integer,
	Terminals.Identifier.PARSER.followedBy(token("damage")),
	::Attack
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
```

## Part 1
It's another iterative simulation with a bunch of details to implement. I chose to do it completely functionally with immutable data, with the overall battle state updated on each step and threaded through the entire calculation. As before I sumulated the whole battle as a sequence of states, and the part 1 result can be obtained from the last in this sequence:
```kotlin
fun battle(groups: Set<ArmyGroup>): Sequence<Set<ArmyGroup>> = sequence {
	var remaining = groups
	while (remaining.map { g -> g.type }.toSet().size == 2) {
		remaining = fight(remaining)
		yield(remaining)
	}
}

fun part1(groups: Set<ArmyGroup>): Int =
	battle(groups).last().map { g -> g.count }.sum()
```

The `fight()` function must therefore compute the next state for the battle. First it does target selection, the result of which is a map of attacking groups IDs to defending groups IDs. I used IDs as the data objects are immutable and will be getting replaced with modified versions as the battle proceeds.

Target selection needs a set of unselected groups and the set of selections to be threaded through the computation so I wrapped both in a container data structure and gave it a helper function for adding new selections:
```kotlin
typealias TargetSelection = Map<Int, Int>

data class SelectionBuilder(val unselected: Set<ArmyGroup>, val selected: TargetSelection) {
	fun add(attack: ArmyGroup, defend: ArmyGroup) = 
		SelectionBuilder(unselected - defend, selected + Pair(attack.id, defend.id))
}
```
Selecting targets and fighting involves a few sorts and orderings so I used Kotlin Comparators again:
```kotlin
val initiativeOrder = Comparator<ArmyGroup> {
	x, y -> y.unit.initiative - x.unit.initiative	
}

val attackingOrder = Comparator<ArmyGroup> {
	x, y -> y.effectivePower - x.effectivePower
}.then(initiativeOrder)

fun defendingOrder(attack: Attack) = Comparator<ArmyGroup> {
	x, y -> x.damageFrom(attack) - y.damageFrom(attack)
}.then(attackingOrder.reversed())
```
These depends on a couple of simple helpers which capture the business logic quite neatly:
```kotlin
val ArmyGroup.effectivePower: HitPoints get() = count * unit.attack.damage

fun ArmyGroup.damageFrom(attack: Attack): HitPoints = when {
	unit.immunities.contains(attack.type) -> 0
	unit.weaknesses.contains(attack.type) -> attack.damage * 2
	else -> attack.damage
}
```
Building the target selection now becomes:
1. Sort the groups in attack order
2. For each attacking group select the maximum opposing group in defending order
3. Add the pairing to the selection, removing the defending group from future candidates
```kotlin
fun selectTargets(groups: Set<ArmyGroup>): TargetSelection {
	return groups.sortedWith(attackingOrder).fold(SelectionBuilder(groups, mapOf())) { selection, attackingGroup ->
		val opposingGroups = selection.unselected.filter { g -> g.type != attackingGroup.type }
		val defendingGroup = opposingGroups.maxWith(defendingOrder(attackingGroup.unit.attack))
		if (defendingGroup == null) 
			selection
		else 
			selection.add(attackingGroup, defendingGroup)
	}.selected
}
```

Once we've done the target selection we run the fight. We take the target selection, order the attackers by initiative and put them back into a (now ordered) list of attacker, defender pairs:
```kotlin
val orderedAttackIds = targetSelection.keys
							.mapNotNull(groups::find)
							.sortedWith(initiativeOrder)
							.map { g -> Pair(g.id, targetSelection[g.id]!!) }
```

Running the attacks is pretty simple but we have to be careful to use the _current state_ at each step. `groups` is the set of Army Groups at the beginning of the fight round, `groups_` is the set at the current point in the attack sequence. One nice aspect of this style of design is that by swapping those around it's trivial to change the behaviour from sequential to parallel (i.e. all attacks happen at once), should that requirement change.
```kotlin
orderedAttackIds.fold(groups) { groups_, (attackingGroupId, defendingGroupId) ->
	val attackingGroup = groups_.find(attackingGroupId)
	val defendingGroup = groups_.find(defendingGroupId)
	if (attackingGroup == null || defendingGroup == null) // might be eliminated
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
```
