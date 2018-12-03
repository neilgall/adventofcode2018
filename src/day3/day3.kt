package adventofcode2018.day3

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Scanners.*

data class Origin(val left: Int, val top: Int)
data class Size(val width: Int, val height: Int)
data class Claim(val id: Int, val origin: Origin, val size: Size)

val integer: Parser<Int> = INTEGER.map(String::toInt)
fun <T> integerPair(sep: Char, c: (Int, Int) -> T): Parser<T> =  
    sequence(integer.followedBy(isChar(sep)), integer, c)

val claimId: Parser<Int> = isChar('#').next(integer)
val origin: Parser<Origin> = WHITESPACES.followedBy(isChar('@')).followedBy(WHITESPACES).next(integerPair(',', ::Origin))
val size: Parser<Size> = isChar(':').followedBy(WHITESPACES).next(integerPair('x', ::Size))
val claim: Parser<Claim> = sequence(claimId, origin, size, ::Claim)

fun main(args: Array<String>) {
    val input: List<Claim> = File(args[0]).readLines().map(claim::parse)

    println("Part 1, ${input}")
}

