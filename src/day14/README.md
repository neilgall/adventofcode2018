# Day 14
Today's is a low-level one. Might be interesting to do it in assembly language,
if I could remember any. The key insight is you're going to eventually need a
very large array but we only ever append to it, so preallocating the buffer is 
a sensible idea for performance. I decided to forego all the high-level modelling
and implement it C-style.

## Part 1

First make a big buffer and initialise all the state we need.
```
val recipies = CharArray(make + 20) { '0' }
	recipies[0] = '3'
	recipies[1] = '7'
	var length: Int = 2
	var elf1: Int = 0
	var elf2: Int = 1
```
I'm storing the values as the ASCII characters for the digits, so one high-level
language comfort is a helper function to pull out the values.
```
fun recipe(i: Int) = (recipies[i] - '0').toInt()
```
Then run the algorithm until we have enough data in the buffer:
```
while (length < make + 10) {
	var new = (recipe(elf1) + recipe(elf2)).toString().toCharArray()
	new.forEachIndexed { i, c -> recipies[length + i] = c }
	length += new.size
	elf1 = (elf1 + 1 + recipe(elf1)) % length
	elf2 = (elf2 + 1 + recipe(elf2)) % length
}
```
And extract the answer:
```
return recipies.slice(make..make+9).joinToString("")
}
```

## Part 2
Part 2 makes the problem slightly harder in that we don't know the eventual
size of the buffer. A classic approach is to start with some sensible number
(let's say 1024) and reallocate it twice the size each time we hit the end.
The number of copies is therefore limited to log2(N).

```
if (length + new.size >= recipies.size) {
	recipies += CharArray(recipies.size) { '0' }
}
```

The other tricky part is that we don't know how many digits are added to
the array each iteration, so we can't assume the target string is right
at the end. I therefore search for the target string from 5 characters before
the old end to the new end of the array.

```
if (oldLength >= 5) {
	val index = recipies.slice(oldLength-5..length-1).joinToString("").indexOf(input)
	if (index > -1) {
		return oldLength-5+index
	}
}
```
