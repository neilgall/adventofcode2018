package adventofcode2018.day11

import adventofcode2018.toolbox
import java.io.File

val IntRange.steps: Sequence<Int> get() = sequence {
    var i: Int = start
    var end: Int = endInclusive+1
    var bit: Int = 1
    do {
        if (i and bit != 0) {
            yield(bit)
            i += bit
        }
        bit *= 2
    } while (i + bit <= end)
    do {
        bit /= 2
        if (i + bit <= end) {
            yield(bit)
            i += bit
        }
    } while (bit > 1)
}

sealed class Pos {
    data class Atom(val x: Int, val y: Int): Pos()
    
    data class Square(val x: Int, val y: Int, val size: Int): Pos() {
        val xRange: IntRange = x .. (x+size-1)
        val yRange: IntRange = y .. (y+size-1)

        val cells: Sequence<Pos> get() = when(size) {
            1 -> sequenceOf(Atom(x, y))
            2 -> sequenceOf(Atom(x, y), Atom(x+1, y), Atom(x, y+1), Atom(x+1, y+1))
            else -> 
                val qtr = size / 2
                yield(Square(x,     y,     qtr))
                yield(Square(x+qtr, y,     qtr))
                yield(Square(x,     y+qtr, qtr))
                yield(Square(x+qtr, y+qtr, qtr))
                if (size and 1 != 0) {
                    yieldAll((x..x+size-1).asSequence().map { i -> Pos.Atom(i, y+size-1) })
                    yieldAll((y..y+size-1).asSequence().map { i -> Pos.Atom(x+size-1, i) })
                }
            }
        }
    }
}

fun Pos.Atom.rackId(): Int = x + 10
fun Pos.Atom.startLevel(): Int = rackId() * y
fun Int.hundreds(): Int = (this / 100) % 10

val nines: Sequence<Pos.Square> get() =
    (1..298).asSequence().flatMap { y ->
        (1..298).asSequence().map { x -> Pos.Square(x, y, 3) }
    }  

val squares: Sequence<Pos.Square> get() = 
    (1..300).asSequence().flatMap { size -> 
        val range = 1..(301-size)
        println(size)
        range.asSequence().flatMap { y ->
            range.asSequence().map { x -> Pos.Square(x, y, size) }
        }
    }

data class Grid(val serial: Int) {
    val cache = mutableMapOf<Pos, Int>()

    operator fun get(pos: Pos): Int = when(pos) {
        is Pos.Atom -> ((pos.startLevel() + serial) * pos.rackId()).hundreds() - 5
        is Pos.Square -> cache.getOrPut(pos) { pos.cells.asSequence().map(this::get).sum() }
    }
}

fun part1(grid: Grid) {
    val max: Pos.Square = nines.maxBy(grid::get) ?: throw IllegalStateException()

    println("Part 1: ${max}")
    println(
        (max.y-1..max.y+3).map { y ->
            (max.x-1..max.x+3).map { x -> grid[Pos.Atom(x, y)].toString().padStart(4, ' ') }.joinToString(" ")
        }.joinToString("\n")
    )
}

fun part2(grid: Grid) {
    // squares.forEach { sq -> println("${sq} ${sq.cells.toList()}") }

    val max: Pos.Square = squares.maxBy(grid::get) ?: throw IllegalStateException()

    println("Part 2: ${max}")
    println(
        (max.y-1..max.y+max.size+1).map { y ->
            (max.x-1..max.x+max.size+1).map { x -> grid[Pos.Atom(x, y)].toString().padStart(4, ' ') }.joinToString(" ")
        }.joinToString("\n")
    )
}

fun main(vararg args: String) {
    // val serial = File(args[0]).readText().trim().toInt()
    // val grid = Grid(serial)
    // part1(grid)
    // part2(grid)

    (2..100).forEach { size -> 
        (1..(101-size)).forEach { i ->
            val r = IntRange(i, i+size-1)
            println("$r ${r.steps.toList()}")
        }
    }
}