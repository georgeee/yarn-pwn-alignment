#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "Usage: $0 <assignmentid1> <assignmentid1> [discard list]" >&2
echo "Example: $0 950530ce-20f3-449b-8d60-22f42dff8dcc 950530ce-20f3-449b-8d60-22f42d444444 \"'both'\"" >&2

discardList=""

if [[ "$3" != "" ]]; then
    discardList="$3"
fi

export QUERY="SELECT * FROM cs_b_b_compare('$1', '$2', ARRAY[$discardList]::TEXT[]) c"
export TYPE=2

source "$DIR/cs_compare_base.sh"
