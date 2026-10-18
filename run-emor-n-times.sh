#!/bin/bash
#
# Kordab simulatsiooni n korda ühes protsessis ja kirjutab kandidaatide valituks
# osutumise sagedused faili runs/<ajatempel>-sagedused.txt.
# Lisaargumendid antakse programmile edasi, nt: ./run-emor-n-times.sh 100 --seed=1

if [ $# -eq 0 ]; then
    echo "Usage: $0 <number of times to run> [program arguments]"
    exit 1
fi

n=$1
shift

./gradlew run --args="--runs=$n --pollster=emor $*"
