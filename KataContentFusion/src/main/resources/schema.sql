-- applied on every program startup
-- script with 'if not exists' idempotency

-- Create script tracking
-- $ sqlite3 contentfusion.sqlt < script.sql
-- $ cmd /c "sqlite3 contentfusion.sqlt < script.sql"
-- $ Get-Content script.sql -Encoding UTF 8 | sqlite3 contentfusion.sqlt

-- SELECT * FROM sqlite_schema WHERE type='table'

CREATE TABLE IF NOT EXISTS 'ScriptTracking' 
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ScriptName' TEXT(100),
    'CreatedAt' NUMERIC
);

BEGIN TRANSACTION;
    INSERT OR REPLACE INTO 'ScriptTracking' (Id, ScriptName, CreatedAt) 
    VALUES (1,'00_INIT.sql', CURRENT_TIMESTAMP);
COMMIT TRANSACTION;


-- asset store
-- Volume
CREATE TABLE IF NOT EXISTS 'Volume'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'Name' TEXT(10)
);

CREATE TABLE IF NOT EXISTS 'ScanName'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'Name' TEXT(50),
    'Collection' TEXT(20) NULL
);

-- Movie / TV-Show
CREATE TABLE IF NOT EXISTS 'ScanType'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'Name' TEXT(10)
);

-- Video / Subtitle / Image
CREATE TABLE IF NOT EXISTS 'AssetType'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'Name' TEXT(10)
);

-- Asset
PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS 'Asset'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ScanTypeId' INTEGER,
    'ScanNameId' INTEGER,
    'AssetTypeId' INTEGER,
    'MediaPath' TEXT(250),
    'ResolutionX' INTEGER,
    'ResolutionY' INTEGER,
    FOREIGN KEY(ScanTypeId) REFERENCES ScanType(Id),
    FOREIGN KEY(ScanNameId) REFERENCES ScanName(Id),
    FOREIGN KEY(AssetTypeId) REFERENCES AssetType(Id)
);

-- SubtitleEntry
CREATE TABLE IF NOT EXISTS 'SubtitleEntry'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'AssetId' INTEGER,
    'SequenceNumber' INTEGER,
    'TimeFrom' INTEGER,
    'TimeTo' INTEGER,
    'Text' TEXT(50),
    FOREIGN KEY(AssetId) REFERENCES Asset(Id)
);

-- AssetVolume relation
CREATE TABLE IF NOT EXISTS 'AssetVolume'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'AssetId' INTEGER,
    'VolumeId' INTEGER,
    FOREIGN KEY(AssetId) REFERENCES Asset(Id),
    FOREIGN KEY(VolumeId) REFERENCES Volume(Id)
);

BEGIN TRANSACTION;
    INSERT OR REPLACE INTO 'ScriptTracking' (Id, ScriptName, CreatedAt) 
    VALUES (2,'01_ASSET_STORE.sql', CURRENT_TIMESTAMP);
COMMIT TRANSACTION;
