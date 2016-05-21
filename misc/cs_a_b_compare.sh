#!/bin/bash

echo "Usage: $0 <assignmentid> <poolId> <tagId> [<discard_both=false>]" >&2

QUERY="SELECT * FROM CS_A_B_COMPARE WHERE (assignmentid IS NULL OR assignmentid = '$1') AND COALESCE(poolId,$2)=12 AND (tag IS NULL OR tag = '$3')"

echo "$QUERY" >&2

discard_both=$4

PAT='s/^([0-9]+)\|([0-9]+)\|([a-z]+)\|.*$'

echo "{"

last=-1
print_current=true

echo "$QUERY" | psql -d yarn_pwn_alignment -A -t | while read l; do
  pwnId=`echo "$l" | sed -r "$PAT/\1/"`
  yarnId=`echo "$l" | sed -r "$PAT/\2/"`
  src=`echo "$l" | sed -r "$PAT/\3/"`
  if [[ "$last" != "$pwnId" ]]; then
    print_current=true
    if [[ "$src" == 'both' ]] && $discard_both; then
      print_current=false
    fi
  fi
  if $print_current; then
    if [[ "$last" = "-1" ]]; then
      echo -n "\"$pwnId\": {"
    elif [[ "$last" == "$pwnId" ]]; then
      echo -n ","
    else
      echo "},"
      echo -n "\"$pwnId\": {"
    fi
    if [[ "$src" = 'both' ]]; then
      echo -n "\"e_checked_$yarnId\":1,"
      echo -n "\"a_checked_$yarnId\":1"
    elif [[ "$src" = 'aggr' ]]; then
      echo -n "\"a_checked_$yarnId\":1"
    elif [[ "$src" = 'expert' ]]; then
      echo -n "\"e_checked_$yarnId\":1"
    fi
  fi
  last=$pwnId
  # echo "$pwnId $yarnId $src"
done
echo "}}"
