#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

poolId=$1

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

mkdir "$DIR/$poolId/aggregation"
cd "$DIR/$poolId/aggregation"

echo "Majority aggregation"
curl -s -X PATCH "http://127.0.0.1:8080/stages/$stage" -d "answerAggregator=mtsar.processors.answer.MajorityVoting"
wget --timeout=0 --tries=1 -O majority.csv "http://127.0.0.1:8080/stages/$stage/answers/aggregations.csv"

echo "KOS aggregation"
curl -s -X PATCH "http://127.0.0.1:8080/stages/$stage" -d "answerAggregator=mtsar.processors.answer.KOSAggregator"
wget --timeout=0 --tries=1 -O kos.csv "http://127.0.0.1:8080/stages/$stage/answers/aggregations.csv"

echo "Zencrowd aggregation"
curl -s -X PATCH "http://127.0.0.1:8080/stages/$stage" -d "answerAggregator=mtsar.processors.meta.ZenCrowd"
wget --timeout=0 --tries=1 -O zencrowd.csv "http://127.0.0.1:8080/stages/$stage/answers/aggregations.csv"

for i in majority zencrowd kos; do
  "$DIR/aggrCsv2InputJson.sh" "$i.csv" > "$i.json"
done
