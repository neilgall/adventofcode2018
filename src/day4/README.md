# Day 4
Today we're presented with event data where th events are not necessarily
ordered and the information they contain in partial. This sort of scenario
is pretty common in communications protocols and user interface code. I'm
going to break the problem down as follows:

1. Parse the input data to an event list
2. Sort the event list by time if that's necessary
3. Run the events through an interpreter to assemble all the information
4. Keep asleep counts keyed by minute so the most frequent can be found

It would be possible to merge these things into a single step, but we don't
know what's coming in part 2 and breaking it into indepedent steps keeps
the code clear and flexible.

## Parsing
Of course I'm going to use the One True Parsing method of parser combinators
again. It can seem like overkill for such simple examples but in the real
world it pays off in the long run.