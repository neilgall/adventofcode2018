# Day 4
Today we're presented with event data where th events are not necessarily
ordered and the information they contain is partial. This sort of scenario
is pretty common in communications protocols and user interface code. I'm
going to break the problem down as follows:

1. Parse the input data to an event list
2. Sort the event list by time
3. Run the events through an interpreter to assemble all the information
4. Keep asleep counts keyed by minute so the most frequent can be found

It would be possible to merge these things into a single step, but we don't
know what's coming in part 2 and breaking it into indepedent steps keeps
the code clear and flexible.

## Parsing
Of course I'm going to use the One True Parsing method of parser combinators
again. It can seem like overkill for such simple examples but in the real
world it pays off in the long run.

The event model is a [sum type](https://chadaustin.me/2015/07/sum-types/):
```
sealed class Event {
    abstract val time: LocalDateTime
    data class BeginShift(override val time: LocalDateTime, val guard: Int): Event()
    data class FallAsleep(override val time: LocalDateTime): Event()
    data class WakeUp(override val time: LocalDateTime): Event()
}
```

A few parsing helpers:
```
fun <T> Parser<T>.skip(c: Char): Parser<T> = followedBy(isChar(c))
fun <T> Parser<T>.skip(s: String): Parser<T> = followedBy(string(s))
val integer: Parser<Int> = INTEGER.map(String::toInt)
```

Every event has a date-time, fortunately with the fields in exactly the right
order for one of `java.time.LocalDateTime`'s constructor methods:
```
val date: Parser<LocalDateTime> = isChar('[').next(sequence(
    integer.skip('-'),
    integer.skip('-'),
    integer.skip(' '),
    integer.skip(':'),
    integer.skip("] "),
    LocalDateTime::of
))
```

We need a parser for each event type, and a top-level parser that chooses any
of them:
```
val beginShift: Parser<Event> =
    sequence(date.skip("Guard #"), integer.skip(" begins shift"), Event::BeginShift)

val fallAsleep: Parser<Event> =
    date.skip("falls asleep").map(Event::FallAsleep)

val wakeUp: Parser<Event> =
    date.skip("wakes up").map(Event::WakeUp)

val event: Parser<Event> =
    or(beginShift, fallAsleep, wakeUp)
```

You can keep your regular expressions.


## Interpreting Events
The full-on design for this is to build a [Free Monad](https://softwaremill.com/free-monads/)
where the events become actions on an immutable state. Based on my experience
yesterday of trying to use immutable maps in Kotlin I'm going to take a halfway
house approach. I'll fold the event list over a partial state and use side-effects
to mutate the map as we go. This code structure makes everything really explicit and
ensures you've thought about all the possible events in each state.

First a model for a guard with all the captured information:
```
data class Guard(val id: Int, val minutesAsleep: Int, val daysByMinute: Map<Int, Int>)
```

And another sum type for the intermediate states:
```
sealed class State {
    object Initial: State()
    data class OnDuty(val guard: Guard): State()
    data class Asleep(val guard: Guard, val sleepMinute: Int): State()
}
```

When we get a BeginShift event, we enter the OnDuty state, which remembers the
guard object. When he falls asleep we enter the Asleep state capturing the
start minute along with the guard. On a wake event we have all the information
we need to update the Guard and continue.

```
events.sorted().fold(State.Initial as State) { state, event ->
    when(event) {
        is Event.BeginShift -> {
            val guard = guards[event.guard] ?: Guard(event.guard, 0, mapOf())
            guards[guard.id] = guard
            State.OnDuty(guard)
        }
        is Event.FallAsleep -> when(state) {
            is State.OnDuty -> State.Asleep(state.guard, event.time.minute)
            else -> throw IllegalStateException("FallAsleep event in ${state}")
        }
        is Event.WakeUp -> when(state) {
            is State.Asleep -> {
                val guard = state.guard.sleep(state.sleepMinute .. event.time.minute - 1)
                guards[state.guard.id] = guard
                State.OnDuty(guard)
            }
            else -> throw IllegalStateException("WakeUp event in ${state}")
        }
    }
}
```

One thing that caught me out here is a guard can sleep multiple times on his
shift. So the output state of a WakeUp event is OnDuty, not Initial.

## Part 1
After building the Guard model, answering the part 1 question is pretty much a 
direct translation of the Strategy 1 description into code:

```
fun part1(guards: Collection<Guard>): Int {
    val guard = guards.sortedBy { g -> g.minutesAsleep }.last()
    val minute = guard.daysByMinute.entries.maxBy { e -> e.value }.key
    return guard.id * minute
}
```

## Part 2
Find the answer using the second strategy means reorganising our data a bit.
We have sleep events by minute by guard, but need sleep events by guard by minute.
With a small struct to keep things clear that should be easy to transform:

```
data class SleepEvent(val guard: Int, val minute: Int, val count: Int)

val sleepEvents = guards.flatMap { guard ->
    guard.daysByMinute.map { (minute, count) -> SleepEvent(guard.id, minute, count) }
}
```

Sort these by count and the answer is right there at the end of the list.
