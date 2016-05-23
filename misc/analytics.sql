CREATE OR REPLACE FUNCTION cs_a_b_compare2
(bAssignmentId TEXT, aPoolId INT, aTags TEXT[], aWeightTh FLOAT)
RETURNS TABLE (pwnId INT, yarnId INT, src VARCHAR(10), weight FLOAT, clean BOOLEAN)
AS $$
    SELECT pwnId, yarnId,
          (CASE WHEN src = 1 THEN 'expert' WHEN src = 2 THEN 'aggr' WHEN src = 3 THEN 'both' ELSE 'none' END) as src,
          weight, clean
    FROM (
      SELECT COALESCE(ed.pwnId, ad.pwnId, td.pwnId) as pwnId, COALESCE(ed.yarnId, ad.yarnId, 0) as yarnId,
             ed.clean, ad.weight, COALESCE(ad.src,0) + COALESCE(ed.src,0) as src
      from
      (SELECT DISTINCT ts.pwnId FROM cs_a_task ts WHERE ts.poolId = aPoolId) td
      FULL OUTER JOIN
      (SELECT ba.pwnId, bas.yarnId, bas.clean, 1 as src from cs_b_answer ba left
            JOIN cs_b_answer_selected bas ON ba.Id = bas.answerId
            where ba.assignmentId = bAssignmentId
           ) ed ON ed.pwnId = td.pwnId
      FULL OUTER JOIN (SELECT t.pwnId, ts.yarnId, AVG(COALESCE(aa.weight,0)) as weight, 2 as src
                       FROM cs_a_task t
                       JOIN cs_a_aggregation aa ON aa.taskId = t.Id AND aWeightTh <= aa.weight AND aa.tag = ANY(aTags)
                       LEFT JOIN cs_a_task_synset ts ON ts.Id = aa.selectedId
                       WHERE t.poolId = aPoolId
                       GROUP BY t.pwnId, ts.yarnId
                      ) ad ON COALESCE(ed.yarnId,0) = COALESCE(ad.yarnId,0) AND ed.pwnId = ad.pwnId
                   ) t
    ORDER BY pwnId, t.src DESC, yarnId;
$$ language 'sql';

CREATE OR REPLACE FUNCTION cs_a_b_compare
(bAssignmentId TEXT, aPoolId INT, aTags TEXT[], aWeightTh FLOAT, ignoringHavingSrc TEXT[])
RETURNS TABLE (pwnId INT, yarnId INT, src VARCHAR(10), weight FLOAT, clean BOOLEAN)
AS $$
    SELECT t2.* FROM cs_a_b_compare2(bAssignmentId, aPoolId, aTags, aWeightTh) t2
        WHERE t2.pwnId NOT IN (SELECT DISTINCT t.pwnId
                                 FROM cs_a_b_compare2(bAssignmentId, aPoolId, aTags, aWeightTh) t
                                 WHERE t.src = ANY(ignoringHavingSrc))
$$ language 'sql';
