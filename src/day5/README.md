# Day 5
Seems pretty simple. Find and remove the reacting units (characters) from
the polymer (string) repeatedly until it is stable. I thought I'd be clever
and try to model the removals rather than do lots of array or string copying.
The idea was an `IntRange` for each reaction, which starts at two positions,
growing to the left and right as the now-adjacent units underwent further
reactions. It was too clever and I couldn't get the right answer to come out!
The code for this attempt can be seen in day5b.kt

So I resorted to the simple algorithm of scanning the list of units and
building a new list, dropping them as appropriate. Then run again until it
is stable. Pretty much got it working first time.

Part 2 was a straightforward extension. Remove the candidate units, and run
the part 1 algorithm on the result. Collect these results and pick the
smallest.

I didn't enjoy this one.