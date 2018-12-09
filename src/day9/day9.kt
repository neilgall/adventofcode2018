package adventofcode2018.day9

class Circle<T> {
    private var value: T
    private var prev: Circle<T>
    private var next: Circle<T>

    constructor(value: T, prev: Circle<T>? = null, next: Circle<T>? = null) {
        this.value = value
        this.prev = prev ?: this
        this.next = next ?: this
    }

    operator fun get(n: Int): T = when {
        n == 0 -> value
        else   -> plus(n).get(0)
    }

    operator fun set(n: Int, value: T): Unit = when {
        n == 0 -> this.value = value
        else   -> plus(n).set(0, value)
    }

    operator fun plus(n: Int): Circle<T> = when {
        n == 0 -> this
        n < 0  -> minus(-n)
        n > 0  -> next.plus(n-1)
        else   -> throw RuntimeException("kotlin is crazy")
    }

    operator fun minus(n: Int): Circle<T> = when {
        n == 0 -> this
        n < 0  -> plus(-n)
        n > 0  -> prev.minus(n-1)
        else   -> throw RuntimeException("kotlin is crazy")
    }

    fun insertClockwise(value: T): Circle<T> {
        val node = Circle(value, this, next)
        next.prev = node
        this.next = node
        return node
    }

    fun insertAnticlockwise(value: T): Circle<T> {
        val node = Circle(value, prev, this)
        prev.next = node
        this.prev = node
        return node
    }

    fun remove(): Circle<T> {
        if (prev == this && next == this)
            throw IllegalStateException("can't remove the last node in a circle")
        prev.next = next
        next.prev = prev
        return next
    }

    fun take(n: Int): List<T> = when {
        n == 0 -> listOf()
        n > 0  -> listOf(value) + next.take(n-1)
        n < 0  -> prev.take(n+1) + listOf(value)
        else   -> throw RuntimeException("kotlin is crazy")
    }
}

fun game(marbles: Int, players: Int): Long {
    var circle = Circle<Int>(0)
    var player = (2..players).fold(Circle<Long>(0)) { c, _ -> c.insertClockwise(0) }

    val (_, finalScores) = (1..marbles).fold(Pair(circle, player)) { (circle, player), marble ->
        when {
            marble % 23 == 0 -> {
                player[0] += (marble + circle[-7]).toLong()
                Pair((circle - 7).remove(), player + 1)
            }
            else -> {
                Pair((circle + 1).insertClockwise(marble), player + 1)
            }
        }
    }

    return finalScores.take(players).maxBy { it } ?: throw IllegalStateException()
}

fun testGame(marbles: Int, players: Int, expected: Long) {
    val score = game(marbles, players)
    val result = if (score == expected) "PASSED" else "FAILED"
    println("marbles=${marbles}, players=${players}: score=${score}: ${result}")
}

fun main(args: Array<String>) {
    testGame(32, 9, 32)
    testGame(1618, 10, 8317)
    testGame(7999, 13, 146373)
    testGame(1104, 17, 2764)
    testGame(6111, 21, 54718)
    testGame(5807, 30, 37305)

    println(game(70901, 429))
    println(game(7090100, 429))
}