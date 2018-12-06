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

    val width: Int = x.endInclusive - x.start + 1
    val height: Int = y.endInclusive - y.start + 1
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

fun around(p: Pos): Set<Pos> = enumValues<Dir>().map { dir -> p + dir }.toSet()

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

fun Cell.claimedBy(id: CoordinateID): Boolean = when(this) {
    is Cell.Claimed -> this.id == id
    is Cell.Coordinate -> this.id == id
    else -> false
}

data class Space<T>(val box: Box, val cells: Array<Array<T>>) {
    fun rowIndex(p: Pos): Int = p.y - box.y.start
    fun colIndex(p: Pos): Int = p.x - box.x.start

    fun topEdge(): Array<T> = cells.first()
    fun bottomEdge(): Array<T> = cells.last()
    fun leftEdge(): List<T> = cells.map { row -> row.first() }
    fun rightEdge(): List<T> = cells.map { row -> row.last() }

    operator fun get(p: Pos): T = cells[rowIndex(p)][colIndex(p)]
    operator fun set(p: Pos, c: T) { cells[rowIndex(p)][colIndex(p)] = c }

    fun count(p: (T) -> Boolean): Int = cells.map { row -> row.count(p) }.sum()
}

inline fun <reified T> makeSpace(box: Box, empty: T): Space<T> =
    Space(box, Array<Array<T>>(box.height) { Array<T>(box.width) { empty } })

fun Space<Cell>.fill(c: Coordinate) {
    val stack = mutableSetOf<Pos>()

    fun fill(p: Pos): Set<Pos> {
        if (!box.contains(p)) return setOf()

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
        return if (newCell != cell) {
            this[p] = newCell
            around(p)
        } else {
            setOf()
        }
    }

    stack.addAll(around(c.pos))
    while (!stack.isEmpty()) {
        val p = stack.first()
        stack.addAll(fill(p))
        stack.remove(p)
    }
}

sealed class Area {
    object Infinite: Area()
    data class Finite(val size: Int): Area()
}

fun Space<Cell>.areaForCoordinate(id: CoordinateID): Area {
    val claimed: (Cell) -> Boolean = { cell -> cell.claimedBy(id) }

    return if (topEdge().any(claimed) || bottomEdge().any(claimed) || leftEdge().any(claimed) || rightEdge().any(claimed))
        Area.Infinite
    else
        Area.Finite(count(claimed))
}

fun part1(input: List<Coordinate>): Area? {
    val space = makeSpace<Cell>(spaceExtent(input), Cell.Unclaimed)
    input.forEach { c -> space[c.pos] = Cell.Coordinate(c.id) }

    input.forEach { c -> space.fill(c) }

    val areas: List<Area> = input.map { c -> space.areaForCoordinate(c.id) }

    return areas.maxBy { when(it) {
        is Area.Finite -> it.size
        is Area.Infinite -> 0
    }}
}

fun Space<Manhattan>.fillDistances(coords: Collection<Coordinate>) {
    box.y.forEach { y ->
        box.x.forEach { x -> 
            val pos = Pos(x, y)
            this[pos] = coords.map { c -> c.pos - pos }.sum()
        }
    }
}

fun part2(input: List<Coordinate>, max: Int): Int {
    val space = makeSpace<Manhattan>(spaceExtent(input), 0)
    space.fillDistances(input)

    return space.count { d -> d < max }
}

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    val max = if (args.size > 1) args[1].toInt() else 10000

    println("Part 1: ${part1(input)}")
    println("Part 2: ${part2(input, max)}")
}
 