#!/bin/bash

echo "Usage: $0 <assignmentid> <poolId> [tags list] [weight threshold] [discard list]" >&2
echo "Example: $0 950530ce-20f3-449b-8d60-22f42dff8dcc 12 \"'zencrowd','dawid-skene'\" 0.4 \"'both'\"" >&2

tagsList="'zencrowd','dawid-skene'"
discardList=""
weightTh=0.4

if [[ "$3" != "" ]]; then
    tagsList="$3"
fi

if [[ "$4" != "" ]]; then
    weightTh="$4"
fi

if [[ "$5" != "" ]]; then
    discardList="$5"
fi

QUERY="SELECT * FROM cs_a_b_compare('$1', $2, ARRAY[$tagsList], $weightTh, ARRAY[$discardList]::TEXT[]) c"

echo "$QUERY" >&2

PAT='s/^([^\|]*)\|([^\|]*)\|([^\|]*)\|([^\|]*)\|([^\|]*)$'

echo "{"

last=-1

echo "$QUERY" | psql -d yarn_pwn_alignment -A -t | while read l; do
  pwnId=`echo "$l" | sed -r "$PAT/\1/"`
  yarnId=`echo "$l" | sed -r "$PAT/\2/"`
  src=`echo "$l" | sed -r "$PAT/\3/"`
  weight=`echo "$l" | sed -r "$PAT/\4/"`
  if [[ "$weight" == "" ]]; then
    weight=null
  fi
  clean_=`echo "$l" | sed -r "$PAT/\5/"`
  clean=null
  if [[ "$clean_" == "t" ]]; then
    clean=true
  elif [[ "$clean_" == "f" ]]; then
    clean=false
  fi
  if [[ "$last" = "-1" ]]; then
    echo -n "\"$pwnId\": {"
  elif [[ "$last" == "$pwnId" ]]; then
    echo -n ","
  else
    echo "},"
    echo -n "\"$pwnId\": {"
  fi
  echo -n "\"weight_$yarnId\":$weight,"
  echo -n "\"clean_$yarnId\":$clean,"
  if [[ "$src" = 'both' ]]; then
    echo -n "\"e_checked_$yarnId\":1,"
    echo -n "\"a_checked_$yarnId\":1"
  elif [[ "$src" = 'aggr' ]]; then
    echo -n "\"a_checked_$yarnId\":1"
  elif [[ "$src" = 'expert' ]]; then
    echo -n "\"e_checked_$yarnId\":1"
  fi
  last=$pwnId
  # echo "$pwnId $yarnId $src"
done
echo "}}"
