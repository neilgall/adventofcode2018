
INPUT ?= input.txt
DAYS := $(patsubst %,day%,1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25)
all: $(DAYS)

.PHONY: all clean

clean:
	rm -rf build lib

build:
	mkdir -p build

lib:
	mkdir -p lib

day%: build/day%.jar
	cp=`cat src/$@/classpath 2>/dev/null || true` && \
	class=`python -c 'print("$@".capitalize() + "Kt")'` && \
		kotlin -J-Xmx2048m -cp $<:$$cp adventofcode2018.$@.$$class src/$@/$(INPUT)

build/day%.jar: src/day%/*.kt | toolbox
	src=`dirname $<` && \
	cp=`cat $$src/classpath 2>/dev/null || true` && \
		kotlinc -cp build/toolbox.jar:$$cp -d $@ $^

day3: jparsec 
day4: jparsec
day6: jparsec
day7: jparsec
day8: jparsec
day10: jparsec
day12: jparsec
day16: jparsec
day17: jparsec
day19: jparsec
day21: jparsec
day22: jparsec
day23: jparsec
day24: jparsec
day25: jparsec

.PHONY: toolbox
toolbox: build/toolbox.jar

build/toolbox.jar: src/toolbox/*.kt | build
	kotlinc -d $@ $^

.PHONY: jparsec
jparsec: lib lib/jparsec/jparsec/target/jparsec-3.1-SNAPSHOT.jar

lib/jparsec/jparsec/target/jparsec-3.1-SNAPSHOT.jar:
	(cd lib && \
		git clone https://github.com/jparsec/jparsec.git && \
		cd jparsec && \
		mvn package)
