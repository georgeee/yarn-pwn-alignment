#!/bin/bash

echo "SELECT q.pwnId, COALESCE(q.yarnId, q.masterYarnId) as yarnId from translate_edge_mastered_for_export(15) q JOIN (SELECT DISTINCT pwnId FROM cs_a_b_compare('465d906c-dd6a-4080-8a7a-9e02b6171129', ARRAY[12,20], ARRAY['zencrowd'], 0.4, ARRAY['both']::TEXT[])) c ON c.pwnId = q.iPwnId" | psql -d yarn_pwn_alignment -A -F , | head -n -1
