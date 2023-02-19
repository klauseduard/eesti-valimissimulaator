#!/bin/bash
#
# this simple script was actually produced by chatGPT, not Copilot

# Check that the user provided a command line argument
if [ $# -eq 0 ]; then
    echo "Usage: $0 <number of times to run>"
    exit 1
fi

# Parse the command line argument as an integer
n=$1

# Create the 'runs' directory if it doesn't exist
if [ ! -d "runs" ]; then
    echo "Creating 'runs' directory..."
    mkdir runs
fi

# Run ./gradlew run n times
for ((i=1; i<=n; i++)); do
    echo "Running ./gradlew run (iteration $i of $n)..."
    ./gradlew run
    echo "Sleeping for 1 second..."
    sleep 1
done
