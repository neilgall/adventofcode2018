#!/bin/bash
set -e
if [ -z "$1" ]; then
  days=`ls -d day*`
else
  days=$@
fi

for day in ${days}; do
  echo "Building ${day}..."
  kotlinc ${day}/*.kt -include-runtime -d ${day}/${day}.jar
done
