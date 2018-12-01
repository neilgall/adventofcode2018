package adventofcode2018.toolbox

/**
 * Infinitely repeat a sequence
 */
fun <T> Sequence<T>.repeat() = generateSequence { asIterable() }.flatten()


/**
 * Like fold() but returns a sequence of all th accumulator values,
 * not just the last one. `seq.scanl(i, f).last() == seq.fold(i, f)`
 */
fun <T, U> Sequence<T>.scanl(initial: U, f: (U, T) -> U): Sequence<U> {
    var acc: U = initial
    return map { x -> acc = f(acc, x); acc }
}
