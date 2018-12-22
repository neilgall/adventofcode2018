package adventofcode2018.day20

import java.io.File

// Model
data class Pos(val x: Int, val y: Int)

sealed class Tree {
	data class Room(val pos: Pos): Tree()
	data class Seq(val steps: List<Tree>): Tree()
	data class Opt(val choices: List<Tree>): Tree()
}

// Parsing

fun parseTree(regex: String): Tree {
	val chars = regex.toCharArray()
	var pos = Pos(0, 0)

	fun room(dir: Char): Tree {
		val t = Tree.Room(pos)
		pos = when (dir) {
			'N' -> Pos(pos.x, pos.y-1)
			'S' -> Pos(pos.x, pos.y+1)
			'E' -> Pos(pos.x+1, pos.y)
			'W' -> Pos(pos.x-1, pos.y)
			else -> throw IllegalStateException()
		}
		return t
	}

	fun parseNode(start: Int): Pair<Int, Tree> {
		var opts = mutableListOf<Tree>()
		var seq = mutableListOf<Tree>()
		var p = start
		while (p < chars.size) {
			when (chars[p]) {
				'N', 'S', 'E', 'W' -> seq.add(room(chars[p]))
				'(' -> {
					val (p_, t) = parseNode(p+1)
					seq.add(t)
					p = p_
				}
				'|' -> {
					opts.add(Tree.Seq(seq))
					seq = mutableListOf<Tree>()
				}
				')', '$' -> {
					val t = if (opts.isEmpty()) Tree.Seq(seq) else Tree.Opt(opts + Tree.Seq(seq))
					return Pair(p, t)
				}
				else -> throw IllegalStateException("unexpected char '${chars[p]}'")
			}
			p += 1
		}
		throw IllegalStateException("EOF")
	}

	if (chars[0] != '^') throw IllegalArgumentException("Missing ^")
	return parseNode(1).second
}

// Part 1

fun Tree.hasLoop(): Boolean = when(this) {
	is Tree.Room -> false
	is Tree.Seq -> steps.isEmpty()
	is Tree.Opt -> choices.any(Tree::hasLoop)
}

fun Tree.longestPath(): Int = if (hasLoop()) 0 else when(this) {
	is Tree.Room -> 1
	is Tree.Seq -> steps.map(Tree::longestPath).sum()
	is Tree.Opt -> choices.map(Tree::longestPath).max()!!
}

fun part1(input: Tree): Int = input.longestPath()

fun part1Test(regex: String, expect: Int) {
	val longest = parseTree(regex).longestPath()
	println("$regex: longest = $longest expect = $expect, ${longest == expect}")
}

// Part 2

fun Collection<Int>.product(): Int = fold(1, Int::times)

fun Tree.rooms(): Sequence<Pair<Pos, Int>> = sequence {
	val remaining = mutableListOf(Pair<Tree, Int>(this@rooms, 0))
	while (!remaining.isEmpty()) {
		var (node, distance) = remaining.removeAt(0)
		when (node) {
			is Tree.Room -> yield(Pair(node.pos, distance))
			is Tree.Opt -> remaining += node.choices.map { c -> Pair(c, distance) }
			is Tree.Seq -> remaining += node.steps.mapIndexed { i, s -> Pair(s, distance + i) }
		}
	}
}

fun Tree.nodesAtDistance(distance: Int) = rooms().count { (_, d) -> d >= distance }

fun part2(input: Tree): Int = input.nodesAtDistance(1000)

fun part2Test(regex: String, distance: Int, expect: Int) {
	val count = parseTree(regex).nodesAtDistance(distance)
	println("$regex distance $distance expect $expect result $count, ${count == expect}")
}

fun main(vararg args: String) {
	val input = parseTree(File(args[0]).readText().trim())

	part1Test("^WNE$", 3)
	part1Test("^ENWWW(NEEE|SSE(EE|N))$", 10)
	part1Test("^ENNWSWW(NEWS|)SSSEEN(WNSE|)EE(SWEN|)NNN$", 18)
	part1Test("^ESSWWN(E|NNENN(EESS(WNSE|)SSS|WWWSSSSE(SW|NNNE)))$", 23)
	part1Test("^WSSEESWWWNW(S|NENNEEEENN(ESSSSW(NWSW|SSEN)|WSWWN(E|WWS(E|SS))))$", 31)

	part2Test("^WNES$", 2, 1)
	part2Test("^WNE(N|S)$", 2, 2)
	part2Test("^ENWWW(NEEE|SSE(EE|N))$", 3, 3)

	println("Part1: ${part1(input)}")
	println("Part2: ${part2(input)}")
}
