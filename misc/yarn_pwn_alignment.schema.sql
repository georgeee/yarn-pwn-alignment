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
ALTER TABLE Synset ADD CONSTRAINT UQ_Synset_ExternalId UNIQUE (ExternalId);
