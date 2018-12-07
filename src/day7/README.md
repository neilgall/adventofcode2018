# Day 7
Seems straightforward. The question says there's a definite order so the instruction
steps must form a [directed acyclic graph](https://cran.r-project.org/web/packages/ggdag/vignettes/intro-to-dags.html).

First we parse the instructions. I wonder what technology would be appropriate...

```
fun parse(input: String): List<Instruction> {
    val instr = sequence(
        string("Step ").next(IDENTIFIER),
        string(" must be finished before step ").next(IDENTIFIER).followedBy(string(" can begin.")),
        ::Instruction
    )
    return instr.sepBy(WHITESPACES).parse(input)
}
```

## Part 1

Getting a linear ordering of vertices in a directed acyclic graph is called Topological
Ordering. One way to do this is Kahn's algorithm which is roughly:
    - start with the initial node (the one with no inputs) in a queue
    - while there are nodes to process in the queue
        - add the node to the output
        - remove edges from the graph leading from this node
        - for every node reachable from this node
            - if there are no more incoming edges, add it to the queue

The one variation in this problem is that when there are multiple available
paths, they should be taken in alphabetical order. Since the queue represents
the set of available paths, this amounts to sorting the queue each time a new
node is added. Here's my Kotlin version:

```
fun topologicalOrder(input: List<Instruction>): Sequence<Name> {
    fun from(n: Name): (Instruction) -> Boolean = { i -> i.before == n }
    fun to(n: Name): (Instruction) -> Boolean = { i -> i.after == n }

    var graph = input.toMutableList()
    val stack = initials(input).toMutableList()
    return generateSequence {
        when {
            stack.isEmpty() -> null
            else -> {
                val step = stack.removeAt(0)
                graph.filter(from(step)).forEach { edge ->
                    graph.remove(edge)
                    if (graph.none(to(edge.after))) {
                        stack.add(edge.after)
                    }
                }
                stack.sort()
                step
            }
        }
    }
}
```

- I deliberately practiced generating sequences as I've not done that much in Kotlin.
- `from` and `to` are simple helpers to avoid lambdas in the code that's already pretty dense.
- The data set isn't that big so I just sort the queue on each step.
