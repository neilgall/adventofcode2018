
INPUT ?= input.txt
DAYS := $(patsubst %,day%,1 2 3 4 5 6)
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
		kotlin -cp $<:$$cp adventofcode2018.$@.$$class src/$@/$(INPUT)

build/day%.jar: src/day%/*.kt | toolbox
	src=`dirname $<` && \
	cp=`cat $$src/classpath 2>/dev/null || true` && \
		kotlinc -cp build/toolbox.jar:$$cp -d $@ $^

day3: jparsec 

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
