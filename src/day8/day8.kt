package adventofcode2018.day8

import java.io.File
import org.jparsec.Parser
import org.jparsec.Parsers.*
import org.jparsec.Terminals
import org.jparsec.Scanners

// Parsing

data class Node(val children: List<Node>, val metadata: List<Int>)

fun parse(input: String): Node {
    val integer = Terminals.IntegerLiteral.PARSER.map(String::toInt)

    val treeRef = Parser.Reference<Node>()

    fun nodeParser(numChildren: Int, numMetadata: Int): Parser<Node> =
        sequence(
            treeRef.lazy().times(numChildren),
            integer.times(numMetadata),
            ::Node
        )

    val nodeInfo: Parser<Pair<Int, Int>> = sequence(integer, integer) { nc, nm -> Pair(nc, nm) }
    val tree: Parser<Node> = nodeInfo.next { (nc, nm) -> nodeParser(nc, nm) }
    treeRef.set(tree)

    val parser = tree.from(Terminals.IntegerLiteral.TOKENIZER, Scanners.WHITESPACES)
    return parser.parse(input.trim())
}

// Part 1

fun Node.metadataTotal(): Int =
    metadata.sum() + children.fold(0) { n: Int, c: Node -> n + c.metadataTotal() }

fun part1(input: Node): Int = input.metadataTotal()

// Part 2

fun Node.metadataComplex(): Int =
    if (children.isEmpty())
        metadata.sum()
    else
        metadata.fold(0) { total, i ->
            total + (if (children.indices.contains(i-1)) children[i-1].metadataComplex() else 0)
        }

fun part2(input: Node): Int = input.metadataComplex()

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    println("Part 1: ${part1(input)}")
    println("Part 2: ${part2(input)}")
}