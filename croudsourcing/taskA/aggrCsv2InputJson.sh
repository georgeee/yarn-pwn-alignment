#!/bin/bash

echo "{"
tail -n +2 "$1" | head -1 | sed -r 's/([0-9]+),([0-9]+)/"\1":{"selectedId":\2}/'
tail -n +3 "$1" | sed -r 's/([0-9]+),([0-9]+)/,"\1":{"selectedId":\2}/'
echo "}"
