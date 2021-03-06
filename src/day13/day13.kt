package adventofcode2018.day13

import java.io.File

data class Pos(val x: Int, val y: Int)

enum class Dir { LEFT, RIGHT, UP, DOWN }

enum class Turn { LEFT, STRAIGHT, RIGHT }

data class Plan(val dir: Dir, val turn: Turn)

data class Cart(val id: Int, val pos: Pos, val plan: Plan, val collided: Boolean = false)

typealias Track = Char
typealias Tracks = List<CharArray>

data class Model(val tracks: Tracks, val carts: List<Cart>) {
    override fun toString(): String =
        "${carts}\n${tracks.map{r->r.joinToString("")}.joinToString("\n")}"
}

// Parsing

fun load(lines: List<String>): Model {
    fun dirFor(c: Char): Dir = when(c) {
        '^' -> Dir.UP
        'v' -> Dir.DOWN
        '<' -> Dir.LEFT
        '>' -> Dir.RIGHT
        else -> throw IllegalArgumentException()
    }

    val tracks: Tracks = lines.map(String::toCharArray)

    val carts: List<Cart> = tracks.foldIndexed(listOf<Cart>()) { y, carts, row ->
        row.foldIndexed(carts) { x, carts_, c ->
            if (!("^v<>".contains(c)))
                carts_
            else
                carts_ + Cart(carts_.size, Pos(x, y), Plan(dirFor(c), Turn.LEFT))
        }
    }

    carts.forEach { c ->
        tracks[c.pos.y][c.pos.x] = (if (c.plan.dir == Dir.LEFT || c.plan.dir == Dir.RIGHT) '-' else '|')
    }

    return Model(tracks, carts)
}

// Simulation

fun timeline(initial: Model, tick: (Model) -> Model): Sequence<Model> {
    var model = initial
    return sequence {
        while (true) {
            yield(model)
            model = tick(model)
        }
    }
}

val scanOrder: Comparator<Cart> =
    Comparator<Cart> { 
        c1, c2 -> c1.pos.y - c2.pos.y
    }.then(Comparator<Cart> {
        c1, c2 -> c1.pos.x - c2.pos.x
    })

fun Dir.turn(t: Turn): Dir = when(t) {
    Turn.STRAIGHT -> this
    Turn.LEFT -> when (this) {
        Dir.UP -> Dir.LEFT
        Dir.LEFT -> Dir.DOWN
        Dir.DOWN -> Dir.RIGHT
        Dir.RIGHT -> Dir.UP
    }
    Turn.RIGHT -> when (this) {
        Dir.UP -> Dir.RIGHT
        Dir.RIGHT -> Dir.DOWN
        Dir.DOWN -> Dir.LEFT
        Dir.LEFT -> Dir.UP
    }
}

fun Turn.next(): Turn = when(this) {
    Turn.LEFT -> Turn.STRAIGHT
    Turn.STRAIGHT -> Turn.RIGHT
    Turn.RIGHT -> Turn.LEFT
}

fun Plan.adjust(t: Track): Plan = when(t) {
    '+' -> Plan(dir.turn(turn), turn.next())
    '/' -> when (dir) {
        Dir.UP -> Plan(Dir.RIGHT, turn)
        Dir.RIGHT -> Plan(Dir.UP, turn)
        Dir.DOWN -> Plan(Dir.LEFT, turn)
        Dir.LEFT -> Plan(Dir.DOWN, turn)
    }
    '\\' -> when (dir) {
        Dir.UP -> Plan(Dir.LEFT, turn)
        Dir.LEFT -> Plan(Dir.UP, turn)
        Dir.DOWN -> Plan(Dir.RIGHT, turn)
        Dir.RIGHT -> Plan(Dir.DOWN, turn)
    }
    '-' -> this
    '|' -> this
    else -> throw IllegalStateException()
}

fun Pos.apply(plan: Plan): Pos = when(plan.dir) {
    Dir.UP -> Pos(x, y-1)
    Dir.DOWN -> Pos(x, y+1)
    Dir.LEFT -> Pos(x-1, y)
    Dir.RIGHT -> Pos(x+1, y)
}

fun Cart.move(tracks: Tracks): Cart {
    val newPlan = plan.adjust(tracks[pos.y][pos.x])
    return Cart(id, pos.apply(newPlan), newPlan)
}

val Model.cartsInScanOrder: List<Cart> get() = carts.sortedWith(scanOrder)


// Part 1

fun Cart.checkCollisions(carts: Collection<Cart>): Cart =
    if (carts.any { c -> c.collided })
        this // only want the first collision
    else if (carts.none { c -> c.pos == pos })
        this
    else
        this.copy(collided=true)

fun Model.tickDetectingCollisions(): Model {
    val newCarts = cartsInScanOrder.fold(carts) { carts_, c ->
        val otherCarts = carts_.filter { it != c }
        otherCarts + c.move(tracks).checkCollisions(otherCarts)
    }
    return Model(tracks, newCarts)
}

fun Model.hasCollisions(): Boolean = carts.any { it.collided }

fun part1(input: Model): Pos {
    val endState = timeline(input) { m -> m.tickDetectingCollisions() }
        .dropWhile { m -> !m.hasCollisions() }
        .first()
    return endState.cartsInScanOrder.filter { c -> c.collided }.first().pos
}

// Part 2

fun Cart.collision(carts: Collection<Cart>): Cart? = carts.find { c -> c.pos == pos }

fun Model.tickRemovingCrashedCarts(): Model {
    val newCarts = cartsInScanOrder.fold(carts) { carts_, c ->
        if (!carts_.contains(c)) // removed by collision
            carts_
        else {
            val c_ = c.move(tracks)
            val otherCarts = carts_.filter { it != c }
            val crash = c_.collision(otherCarts)
            if (crash == null)
                otherCarts + c_
            else
                otherCarts.filterNot { it == crash }
        }
    }
    return Model(tracks, newCarts)
}

fun part2(input: Model): Pos {
    val endState = timeline(input) { m -> m.tickRemovingCrashedCarts() }
        .dropWhile { m -> m.carts.size > 1 }
        .first()
    return endState.carts.first().pos
}

fun main(vararg args: String) {
    val input = load(File(args[0]).readLines())
    println("Part 1: ${part1(input)}")
    println("Part 2: ${part2(input)}")
}
