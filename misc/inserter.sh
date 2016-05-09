#!/bin/bash

db=yarn_pwn_alignment

ls | while read s; do
  echo "INSERT INTO Synset (ExternalId, Source) VALUES ('$s', 'PWN')" | psql -d $db 2>/dev/null 1>/dev/null
  id=`echo "SELECT Id FROM Synset WHERE ExternalId = '$s'"| psql -q -d $db -t -A`
  ls $s | while read fn; do
    echo "INSERT INTO Synset_Image (SynsetId, Filename, Source) VALUES ($id, '$fn', 'IMAGENET');"
  done | psql -q -d $db
done
