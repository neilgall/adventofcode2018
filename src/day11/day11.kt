package adventofcode2018.day11
import java.io.File

sealed class Pos {
    data class Single(val x: Int, val y: Int): Pos()
    data class Square(val x: Int, val y: Int, val size: Int): Pos() {
        val cells: Sequence<Single> get() =
            (x..x+size-1).asSequence().flatMap { sx ->
                (y..y+size-1).asSequence().map { sy -> Single(sx, sy) }
            }
    }
}

fun Pos.Single.rackId(): Int = x + 10
fun Pos.Single.startLevel(): Int = rackId() * y
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
    operator fun get(pos: Pos): Int = when(pos) {
        is Pos.Single -> ((pos.startLevel() + serial) * pos.rackId()).hundreds() - 5
        is Pos.Square -> pos.cells.asSequence().map(this::get).sum()
    }
}

fun part1(grid: Grid) {
    val max: Pos.Square = nines.maxBy(grid::get) ?: throw IllegalStateException()

    println("Part 1: ${max}")
    println(
        (max.y-1..max.y+3).map { y ->
            (max.x-1..max.x+3).map { x -> grid[Pos.Single(x, y)].toString().padStart(4, ' ') }.joinToString(" ")
        }.joinToString("\n")
    )
}

fun part2(grid: Grid) {
    val max: Pos.Square = squares.maxBy(grid::get) ?: throw IllegalStateException()

    println("Part 2: ${max}")
    println(
        (max.y-1..max.y+max.size+1).map { y ->
            (max.x-1..max.x+max.size+1).map { x -> grid[Pos.Single(x, y)].toString().padStart(4, ' ') }.joinToString(" ")
        }.joinToString("\n")
    )
}

fun main(vararg args: String) {
    val serial = File(args[0]).readText().trim().toInt()
    val grid = Grid(serial)
    part1(grid)
    part2(grid)
}