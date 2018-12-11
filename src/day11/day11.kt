package adventofcode2018.day11
import java.io.File

sealed class Pos {
    data class Single(val x: Int, val y: Int): Pos()
    data class Nine(val x: Int, val y: Int): Pos() {
        val cells: Sequence<Single> get() =
            (x..x+2).asSequence().flatMap { sx ->
                (y..y+2).asSequence().map { sy -> Single(sx, sy) }
            }
    }
}

fun Pos.Single.rackId(): Int = x + 10
fun Pos.Single.startLevel(): Int = rackId() * y
fun Int.hundreds(): Int = (this / 100) % 10

val nines: Sequence<Pos.Nine> get() =
    (1..298).asSequence().flatMap { y ->
        (1..298).asSequence().map { x -> Pos.Nine(x, y) }
    }  

data class Grid(val serial: Int) {
    operator fun get(pos: Pos): Int = when(pos) {
        is Pos.Single -> ((pos.startLevel() + serial) * pos.rackId()).hundreds() - 5
        is Pos.Nine -> pos.cells.map(this::get).sum()
    }

    fun maxPowerSquare(): Pos.Nine =
        nines.maxBy(this::get) ?: throw IllegalStateException()
}

fun part1(serial: Int) {
    val grid = Grid(serial)
    val max = grid.maxPowerSquare()

    println("Part 1: ${max}")
    println(
        (max.y-1..max.y+3).map { y ->
            (max.x-1..max.x+3).map { x -> grid[Pos.Single(x, y)].toString().padStart(4, ' ') }.joinToString(" ")
        }.joinToString("\n")
    )
}

fun main(vararg args: String) {
    val serial = File(args[0]).readText().trim().toInt()
    part1(serial)
}