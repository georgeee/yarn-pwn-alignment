CREATE OR REPLACE FUNCTION cs_a_winners
(poolIds INT [], tags TEXT [], weightTh FLOAT)
RETURNS TABLE (pwnId INT, yarnId INT, poolId INT, weight FLOAT)
AS $$
DECLARE
    nonNullIds INT[];
BEGIN
    nonNullIds := ARRAY(SELECT s.pwnId FROM cs_a_winners2(poolIds, tags, weightTh) s WHERE s.yarnId IS NOT NULL GROUP BY s.pwnId);
--    RAISE INFO 'array %', nonNullIds;
    RETURN QUERY SELECT s.pwnId, NULL, s.poolId, AVG(s.weight) as weight FROM cs_a_winners2(poolIds, tags, weightTh) s
                    WHERE s.pwnId != ALL(nonNullIds)
                    GROUP BY s.pwnId, s.poolId
    UNION
    SELECT s.pwnId, s.yarnId, s.poolId, s.weight FROM cs_a_winners2(poolIds, tags, weightTh) s WHERE s.pwnId = ANY(nonNullIds) AND s.yarnId IS NOT NULL;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION cs_a_winners2
(poolIds INT [], tags TEXT [], weightTh FLOAT)
RETURNS TABLE (pwnId INT, yarnId INT, poolId INT, taskId INT, weight FLOAT)
AS $$
    SELECT p.pwnId, ts.yarnId, t.poolId, t.id as taskId, AVG(aggr.weight) as weight
    FROM (SELECT DISTINCT pwnId FROM cs_a_task WHERE poolId = ANY(poolIds)) p
    JOIN cs_a_task t ON t.pwnId = p.pwnId
         AND t.poolId = (SELECT max(t2.poolId) from cs_a_task t2
                        JOIN cs_a_aggregation aggr ON aggr.taskId = t2.id
                        WHERE t2.pwnId = p.pwnId AND t2.poolId = ANY(poolIds))
    JOIN cs_a_aggregation aggr ON aggr.taskId = t.id
         AND aggr.tag = ANY(tags) AND aggr.weight > weightTh
    LEFT JOIN cs_a_task_synset ts ON ts.id = aggr.selectedId
    GROUP BY p.pwnId, ts.yarnId, t.poolId, t.id
$$ language 'sql';

CREATE OR REPLACE FUNCTION cs_b_b_compare2
(bAssignmentId1 TEXT, bAssignmentId2 TEXT)
RETURNS TABLE (pwnId INT, yarnId INT, src INT, srcLabel VARCHAR(10), clean1 BOOLEAN, clean2 BOOLEAN)
AS $$
    SELECT pwnId, yarnId, src,
          (CASE WHEN src = 1 THEN 'e1' WHEN src = 2 THEN 'e2' WHEN src = 3 THEN 'both' ELSE 'none' END) as srcLabel,
          clean1, clean2
    FROM (
      SELECT COALESCE(ed.pwnId, ad.pwnId) as pwnId, COALESCE(ed.yarnId, ad.yarnId, 0) as yarnId,
             ed.clean as clean1, ad.clean as clean2, COALESCE(ad.src,0) + COALESCE(ed.src,0) as src
      from
      (SELECT ba.pwnId, bas.yarnId, bas.clean, 1 as src from cs_b_answer ba left
            JOIN cs_b_answer_selected bas ON ba.Id = bas.answerId
            where ba.assignmentId = bAssignmentId1
           ) ed
      FULL OUTER JOIN (SELECT ba.pwnId, bas.yarnId, bas.clean, 2 as src from cs_b_answer ba left
            JOIN cs_b_answer_selected bas ON ba.Id = bas.answerId
            where ba.assignmentId = bAssignmentId2
           ) ad ON COALESCE(ed.yarnId,0) = COALESCE(ad.yarnId,0) AND ed.pwnId = ad.pwnId
                   ) t
    ORDER BY pwnId, t.src DESC, yarnId;
$$ language 'sql';

CREATE OR REPLACE FUNCTION cs_b_b_compare
(bAssignmentId1 TEXT, bAssignmentId2 TEXT, ignoringHavingSrc TEXT[])
RETURNS TABLE (pwnId INT, yarnId INT, src INT, srcLabel VARCHAR(10), clean1 BOOLEAN, clean2 BOOLEAN)
AS $$
    SELECT t2.* FROM cs_b_b_compare2(bAssignmentId1, bAssignmentId2) t2
        WHERE t2.pwnId NOT IN (SELECT DISTINCT t.pwnId
                                 FROM cs_b_b_compare2(bAssignmentId1, bAssignmentId2) t
                                 WHERE t.srcLabel = ANY(ignoringHavingSrc))
