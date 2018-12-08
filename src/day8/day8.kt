package adventofcode2018.day8

import java.io.File

// Parsing

fun parse(input: String): List<Int> {
    return input.trim().split(" ").map(String::toInt)
}

// Part 1

data class Node(val children: List<Node>, val metadata: List<Int>)

fun Node.metadataTotal(): Int =
    metadata.sum() + children.fold(0) { n: Int, c: Node -> n + c.metadataTotal() }

fun tree(input: List<Int>): Pair<Node, List<Int>> {
    val numChildren = input[0]
    val numMetadata = input[1]

    val (children, remainder) = (1..numChildren).fold(Pair(listOf<Node>(), input.drop(2))) { (cs, input), _ -> 
        val (child, input_) = tree(input)
        Pair(cs + child, input_)
    }

    val metadata = remainder.take(numMetadata)
    val node = Node(children, metadata)
    return Pair(node, remainder.drop(numMetadata))
}

fun part1(input: List<Int>): Int = tree(input).first.metadataTotal()

fun main(args: Array<String>) {
    val input = parse(File(args[0]).readText())
    println("Part 1: ${part1(input)}")
}