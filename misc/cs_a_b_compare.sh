#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "Usage: $0 <assignmentid> <poolIds> [tags list] [weight threshold] [discard list]" >&2
echo "Example: $0 950530ce-20f3-449b-8d60-22f42dff8dcc 12,20 \"'zencrowd','dawid-skene'\" 0.4 \"'both'\"" >&2

tagsList="'zencrowd'"
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

export QUERY="SELECT * FROM cs_a_b_compare('$1', ARRAY[$2], ARRAY[$tagsList], $weightTh, ARRAY[$discardList]::TEXT[]) c"
export TYPE=1

source "$DIR/cs_compare_base.sh"
