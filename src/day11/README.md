# Day 11
Not a lot to say about this. Model the grid, model the 3x3 groups
and add the accessors. Solving part 1 is a `maxBy()`

Part 2 requires replacing the `Pos.Nine` and `nines` sequence
with a `Pos.Square` and `squares` sequence. The rest remains 
the same. The runtime is significantly longer though, so there is 
presumably a more efficient solution.

Approximately 9 million squares need calculated. You could perhaps
cache the value of each square in memory, starting with the smaller
ones and computing larger ones by summing the values of known smaller
squares. A binary breakdown might work whereby you calculate all the
1x1s, 2x2s, 4x4s, 8x8s etc. Then any square can be calculated by
summing the squares associated with its binary bits. I have no idea
how much faster this would be.
