#!/bin/bash

function urlencode() {
  echo -n "$1" | perl -MURI::Escape -ne 'print uri_escape($_)'
}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

poolId=$1
mod=$2

cd "$DIR/$poolId/mtsar"

stage="alignment-taskA-pool$poolId"

curl -s -X POST "http://127.0.0.1:8080/stages" -d "id=$stage&workerRanker=mtsar.processors.meta.ZenCrowd&taskAllocator=mtsar.processors.task.RandomAllocator&answerAggregator=mtsar.processors.answer.MajorityVoting&description=taskA_$poolId"

echo -n "Workers: "
curl -F 'file=@workers.csv' "http://127.0.0.1:8080/stages/$stage/workers"
echo ''
echo -n "Tasks: "
curl -F 'file=@tasks.csv' "http://127.0.0.1:8080/stages/$stage/tasks"
echo ''
echo -n "Answers: "
curl -F 'file=@answers.csv' "http://127.0.0.1:8080/stages/$stage/answers"
echo ''

aggrDir="$DIR/$poolId/aggregation"

mkdir "$aggrDir"
cd "$aggrDir"

function aggregate {
  if [[ ! -f "$1$mod.csv" ]]; then
    echo "$1 aggregation"
    curl -s -X PATCH "http://127.0.0.1:8080/stages/$stage" -d $2
    wget --timeout=0 --tries=1 -O $1$mod.csv "http://127.0.0.1:8080/stages/$stage/answers/aggregations.csv"
  fi
}

opts=`urlencode "{\"maxIter\": 50000}"`

aggregate majority "workerRanker=mtsar.processors.meta.ZenCrowd&answerAggregator=mtsar.processors.answer.MajorityVoting"
aggregate zencrowd "workerRanker=mtsar.processors.meta.ZenCrowd&answerAggregator=mtsar.processors.meta.ZenCrowd&options=$opts"
aggregate dawid-skene "workerRanker=mtsar.processors.meta.DawidSkeneProcessor&answerAggregator=mtsar.processors.meta.DawidSkeneProcessor&options=$opts"

cd "$DIR/../.."
java -jar alignment/target/alignment-0.1.jar --action=a.aggr --files="$aggrDir/zencrowd$mod.csv,$aggrDir/dawid-skene$mod.csv,$aggrDir/majority$mod.csv"
java -jar alignment/target/alignment-0.1.jar --action=a.eaj --poolId=$poolId
