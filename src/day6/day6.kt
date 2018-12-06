package adventofcode2018.day6

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

data class Pos(val x: Int, val y: Int)

fun IntRange.extend(i: Int): IntRange = minOf(start, i) .. maxOf(endInclusive, i)

data class Box(val x: IntRange, val y: IntRange) {
    companion object {
        val EMPTY = Box(IntRange.EMPTY, IntRange.EMPTY)
    }
}

enum class Dir { UP, DOWN, LEFT, RIGHT }

typealias Manhattan = Int

fun Box.contains(p: Pos): Boolean = x.contains(p.x) && y.contains(p.y)

operator fun Box.plus(p: Pos): Box = Box(x.extend(p.x), y.extend(p.y))

operator fun Pos.minus(other: Pos): Manhattan =
    Math.abs(x - other.x) + Math.abs(y - other.y)

operator fun Pos.plus(dir: Dir) = when(dir) {
    Dir.UP -> Pos(x, y-1)
    Dir.DOWN -> Pos(x, y+1)
    Dir.LEFT -> Pos(x-1, y)
    Dir.RIGHT -> Pos(x+1, y)
}

typealias CoordinateID = Int
data class Coordinate(val id: CoordinateID, val pos: Pos)

fun parse(input: String): List<Coordinate> {
    val integer: Parser<Int> = INTEGER.map(String::toInt)
    val pos: Parser<Pos> = sequence(integer.followedBy(string(", ")), integer, ::Pos)
    val positions: List<Pos> = pos.sepBy(WHITESPACES).parse(input)
    val ids: Sequence<Int> = generateSequence(0) { it + 1 }

    return positions.asSequence().zip(ids).map { (pos, id) -> Coordinate(id, pos) }.toList()
}

fun spaceExtent(coords: Collection<Coordinate>): Box =
    coords.map { c -> c.pos }.fold(Box.EMPTY, Box::plus)

sealed class Cell {
    object Unclaimed: Cell()
    data class Coordinate(val id: CoordinateID): Cell()
    data class Claimed(val id: CoordinateID, val distance: Manhattan): Cell()
    data class Equidistant(val distance: Manhattan): Cell()
}

data class Space(val box: Box) {
    val cells: Array<Array<Cell>>

    init {
        val width = box.x.endInclusive - box.x.start
        val height = box.y.endInclusive - box.y.start
        cells = Array<Array<Cell>>(height) { Array<Cell>(width) { Cell.Unclaimed } }
    }

    operator fun get(p: Pos): Cell = cells[p.y - box.y.start][p.x - box.x.start]
    operator fun set(p: Pos, c: Cell) { cells[p.y - box.y.start][p.x - box.x.start] = c }

    override fun toString(): String {
        val cell: (Cell) -> Char = { c -> when(c) {
                is Cell.Unclaimed -> ' '
                is Cell.Coordinate -> 'A' + c.id
                is Cell.Claimed -> 'a' + c.id
                is Cell.Equidistant -> '.'
            }
        }
        val row: (Array<Cell>) -> String = { cs -> cs.map(cell).joinToString("") }
        return cells.map(row).joinToString("\n")
    }
}

fun Space.fill(c: Coordinate) {

    fun fill(p: Pos) {
        if (!box.contains(p)) return

        val distance: Manhattan = p - c.pos
        val cell = this[p]
        val newCell = when (cell) {
            is Cell.Unclaimed -> Cell.Claimed(c.id, distance)
            is Cell.Claimed -> when {
                cell.id == c.id -> cell
                distance < cell.distance -> Cell.Claimed(c.id, distance)
                distance == cell.distance -> Cell.Equidistant(distance)
                else -> cell
            }
            is Cell.Equidistant -> when {
                distance < cell.distance -> Cell.Claimed(c.id, distance)
                else -> cell
            }
            is Cell.Coordinate -> cell
        }
        if (newCell != cell) {
            this[p] = newCell
            fill(p + Dir.UP)
            fill(p + Dir.DOWN)
            fill(p + Dir.RIGHT)
            fill(p + Dir.LEFT)
        }
    }

    fill(c.pos + Dir.UP)
    fill(c.pos + Dir.DOWN)
    fill(c.pos + Dir.RIGHT)
    fill(c.pos + Dir.LEFT)
}

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    val space = Space(spaceExtent(input))

    input.forEach { c -> space[c.pos] = Cell.Coordinate(c.id) }
    input.forEach { c -> space.fill(c) }

    println(space)
}
 