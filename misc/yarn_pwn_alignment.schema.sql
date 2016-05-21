CREATE TABLE Translate_Edge (
  Id SERIAL NOT NULL,
  PwnId INT NOT NULL,
  YarnId INT NOT NULL,
  MasterEdgeId INT,
  Weight FLOAT NOT NULL,
  PRIMARY KEY (PwnId, YarnId)
);

CREATE TABLE Synset (
  Id SERIAL NOT NULL PRIMARY KEY,
  ExternalId VARCHAR(14) NOT NULL,
  Source VARCHAR(7) NOT NULL
);

ALTER TABLE Translate_Edge ADD CONSTRAINT FK_Translate_Edge_PwnId FOREIGN KEY (PwnId) REFERENCES Synset (Id);
ALTER TABLE Translate_Edge ADD CONSTRAINT FK_Translate_Edge_YarnId FOREIGN KEY (YarnId) REFERENCES Synset (Id);
ALTER TABLE Translate_Edge ADD CONSTRAINT UQ_Translate_Edge_Id UNIQUE (Id);
ALTER TABLE Translate_Edge ADD CONSTRAINT FK_Translate_Edge_MasterEdgeId FOREIGN KEY (MasterEdgeId) REFERENCES Translate_Edge (Id);
ALTER TABLE Translate_Edge ADD CONSTRAINT CH_Translate_Edge_Weight CHECK (Weight >= 0 AND Weight <= 1);
CREATE INDEX ON Translate_Edge (Weight DESC);

ALTER TABLE Synset ADD CONSTRAINT UQ_Synset_ExternalId UNIQUE (ExternalId);

CREATE TABLE Synset_Image (
  Id SERIAL NOT NULL PRIMARY KEY,
  SynsetId INT NOT NULL,
  Filename VARCHAR(255) NOT NULL,
  Priority INT,
  Source VARCHAR(20) NOT NULL
);

ALTER TABLE Synset_Image ADD CONSTRAINT UQ_Synset_Image_SynsetId_Filename UNIQUE (SynsetId, Filename);
ALTER TABLE Synset_Image ADD CONSTRAINT FK_Synset_Image_SynsetId FOREIGN KEY (SynsetId) REFERENCES Synset (Id);

