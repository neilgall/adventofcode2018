package adventofcode2018.day4

import java.io.File
import java.time.LocalDateTime
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

// Event model and parsing

sealed class Event: Comparable<Event> {
    abstract val time: LocalDateTime
    override operator fun compareTo(other: Event): Int = time.compareTo(other.time)

    data class BeginShift(override val time: LocalDateTime, val guard: Int): Event()
    data class FallAsleep(override val time: LocalDateTime): Event()
    data class WakeUp(override val time: LocalDateTime): Event()
}

fun parseEvents(path: String): List<Event> {
    fun <T> Parser<T>.skip(c: Char): Parser<T> = followedBy(isChar(c))
    fun <T> Parser<T>.skip(s: String): Parser<T> = followedBy(string(s))

    val integer: Parser<Int> = INTEGER.map(String::toInt)
    
    val date: Parser<LocalDateTime> = isChar('[').next(sequence(
        integer.skip('-'),
        integer.skip('-'),
        integer.skip(' '),
        integer.skip(':'),
        integer.skip("] "),
        LocalDateTime::of
    ))
    val beginShift: Parser<Event> =
        sequence(date.skip("Guard #"), integer.skip(" begins shift"), Event::BeginShift)
    
    val fallAsleep: Parser<Event> =
        date.skip("falls asleep").map(Event::FallAsleep)
    
    val wakeUp: Parser<Event> =
        date.skip("wakes up").map(Event::WakeUp)
    
    val event: Parser<Event> =
        or(beginShift, fallAsleep, wakeUp)

    return File(path).readLines().map(event::parse)
}

// Data model

data class Guard(val id: Int, val minutesAsleep: Int, val daysByMinute: Map<Int, Int>) {

    fun sleep(minutes: IntRange): Guard {
        val days = daysByMinute.toMutableMap()
        minutes.forEach { m -> days[m] = (days[m] ?: 0) + 1 }
        return Guard(id, minutesAsleep + days.size, days)
    }
}

// Event interpreter

sealed class State {
    object Initial: State()
    data class OnDuty(val guard: Guard): State()
    data class Asleep(val guard: Guard, val sleepMinute: Int): State()
}

fun runEvents(events: List<Event>): List<Guard> {
    val guards = mutableMapOf<Int, Guard>()

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
                    guards[state.guard.id] = state.guard.sleep(state.sleepMinute .. event.time.minute - 1)
                    State.OnDuty(state.guard)
                }
                else -> throw IllegalStateException("WakeUp event in ${state}")
            }
        }
    }
    return guards.values.toList()
}

fun main(args: Array<String>) {
    val events = parseEvents(args[0])
    println(runEvents(events))
}