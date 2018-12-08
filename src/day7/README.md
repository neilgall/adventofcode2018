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
Ordering. This is really common in scheduling and resource allocation algorithms. Anywhere
where there are a graph of dependencies like this.

One technique well known since 1962 is Kahn's algorithm, which is roughly:
    - start with the nodes with no inputs in a queue
    - while there are nodes to process in the queue
        - take a node from the queue
        - remove edges from the graph leading from this node
        - for every node reachable from this node
            - if there are no more incoming edges, add it to the queue
        - add the node to the output

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

## Part 2

In part 2 we have multiple elves following the steps. This is a natural extension
of many scheduling algorithms where multiple jobs can be run at once. Think of
your favourite build system that runs jobs in parallel. I leaned on a well-known
algorithm for part 1 but I thought I'd try and solve this part myself for educational
purposes.

First I want to convert the very imperative, mutation-heavy code from part 1 into a
more functional solution. To solve the parallel solution I want to be able t apply
multiple graph changes at once.

The core of the sort algorithm then becomes a tail-recursive function which
carries out the same steps as above but generating new state each time rather
than mutating the old:

```
tailrec fun sort(graph: List<Instruction>, stack: List<Name>, result: List<Name>): List<Name> =
    if (stack.isEmpty())
        result
    else {
        val step = stack.first()
        val (edges, graph_) = graph.partition(from(step))
        val next = edges.filter { e -> graph_.none(to(e.after)) }.map { e -> e.after }
        val stack_ = (stack.drop(1) + next).sorted()
        sort(graph_, stack_, result + step)            
    }
```

I'm learning new Kotlin collection functions: `partition()` and `none()` proved very useful.

To parallelise the algorithm, singular things (the active step) become sets of things. To
take account of the length of steps, we need to track the remaining time on each step and
keep a step on the stack after each iteration if it has time remaining. Finally if we
sort the stack by active then pending steps, the work continues on the active steps before
the pending ones begin.

I made a couple of helper data strutures to track steps and remaining time and the final
result:

```
typealias Time = Int

data class Work(val name: Name, val remaining: Time): Comparable<Work>

data class Result(val steps: List<Name>, val time: Time)
```

Not all the code is shown. `Work`'s `compareTo` was defined to put in-progress work
before pending work, so sorting the stack didn't have to change.

The parallel algorithm looks very similar:
```
tailrec fun sort(graph: List<Instruction>, stack: List<Work>, result: Result): Result =
    if (stack.isEmpty())
        result
    else {
```
Instead of pulling the top item from the stack, as many steps as workers can be progressed.
Progress is the smallest remaining time of the active steps. We advance all active steps
by this amount and find the ones which have finished.
```
        val active = stack.take(workers)
        val progress = active.minBy { w -> w.remaining }?.remaining ?: throw IllegalStateException()
        val advance = active.map { w -> w.doWork(progress) }
        val done = advance.filter(Work::done)
```
Previously we removed graph edges and found next steps for the single step at the head of 
the queue, but now we only do that for steps which have just completed. Also there may be
multiple steps so we compute the sum of all graph changes and newly ready steps.
```
        val (edges, graph_) = done.fold(Pair(setOf<Instruction>(), graph)) { (e, g), step -> 
            val (e_, g_) = g.partition(from(step.name))
            Pair(e + e_, g_)
        }
        val next = edges.filter { e -> graph_.none(to(e.after)) }.map { e -> Work(e.after) }
```
For the next iteration, the stack is the set of still-active steps plus any newly ready ones,
and we add the done steps onto the final result.
```
        val stack_ = (stack.drop(workers) + advance.filterNot(Work::done) + next).sorted()
        val result_ = result.add(done, progress)

        sort(graph_, stack_, result_)
    }
```
