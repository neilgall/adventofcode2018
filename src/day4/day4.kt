package adventofcode2018.day4

import java.io.File
import java.time.LocalDateTime
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

sealed class Event {
    data class BeginShift(val time: LocalDateTime, val guard: Int): Event()
    data class FallAsleep(val time: LocalDateTime): Event()
    data class WakeUp(val time: LocalDateTime): Event()
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

fun main(args: Array<String>) {
    val events = parseEvents(args[0])
    println(events)
}
