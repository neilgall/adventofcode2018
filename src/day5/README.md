# Day 5
Seems pretty simple. Find and remove the reacting units (characters) from
the polymer (string) repeatedly until it is stable. The naive approach
is to scan from the beginning, remove matching pairs until the end is
hit, then do it again until no changes are made. This repeats a lot of
work however. It can be done in a single scan.

Starting at the beginning of the string, examine every two characters,
at positions N and N+1. If they react, remove them. This brings the 
characters at N-1 and N+2 together, so move *back* one position to N-1
and continue to the end.

The other obvious inefficiency is string copying. There's no need to
duplicate the rest of the string just to remove to characters. Just
remember the positions which have been removed and use that as a character
index.