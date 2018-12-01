#!/bin/bash
for ext in jar class; do 
    find . -name *.${ext} -exec rm {} \;
done