CREATE TABLE CS_A_Pool (
    Id SERIAL NOT NULL PRIMARY KEY,
    PredecessorId INT,
    Completed BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE CS_A_Task (
    Id SERIAL NOT NULL PRIMARY KEY,
    PwnId INT NOT NULL,
    PoolId INT NOT NULL
);

CREATE TABLE CS_A_Task_Synset (
    Id SERIAL NOT NULL PRIMARY KEY,
    TaskId INT NOT NULL,
    YarnId INT NOT NULL
);

ALTER TABLE CS_A_Pool ADD CONSTRAINT FK_CS_A_Pool_PredecessorId FOREIGN KEY (PredecessorId) REFERENCES CS_A_Pool (Id);
ALTER TABLE CS_A_Task ADD CONSTRAINT FK_CS_A_Task_PwnId FOREIGN KEY (PwnId) REFERENCES Synset (Id);
ALTER TABLE CS_A_Task ADD CONSTRAINT FK_CS_A_Task_PoolId FOREIGN KEY (PoolId) REFERENCES CS_A_Pool (Id);
ALTER TABLE CS_A_Task_Synset ADD CONSTRAINT FK_CS_A_Task_Synset_TaskId FOREIGN KEY (TaskId) REFERENCES CS_A_Task (Id);
ALTER TABLE CS_A_Task_Synset ADD CONSTRAINT FK_CS_A_Task_Synset_YarnId FOREIGN KEY (YarnId) REFERENCES Synset (Id);
ALTER TABLE CS_A_Task_Synset ADD CONSTRAINT UQ_CS_A_Task_Synset_TaskId_YarnId UNIQUE (TaskId, YarnId);

CREATE TABLE CS_A_Worker (
    Id SERIAL NOT NULL PRIMARY KEY,
    Source VARCHAR(20) NOT NULL,
    ExternalId VARCHAR(130) NOT NULL
);

CREATE TABLE CS_A_Answer (
    Id SERIAL NOT NULL PRIMARY KEY,
    WorkerId INT NOT NULL,
    AssignmentId VARCHAR(130) NOT NULL,
    TaskId INT NOT NULL,
    SelectedId INT,
    CreatedDate DATE NOT NULL
);

ALTER TABLE CS_A_Answer ADD CONSTRAINT FK_CS_A_Answer_SelectedId FOREIGN KEY (SelectedId) REFERENCES CS_A_Task_Synset (Id);
ALTER TABLE CS_A_Answer ADD CONSTRAINT FK_CS_A_Answer_TaskId FOREIGN KEY (TaskId) REFERENCES CS_A_Task (Id);
ALTER TABLE CS_A_Answer ADD CONSTRAINT FK_CS_A_Answer_WorkerId FOREIGN KEY (WorkerId) REFERENCES CS_A_Worker (Id);
CREATE INDEX IX_CS_A_Answer_AssignmentId ON CS_A_Answer (AssignmentId);
ALTER TABLE CS_A_Worker ADD CONSTRAINT UQ_CS_A_Worker_Source_ExternalId UNIQUE (Source, ExternalId);


CREATE TABLE Dict_Cache (
    Id SERIAL NOT NULL PRIMARY KEY,
    DictKey VARCHAR(255) NOT NULL,
    Word VARCHAR(255) NOT NULL,
    Data BYTEA NOT NULL
);
ALTER TABLE Dict_Cache ADD CONSTRAINT UQ_Dict_Cache_DictKey_Word UNIQUE (DictKey, Word);


CREATE TABLE CS_B_Answer (
    Id SERIAL NOT NULL PRIMARY KEY,
    Worker VARCHAR(127) NOT NULL,
    AssignmentId VARCHAR(130) NOT NULL,
    PwnId INT NOT NULL,
    CreatedDate DATE NOT NULL
);
CREATE TABLE CS_B_Answer_Selected (
    AnswerId INT NOT NULL,
    YarnId INT NOT NULL,
    Clean BOOLEAN NOT NULL,
    PRIMARY KEY (AnswerId, YarnId)
);
ALTER TABLE CS_B_Answer ADD CONSTRAINT FK_CS_B_Answer_PwnId FOREIGN KEY (PwnId) REFERENCES Synset (Id);
CREATE INDEX IX_CS_B_Answer_AssignmentId ON CS_B_Answer (AssignmentId);
ALTER TABLE CS_B_Answer_Selected ADD CONSTRAINT FK_CS_B_Answer_Selected_YarnId FOREIGN KEY (YarnId) REFERENCES Synset (Id);
ALTER TABLE CS_B_Answer_Selected ADD CONSTRAINT FK_CS_B_Answer_Selected_AnswerId FOREIGN KEY (AnswerId) REFERENCES CS_B_Answer (Id);

CREATE TABLE CS_A_Aggregation (
    Id SERIAL NOT NULL PRIMARY KEY,
    TaskId INT NOT NULL,
    Tag VARCHAR (31) NOT NULL,
    SelectedId INT,
    Weight FLOAT NOT NULL
);

ALTER TABLE CS_A_Aggregation ADD CONSTRAINT CH_CS_A_Aggregation_Weight CHECK (Weight >= 0 AND Weight <= 1);
ALTER TABLE CS_A_Aggregation ADD CONSTRAINT FK_CS_A_Aggregation_TaskId FOREIGN KEY (TaskId) REFERENCES CS_A_Task (Id);
ALTER TABLE CS_A_Aggregation ADD CONSTRAINT FK_CS_A_Aggregation_SelectedId FOREIGN KEY (SelectedId) REFERENCES CS_A_Task_Synset (Id);
ALTER TABLE CS_A_Aggregation ADD CONSTRAINT UQ_CS_A_Aggregation_TaskId_Tag_SelectedId UNIQUE (TaskId, Tag, SelectedId);

CREATE VIEW CS_A_B_COMPARE
AS
SELECT pwnId, yarnId,
      (CASE WHEN src = 1 THEN 'expert' WHEN src = 2 THEN 'aggr' WHEN src = 3 THEN 'both' ELSE 'none' END) as src,
      weight, clean, assignmentId, tag, poolId, taskId
FROM (
  SELECT COALESCE(ed.pwnId, ad.pwnId) as pwnId, COALESCE(ed.yarnId, ad.yarnId, 0) as yarnId,
         ed.Clean, ad.Weight, COALESCE(ad.src,0) + COALESCE(ed.src,0) as src,
         ed.assignmentId, ad.poolId, ad.tag, ad.taskId
  from (SELECT ba.pwnId, bas.yarnId, bas.clean, 1 as src, assignmentId from cs_b_answer ba left
        JOIN cs_b_answer_selected bas ON ba.Id = bas.answerId
        --where ba.assignmentId = '950530ce-20f3-449b-8d60-22f42dff8dcc'
       ) ed
  FULL OUTER JOIN (SELECT t.id as taskId, t.pwnId, ts.yarnId, aa.weight, 2 as src, poolId, tag
                   FROM cs_a_task t
                   JOIN cs_a_aggregation aa ON aa.taskId = t.Id
                   LEFT JOIN cs_a_task_synset ts ON ts.Id = aa.selectedId
                   -- WHERE t.poolId = 12 AND tag = 'zencrowd1'
                  ) ad ON COALESCE(ed.yarnId,0) = COALESCE(ad.yarnId,0) AND ed.pwnId = ad.pwnId
               ) t
ORDER BY pwnId, t.src DESC, yarnId;

