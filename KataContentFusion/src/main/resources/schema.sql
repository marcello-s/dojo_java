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

-- THEMOVIEDATABASE.ORG
-- Movie
CREATE TABLE IF NOT EXISTS 'Movie'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ExternalId' INTEGER,
    'Title' TEXT(50),
    'Adult' BOOLEAN,
    'Budget' INTEGER,
    'ImdbId' TEXT(25),
    'OriginalLanguage' TEXT(10),
    'OriginalTitle' TEXT(50),
    'Overview' TEXT(1000),
    'Popularity' NUMERIC,
    'ReleaseDate' NUMERIC,
    'Revenue' INTEGER,
    'Runtime' INTEGER,
    'Status' TEXT(15),
    'Tagline' TEXT(100),
    'VoteAverage' NUMERIC,
    'VoteCount' INTEGER
);

-- Genre
CREATE TABLE IF NOT EXISTS 'Genre'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ExternalId' INTEGER,
    'Name' TEXT(50)
);

-- ProductionCompany
CREATE TABLE IF NOT EXISTS 'ProductionCompany'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ExternalId' INTEGER,
    'Name' TEXT(50),
    'LogoPath' TEXT(50),
    'OriginCountry' TEXT(50)
);

-- ProductionCountry
CREATE TABLE IF NOT EXISTS 'ProductionCountry'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'Name' TEXT(50),
    'Iso3166_1' TEXT(50)
);

-- Role
CREATE TABLE IF NOT EXISTS 'Role'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'Name' TEXT(50)
);

-- Cast
CREATE TABLE IF NOT EXISTS 'Cast'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ExternalId' INTEGER,
    'Name' TEXT(50),
    'OriginalName' TEXT(50),
    'Adult' BOOLEAN,
    'Gender' INTEGER,
    'RoleId' INTEGER,
    'Popularity' NUMERIC,
    'ProfilePath' TEXT(50),
    'CastId' INTEGER,
    'Character' TEXT(20),
    'CreditId' TEXT(10),
    'OrderNo' INTEGER,
    FOREIGN KEY(RoleId) REFERENCES Role(Id)
);

-- Person
CREATE TABLE IF NOT EXISTS 'Person'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ExternalId' INTEGER,
    'Name' TEXT(50),
    'Adult' BOOLEAN,
    'Biography' TEXT(5000),
    'Birthday' NUMERIC NULL,
    'Deathday' NUMERIC NULL,
    'Gender' INTEGER,
    'Homepage' TEXT(100),
    'ImdbId' TEXT(25),
    'RoleId' INTEGER,
    'PlaceOfBirth' TEXT(100),
    'Popularity' NUMERIC,
    'ProfilePath' TEXT(100),
    FOREIGN KEY(RoleId) REFERENCES Role(Id)
);

-- MovieGenre relation
CREATE TABLE IF NOT EXISTS 'MovieGenre'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'MovieId' INTEGER,
    'GenreId' INTEGER,
    FOREIGN KEY(MovieId) REFERENCES Movie(Id),
    FOREIGN KEY(GenreId) REFERENCES Genre(Id)
);

-- MovieProductionCompany relation
CREATE TABLE IF NOT EXISTS 'MovieProductionCompany'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'MovieId' INTEGER,
    'ProductionCompanyId' INTEGER,
    FOREIGN KEY(MovieId) REFERENCES Movie(Id),
    FOREIGN KEY(ProductionCompanyId) REFERENCES ProductionCompany(Id)
);

-- MovieProductionCountry relation
CREATE TABLE IF NOT EXISTS 'MovieProductionCountry'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'MovieId' INTEGER,
    'ProductionCountryId' INTEGER,
    FOREIGN KEY(MovieId) REFERENCES Movie(Id),
    FOREIGN KEY(ProductionCountryId) REFERENCES ProductionCountry(Id)
);

-- MovieCast relation
CREATE TABLE IF NOT EXISTS 'MovieCast'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'MovieId' INTEGER,
    'CastId' INTEGER,
    FOREIGN KEY(MovieId) REFERENCES Movie(Id),
    FOREIGN KEY(CastId) REFERENCES Cast(Id)
);

-- ScanNameMovie relation
CREATE TABLE IF NOT EXISTS 'ScanNameMovie'
(
    'Id' INTEGER PRIMARY KEY ASC,
    'ScanNameId' INTEGER,
    'MovieId' INTEGER,
    FOREIGN KEY(ScanNameId) REFERENCES ScanName(Id),
    FOREIGN KEY(MovieId) REFERENCES Movie(Id)
);

BEGIN TRANSACTION;
    INSERT OR REPLACE INTO 'ScriptTracking' (Id, ScriptName, CreatedAt) 
    VALUES (3,'02_MOVIE_STORE.sql', CURRENT_TIMESTAMP);
COMMIT TRANSACTION;
