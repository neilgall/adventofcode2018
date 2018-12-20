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
	var pos: Int = 0

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
			}
			p += 1
		}
		throw IllegalStateException("EOF")
	}

	if (chars[0] != '^') throw IllegalArgumentException("Missing ^")
	return parseNode(1).second
}

fun main(vararg args: String) {
	val input = parseTree(File(args[0]).readText().trim())
	println(input)
}
