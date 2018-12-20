package adventofcode2018.day20

import java.io.File

enum class Dir { N, S, E, W }

sealed class Tree {
	data class Move(val dir: Dir): Tree()
	data class Seq(val nodes: List<Tree>): Tree()
	data class Opt(val nodes: List<Tree>): Tree()
}

fun parseTree(regex: String): Tree {
	val chars = regex.toCharArray()

	fun parseNode(start: Int): Pair<Int, Tree> {
		var opts = mutableListOf<Tree>()
		var seq = mutableListOf<Tree>()
		var p = start
		while (p < chars.size) {
			when (chars[p]) {
				'N' -> seq.add(Tree.Move(Dir.N))
				'S' -> seq.add(Tree.Move(Dir.S))
				'E' -> seq.add(Tree.Move(Dir.E))
				'W' -> seq.add(Tree.Move(Dir.W))
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
	is Tree.Move -> false
	is Tree.Seq -> nodes.isEmpty()
	is Tree.Opt -> nodes.any(Tree::hasLoop)
}

fun Tree.longestPath(): Int = if (hasLoop()) 0 else when(this) {
	is Tree.Move -> 1
	is Tree.Seq -> nodes.map(Tree::longestPath).sum()
	is Tree.Opt -> nodes.map(Tree::longestPath).max()!!
}

fun part1(input: Tree): Int = input.longestPath()

fun part1Test(regex: String, expect: Int) {
	val longest = parseTree(regex).longestPath()
	println("$regex: longest = $longest expect = $expect, ${longest == expect}")
}

fun main(vararg args: String) {
	val input = parseTree(File(args[0]).readText().trim())

	part1Test("^WNE$", 3)
	part1Test("^ENWWW(NEEE|SSE(EE|N))$", 10)
	part1Test("^ENNWSWW(NEWS|)SSSEEN(WNSE|)EE(SWEN|)NNN$", 18)
	part1Test("^ESSWWN(E|NNENN(EESS(WNSE|)SSS|WWWSSSSE(SW|NNNE)))$", 23)
	part1Test("^WSSEESWWWNW(S|NENNEEEENN(ESSSSW(NWSW|SSEN)|WSWWN(E|WWS(E|SS))))$", 31)

	println("Part1: ${part1(input)}")
}
