# Day 20
Oh no, I've spent 19 days _avoiding_ regular expressions!

This isn't what it seems though. A regex is a tree, and the answer (to part 1 at least) can be found by a tree traversal. So we'll do just that by parsing the regex into a tree and running the appropriate traversal. 

There are three things that can exist in the tree:
1. A single move (the direction doesn't matter)
2. A sequence of trees
3. A choice of trees

```
sealed class Tree {
	object Move: Tree()
	data class Seq(val nodes: List<Tree>): Tree()
	data class Opt(val nodes: List<Tree>): Tree()
}
```

To parse the regex we just walk over the characters building a sub-tree each time we hit a parenthesis. Within a sub-tree a vertical bar starts a new sequence and appends it to the set of optionals.
```
fun parseTree(regex: String): Tree {
	val chars = regex.toCharArray()

	fun parseNode(start: Int): Pair<Int, Tree> {
		var opts = mutableListOf<Tree>()
		var seq = mutableListOf<Tree>()
		var p = start
		while (p < chars.size) {
			when (chars[p]) {
				'N', 'S', 'E', 'W' -> seq.add(Tree.Move)
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
```

## Part 1
The traversal we want to do is to find the longest path through the tree, avoiding loops. This can be broken down to a longest path calculation for each of the three cases. Recursive data structures are best served by recursive algorithms.

1. For a single move, the length of the longest path is 1
2. For a choice of trees, the length of the longest path is the length of the longest choice
3. For a sequence of trees, the length is the sum of the lengths of the parts

Loops are a special case. When a tree has a loop leading it back to the start, the longest path we'd take is 0. This all translates into surprisingly simple code:
```
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
```

## Part 2
The wording of the question has me stumped. "How many rooms can be reached" sounds like how many steps along any path are at least 1000 doors away, but I can't find an answer for that which satisfies the puzzle. I've also tried how many terminal rooms (i.e. at the end of any whole path) are at least 1000 doors away but no luck.