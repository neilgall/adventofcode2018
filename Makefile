
DAYS := $(patsubst %,day%,1 2)
all: $(DAYS)

.PHONY: all clean

clean:
	rm -rf build

build:
	mkdir -p build

day%: build/day%.jar
	jar=`realpath $^` && cd src/`basename $< .jar` && kotlin $$jar

build/day%.jar: src/day%/*.kt src/toolbox/*.kt | build
	kotlinc -include-runtime -no-reflect -d $@ $^
