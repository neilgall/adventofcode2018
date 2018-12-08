# Day 8
I've been thinking Advent of Code is quite hard so far this year but this one is
pretty easy. I toyed with the idea of using parser combinators again and doing
the whole tree building in the parser but decided to keep it simple for now. I
might have a stab at that later.

## Parsing
Okay I admit in this case parsing a list of integers is simple enough without using
a parser combinator library:
```
fun parse(input: String): List<Int> {
    return input.trim().split(" ").map(String::toInt)
}
```

## Part 1
It's a simple matter of eating the integers, building the data structure as you
go in a recursive manner. Interesting this is effectively a parser combinator
itself as it returns the parsed item (a node) and the remainder of the input.
```
fun tree(input: List<Int>): Pair<Node, List<Int>> {
    val numChildren = input[0]
    val numMetadata = input[1]

    val (children, remainder) = (1..numChildren).fold(Pair(listOf<Node>(), input.drop(2))) { (cs, input), _ -> 
        val (child, input_) = tree(input)
        Pair(cs + child, input_)
    }

    val metadata = remainder.take(numMetadata)
    return Pair(Node(children, metadata), remainder.drop(numMetadata))
}
```

Summing the metadata is another simple tree traversal:
```
fun Node.metadataTotal(): Int =
    metadata.sum() + children.fold(0) { n: Int, c: Node -> n + c.metadataTotal() }
```

## Part 2
Straightforward again, just translate the wordy description into another tree traversal:
```
fun Node.metadataComplex(): Int =
    if (children.isEmpty())
        metadata.sum()
    else
        metadata.fold(0) { total, i ->
            total + (if (children.indices.contains(i-1)) children[i-1].metadataComplex() else 0)
        }
```

## For fun

Day 8 was so straightforward, and I didn't get to use parser combinators so I came back
later and rewrote it so the entire tree pops out of the parser.
```
fun parse(input: String): Node {
    val integer = Terminals.IntegerLiteral.PARSER.map(String::toInt)

    val treeRef = Parser.Reference<Node>()

    fun nodeParser(numChildren: Int, numMetadata: Int): Parser<Node> =
        sequence(
            treeRef.lazy().times(numChildren),
            integer.times(numMetadata),
            ::Node
        )

    val nodeInfo: Parser<Pair<Int, Int>> = sequence(integer, integer) { nc, nm -> Pair(nc, nm) }
    val tree: Parser<Node> = nodeInfo.next { (nc, nm) -> nodeParser(nc, nm) }
    treeRef.set(tree)

    val parser = tree.from(Terminals.IntegerLiteral.TOKENIZER, Scanners.WHITESPACES)
    return parser.parse(input.trim())
}
```

This is my most advanced use of JParsec in this year's Advent of Code. It separates
out lexical parsing and tokenisation:
```
val parser = tree.from(Terminals.IntegerLiteral.TOKENIZER, Scanners.WHITESPACES)
```
`from()` takes a tokenizing parser (in this case just integers) and a non-token parser
(in this case just whitespace but you could allow comments, for example).

It also makes use of a left-recursive parser for the nodes. In JParsec you need
to make an indirect `Parser.Reference<T>` to deal with recursion, then refer to its
`lazy()` reference for the internal reference, but it's not that big a deal. First
we get the information for a node as a pair of integers:
```
val nodeInfo: Parser<Pair<Int, Int>> = sequence(integer, integer) { nc, nm -> Pair(nc, nm) }
```
Then JParsec hides a little bit of magic with an overloaded `next()`, which I don't
really like. It's really monadic bind, or `flatMap()` in most Kotlin types.
```
val tree: Parser<Node> = nodeInfo.next { (nc, nm) -> nodeParser(nc, nm) }
```
Given the two integers describing a node, `nodeParser()` returns the Parser to use
to parse that node. Yes, we dynamically make a new parser on the fly to continue
processing the input. Its definition is pretty simple:
```
sequence(
    treeRef.lazy().times(numChildren),
    integer.times(numMetadata),
    ::Node
)
```
It's the appropriate number of children, the appropriate number of metadata integers,
and we throw them all at the `Node` constructor. Job done.
