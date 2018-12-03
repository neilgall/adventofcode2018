# Day 3
I really enjoyed this type of problem last year. There's an interesting input
format that needs parsing. The input data needs mapped to an appropriate data
model. And what will part 2 bring?

In Haskell I did all the parsing with Parsec, and I thoroughly believe this
is the correct approach to parsing over ad hoc string twiddling, even for simple
cases. I've used [JParsec](https://github.com/jparsec/jparsec) in Kotlin before
so I'll use that this time. I'll need to work out how to include library jars
in a command-line Kotlin program. It's a bit ridiculous in 2018 that this is
still such a difficult part of the JVM ecosystem.

## Parsing

Parsing with JParsec is amazingly easy once you get the hang of it. It's very
declarative and not at all like imperative programming. Read the full docs, but
in short you build parsers for the atoms in your language and construct grammar
by combining the atom parsers into larger constructs. A number of useful
combinators are provided and you can write your own.

With a more complex language you'd build a lexer using JParsec which turned the
input characters into a sequence of tokens, ignoring whitespace and comments,
etc., then build a second layer parser defining a full grammar of tokens. For
the claim syntax in this puzzle I just built a simple parser for each line.t s
Parsing the input data takes a few data definitions and six lines of declarative
code:
```
val integer: Parser<Int> = INTEGER.map(String::toInt)
```
`INTEGER` is a provided scanner that parses the *characters* that form an integer
value. Its type is `Parser<String>` as it is just a character sequence parser,
so we turn it into a `Parser<Int>` by mapping `String::toInt` over it.

```
fun <T> integerPair(sep: Char, c: (Int, Int) -> T): Parser<T> =  
    sequence(integer.followedBy(isChar(sep)), integer, c)
```
A helper to parse pairs of integers separated by `sep`. Once parsed they're passed
to the `c` constructor function to yield a result. This gives us a `Parser<T>` 
where T is the return type of `c`. `sequence()` and `followedBy()` are provided
combinators.

```
val claimId: Parser<Int> = isChar('#').next(integer)
```
The claim ID is a # followed by an integer.

```
val origin: Parser<Origin> = WHITESPACES.followedBy(isChar('@')).followedBy(WHITESPACES).next(integerPair(',', ::Origin))
val size: Parser<Size> = isChar(':').followedBy(WHITESPACES).next(integerPair('x', ::Size))
```
The origin is whitespace, an @, more whitespace and two integers separated by a comma.
We use the `Origin` constructor as the `c` function. Likewise, the size of a claim
is a colon followed by whitespace and an integer pair separated by an x.

```
val claim: Parser<Claim> = sequence(claimId, origin, size, ::Claim)
```
Finally, a whole claim string is a sequence of the above three things, all passed
to the `Claim` constructor. That's it, we can map the `claim.parse()` function
over the set of strings read from the file and get the `List<Claim>` in a one-liner:

```
val input: List<Claim> = File(args[0]).readLines().map(claim::parse)
```

## Part 1
I really wanted to be space-efficient here and do geometric merging and splitting
of claims so that the overlapping areas became separate objects in the model which
could then be filtered out and summed. This does end up being approximately O(N²) 
however and I was limited by time constraints so went for the constant time and
memory-hungry algorithm of just enumerating every coordinate covered by all the
claims and increment a counter for each coordinate. The set of coordinates with
a counter greater than 2 is the solution.

```
val material = mutableMapOf<Pos, Int>()
claims.forEach { claim -> claim.positions().forEach { pos -> material[pos] = (material[pos] ?: 0) + 1 } }
val overlapping = material.filterValues { it >= 2 }.size
```

## Part 2
This turns the algorithm around. Now we need to mark all the claims that overlap
and find the unmarked one at the end. I tried a pure functional approach but 
Kotlin doesn't have efficient pure map and set operations so the runtime was
unreasonable. The solution using mutable collections is still pretty clear though.
Pseudocode:
```
start with a candidate set of all claim IDs
for each position in each claim:
    if the position is unclaimed, claim it with the claim ID
    otherwise remove the claimed ID and the current ID from the candidate set
```
