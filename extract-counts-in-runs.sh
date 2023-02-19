#!/bin/bash

# Concatenate all .txt files in the 'runs' directory and write them to 'all_runs.txt'
cat runs/*.txt > all_runs.txt

# Extract the desired portion of each line and sort them alphabetically
# Count the number of times each line occurs and print the counts along with the lines
cut -d',' -f1,2,3 all_runs.txt | sort | uniq -c | sort -nr > stats.txt

