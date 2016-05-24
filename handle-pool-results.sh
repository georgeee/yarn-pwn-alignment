#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

poolId=$1
file=$2

java -jar "$DIR/alignment/target/alignment-0.1.jar" --action=a.at --file=$file
java -jar "$DIR/alignment/target/alignment-0.1.jar" --action=a.em --poolId=$poolId
"$DIR/croudsourcing/taskA/aggregate-mtsar.sh" $poolId

