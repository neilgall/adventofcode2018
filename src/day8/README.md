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
