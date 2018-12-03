package adventofcode2018.day3

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

data class Origin(val left: Int, val top: Int)
data class Size(val width: Int, val height: Int)
data class Box(val x: IntRange, val y: IntRange)
data class Pos(val x: Int, val y: Int)

data class Claim(val id: Int, val box: Box) {
    constructor(id: Int, origin: Origin, size: Size):
        this(id, Box((origin.left .. origin.left + size.width - 1), (origin.top .. origin.top + size.height - 1)))

    fun positions(): Sequence<Pos> = box.x.asSequence().flatMap { x ->
        box.y.asSequence().map { y -> Pos(x, y) }
    }
}

val integer: Parser<Int> = INTEGER.map(String::toInt)
fun <T> integerPair(sep: Char, c: (Int, Int) -> T): Parser<T> =  
    sequence(integer.followedBy(isChar(sep)), integer, c)

val claimId: Parser<Int> = isChar('#').next(integer)
val origin: Parser<Origin> = WHITESPACES.followedBy(isChar('@')).followedBy(WHITESPACES).next(integerPair(',', ::Origin))
val size: Parser<Size> = isChar(':').followedBy(WHITESPACES).next(integerPair('x', ::Size))
val claim: Parser<Claim> = sequence(claimId, origin, size, ::Claim)

fun part1(claims: List<Claim>) {
    val material = mutableMapOf<Pos, Int>()
    claims.forEach { claim -> claim.positions().forEach { pos -> material[pos] = (material[pos] ?: 0) + 1 } }
    val overlapping = material.filterValues { it >= 2 }.size
    println("Part 1, multiply-claimed material: ${overlapping} square inches")
}

fun main(args: Array<String>) {
    val input: List<Claim> = File(args[0]).readLines().map(claim::parse)
    part1(input)
}

