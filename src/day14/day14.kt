package adventofcode2018.day14

fun part1(make: Int): String {
	val recipies = CharArray(make + 20) { '0' }
	fun recipe(i: Int) = (recipies[i] - '0').toInt()
	recipies[0] = '3'
	recipies[1] = '7'
	var length: Int = 2
	var elf1: Int = 0
	var elf2: Int = 1
	while (length < make + 10) {
		var new = (recipe(elf1) + recipe(elf2)).toString().toCharArray()
		new.forEachIndexed { i, c -> recipies[length + i] = c }
		length += new.size
		elf1 = (elf1 + 1 + recipe(elf1)) % length
		elf2 = (elf2 + 1 + recipe(elf2)) % length
	}
	return recipies.slice(make..make+9).joinToString("")
}

fun part1Test(make: Int, expect: String) {
	val result = part1(make)
	println("make $make: $result -- ${result == expect}")
}

fun main(vararg args: String) {
	part1Test(9, "5158916779")
	part1Test(5, "0124515891")
	part1Test(18, "9251071085")
	part1Test(2018, "5941429882")

	println("Part 1: ${part1(939601)}")
}