$$ language 'sql';

CREATE OR REPLACE FUNCTION cs_a_b_compare2
(bAssignmentId TEXT, aPoolIds INT[], aTags TEXT[], aWeightTh FLOAT)
RETURNS TABLE (pwnId INT, yarnId INT, src INT, srcLabel VARCHAR(10), weight FLOAT, clean BOOLEAN)
AS $$
    SELECT pwnId, yarnId, src,
          (CASE WHEN src = 1 THEN 'expert' WHEN src = 2 THEN 'aggr' WHEN src = 3 THEN 'both' ELSE 'none' END) as srcLabel,
          weight, clean
    FROM (
      SELECT COALESCE(ed.pwnId, ad.pwnId, td.pwnId) as pwnId, COALESCE(ed.yarnId, ad.yarnId, 0) as yarnId,
             ed.clean, ad.weight, COALESCE(ad.src,0) + COALESCE(ed.src,0) as src
      from
      (SELECT DISTINCT ts.pwnId FROM cs_a_task ts WHERE ts.poolId = ANY(aPoolIds)) td
      FULL OUTER JOIN
      (SELECT ba.pwnId, bas.yarnId, bas.clean, 1 as src from cs_b_answer ba left
            JOIN cs_b_answer_selected bas ON ba.Id = bas.answerId
            where ba.assignmentId = bAssignmentId
           ) ed ON ed.pwnId = td.pwnId
      FULL OUTER JOIN
      (SELECT w.*, 2 as src
            FROM cs_a_winners(aPoolIds, aTags, aWeightTh) w
      ) ad ON COALESCE(ed.yarnId,0) = COALESCE(ad.yarnId,0) AND ed.pwnId = ad.pwnId
                   ) t
    ORDER BY pwnId, t.src DESC, yarnId;
$$ language 'sql';

CREATE OR REPLACE FUNCTION cs_a_b_compare
(bAssignmentId TEXT, aPoolIds INT[], aTags TEXT[], aWeightTh FLOAT, ignoringHavingSrc TEXT[])
RETURNS TABLE (pwnId INT, yarnId INT, src INT, srcLabel VARCHAR(10), weight FLOAT, clean BOOLEAN)
AS $$
    SELECT t2.* FROM cs_a_b_compare2(bAssignmentId, aPoolIds, aTags, aWeightTh) t2
        WHERE t2.pwnId NOT IN (SELECT DISTINCT t.pwnId
                                 FROM cs_a_b_compare2(bAssignmentId, aPoolIds, aTags, aWeightTh) t
                                 WHERE t.srcLabel = ANY(ignoringHavingSrc))
$$ language 'sql';

CREATE OR REPLACE FUNCTION translate_edge_mastered (threshold INT)
RETURNS TABLE (id INT, pwnId INT, yarnId INT, masterEdgeId INT, weight FLOAT, mYarnId INT, mWeight FLOAT)
AS $$
  SELECT se.*, ee.yarnId, ee.weight
  FROM (SELECT e.*, rank() over (partition by e.pwnid ORDER BY weight DESC) as rnk
        FROM translate_edge e  WHERE e.masteredgeid IS NULL
        ORDER BY pwnid, Weight DESC) ee
  JOIN Translate_Edge se ON se.masterEdgeId = ee.id WHERE ee.rnk <= threshold
  UNION 
  SELECT NULL, ee.pwnId, NULL, ee.id, NULL, ee.yarnId, ee.weight
  FROM (SELECT e.*, rank() over (partition by e.pwnid ORDER BY weight DESC) as rnk
        FROM translate_edge e  WHERE e.masteredgeid IS NULL
        ORDER BY pwnid, Weight DESC) ee
  WHERE ee.rnk <= threshold;
$$ language 'sql';

CREATE OR REPLACE FUNCTION translate_edge_mastered_for_export (threshold INT)
RETURNS TABLE (pwnId VARCHAR(14), masterYarnId VARCHAR(14), yarnId VARCHAR(14), iPwnId INT, iMasterYarnId INT, iYarnId INT)
AS $$
  SELECT ps.externalId, mys.externalId, ys.externalId, ps.id, mys.id, ys.id
  FROM translate_edge_mastered(threshold) tm
  JOIN synset ps ON ps.Id = tm.pwnId
  LEFT JOIN synset ys ON ys.id = tm.yarnId
  LEFT JOIN synset mys ON mys.id = tm.mYarnId
  ORDER BY ps.externalId, mys.externalId, ys.externalId DESC;
$$ language 'sql';
