#!/bin/bash
day=${1}
if [ -z "${day}" ]; then
  echo "Usage: run.sh <day> [<args>]"
  exit 1
fi
java -jar ${day}/${day}.jar $@
