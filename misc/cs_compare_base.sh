#!/bin/bash

echo "$QUERY" >&2
#       pwnId       yarnId     src     srcLabel   weight/clean1   clean1/clean2
PAT='s/^([^\|]*)\|([^\|]*)\|([^\|]*)\|([^\|]*)\|([^\|]*)\|([^\|]*)$'

last=-1

function parseBool {
  if [[ "$1" == "t" ]]; then
    echo true
  elif [[ "$1" == "f" ]]; then
    echo false
  fi
}

echo "{"
echo "$QUERY" | psql -d yarn_pwn_alignment -A -t | while read l; do
  function arg {
     echo "$l" | sed -r "$PAT/\\$1/"
  }
  pwnId=`arg 1`
  yarnId=`arg 2`
  src=`arg 3`
  if [[ "$last" = "-1" ]]; then
    echo -n "\"$pwnId\": {"
  elif [[ "$last" == "$pwnId" ]]; then
    echo -n ","
  else
    echo "},"
    echo -n "\"$pwnId\": {"
  fi
  if [[ $TYPE == 1 ]]; then
    weight=`arg 5`
    clean=`parseBool $(arg 6)`
    if [[ "$weight" != "" ]]; then
        echo -n "\"weight_$yarnId\":$weight,"
    fi
    if [[ "$clean" != "" ]]; then
        echo -n "\"e_clean_$yarnId\":$clean,"
    fi
  elif [[ $TYPE == 2 ]]; then
    clean1=`parseBool $(arg 5)`
    clean2=`parseBool $(arg 6)`
    if [[ "$clean1" != "" ]]; then
        echo -n "\"e_clean_$yarnId\":$clean1,"
    fi
    if [[ "$clean2" != "" ]]; then
        echo -n "\"a_clean_$yarnId\":$clean2,"
    fi
  fi
  if [[ "$src" = 3 ]]; then
    echo -n "\"e_checked_$yarnId\":1,"
    echo -n "\"a_checked_$yarnId\":1"
  elif [[ "$src" = 1 ]]; then
    echo -n "\"e_checked_$yarnId\":1"
  elif [[ "$src" = 2 ]]; then
    echo -n "\"a_checked_$yarnId\":1"
  fi
  last=$pwnId
  # echo "$pwnId $yarnId $src"
done
echo "}}"
