# Day 16
Machine simulations have come up before in Advent of Code and are fun to implement. This one is an interesting twist. As I read the description I thought we were going to have to decipher the opcodes, but fortunately part 1 is a bit simpler than that.

## Parsing
Complex text structure again. You know what that means! A data model and a JParsec parser.

```
data class Registers(val regs: IntArray)

data class Arguments(val a: Int, val b: Int, val c: Int)

data class Instruction(val opcode: Int, val arguments: Arguments)

data class Sample(val before: Registers, val instruction: Instruction, val after: Registers)

data class Input(val samples: List<Sample>, val program: List<Instruction>)

fun parse(input: String): Input {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
 	val registers = integer.sepBy(string(", ")).between(string("["), string("]")).map(::Registers)
 	val before = string("Before: ").next(registers)
 	val after = string("After:  ").next(registers)
 	val arguments = sequence(integer, WHITESPACES.next(integer), WHITESPACES.next(integer), ::Arguments)
 	val instruction = sequence(integer, WHITESPACES.next(arguments), ::Instruction)
 	val sample = sequence(before, WHITESPACES.next(instruction), WHITESPACES.next(after), ::Sample)
 	val samples = sample.sepBy(WHITESPACES)
 	val program = instruction.sepBy(WHITESPACES)

 	return sequence(samples.followedBy(WHITESPACES), program, ::Input).parse(input.trim())
}
```

## Machine simulation
At first I wrote all the instructions out as Kotlin functions but then decided it might be better to encode them as lambdas along with additional information such as the name. I wrote them as destructive in-place operations on a `Registers` since I have the feeling we're going to have to run the program at some point!

```
data class Operation(val name: String, val run: (Registers, Arguments) -> Unit)

val operations: List<Operation> = listOf(
	Operation("addr", { r, (a, b, c) -> r[c] = r[a] + r[b] }),
	Operation("addi", { r, (a, b, c) -> r[c] = r[a] +   b  }),

	Operation("mulr", { r, (a, b, c) -> r[c] = r[a] * r[b] }),
	Operation("muli", { r, (a, b, c) -> r[c] = r[a] *   b  }),

	Operation("banr", { r, (a, b, c) -> r[c] = r[a] and r[b] }),
	Operation("bani", { r, (a, b, c) -> r[c] = r[a] and   b  }),

	Operation("borr", { r, (a, b, c) -> r[c] = r[a] or r[b] }),
	Operation("bori", { r, (a, b, c) -> r[c] = r[a] or   b  }),

	Operation("setr", { r, (a, _, c) -> r[c] = r[a] }),
	Operation("seti", { r, (a, _, c) -> r[c] =   a  }),

	Operation("gtri", { r, (a, b, c) -> r[c] = if (r[a] >   b ) 1 else 0 }),
	Operation("gtir", { r, (a, b, c) -> r[c] = if (  a  > r[b]) 1 else 0 }),
	Operation("gtrr", { r, (a, b, c) -> r[c] = if (r[a] > r[b]) 1 else 0 }),

	Operation("eqri", { r, (a, b, c) -> r[c] = if (r[a] ==   b ) 1 else 0 }),
	Operation("eqir", { r, (a, b, c) -> r[c] = if (  a  == r[b]) 1 else 0 }),
	Operation("eqrr", { r, (a, b, c) -> r[c] = if (r[a] == r[b]) 1 else 0 })
)
```

## Part 1
Since we need to run different instructions over the same input data we really need non-destructive versions of these operations. That's possible with a simple wrapper.

```
class PureOperation(val name: String, val run: (Registers, Arguments) -> Registers) {
	constructor(op: Operation): this(op.name, { r, a -> 
		val r_ = r.copy()
		op.run(r_, a)
		r_
	})

	override fun toString(): String = name
}

val pureOperations = operations.map(::PureOperation)
```

Checking the number of operations which match a sample is easy;

```
fun Sample.test(): Int =
	pureOperations.count { op -> op.run(before, instruction.arguments) == after }
```

And checking the number of samples which pass this test is similar:

```
fun part1(input: Input): Int =
	input.samples.map(Sample::test).count { it >= 3 }
```
