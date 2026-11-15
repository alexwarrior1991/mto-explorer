-- Las tablas del maestro de infraestructura dejan las claves naturales en texto (ep, track,
-- station…) y pasan a relacionarse por claves foráneas. Los datos son una copia del Excel, así
-- que se borran y se crean de nuevo; se vuelven a importar al arrancar.
--
--   execution_package ─┬─< station ──┐
--                      │             ├──< track_station >── track   (@ManyToMany, ordenada)
--                      └─< track ────┘
--   track ─< profile ─< cantilever
--   profile ─< disconnector (su poste, opcional)
--   station ─< section_insulator ─< section_insulator_switch

DROP TABLE section_insulator_switch;
DROP TABLE section_insulator;
DROP TABLE disconnector;
DROP TABLE cantilever;
DROP TABLE profile;
DROP TABLE track;
DROP TABLE station;

CREATE TABLE station (
    id                   BIGINT PRIMARY KEY,
    execution_package_id BIGINT       NOT NULL REFERENCES execution_package (id),
    name                 VARCHAR(200) NOT NULL,
    CONSTRAINT station_uq UNIQUE (execution_package_id, name)
);

CREATE TABLE track (
    id                   BIGINT PRIMARY KEY,
    execution_package_id BIGINT       NOT NULL REFERENCES execution_package (id),
    name                 VARCHAR(200) NOT NULL,
    enabled              BOOLEAN      NOT NULL,
    source_sheet         VARCHAR(200),
    CONSTRAINT track_uq UNIQUE (execution_package_id, name)
);

-- La tabla intermedia del @ManyToMany. station_order es la @OrderColumn: la posición de la estación
-- en la vía. Al borrar una vía o una estación se borran sus filas aquí.
CREATE TABLE track_station (
    track_id      BIGINT  NOT NULL REFERENCES track (id) ON DELETE CASCADE,
    station_id    BIGINT  NOT NULL REFERENCES station (id) ON DELETE CASCADE,
    station_order INTEGER NOT NULL,
    PRIMARY KEY (track_id, station_order)
);
CREATE INDEX track_station_station_idx ON track_station (station_id);

CREATE TABLE profile (
    id                        BIGINT PRIMARY KEY,
    track_id                  BIGINT       NOT NULL REFERENCES track (id),
    profile_id                VARCHAR(80)  NOT NULL,
    kp                        DOUBLE PRECISION,
    sort_order                INTEGER      NOT NULL,
    status                    VARCHAR(40),
    sectioning                VARCHAR(200),
    anchorage                 VARCHAR(200),
    anchorage_foundation      VARCHAR(200),
    foundation                VARCHAR(200),
    pole_type                 VARCHAR(200),
    portal                    VARCHAR(200),
    return_support            VARCHAR(200),
    sectioning_feeding        VARCHAR(200),
    support_type              VARCHAR(200),
    assembly_configuration    VARCHAR(200),
    span                      DOUBLE PRECISION,
    height_cantilever_support DOUBLE PRECISION,
    pole_gauge_location       DOUBLE PRECISION,
    rail_pole_distance        DOUBLE PRECISION,
    enabled                   BOOLEAN      NOT NULL,
    to_review                 BOOLEAN      NOT NULL,
    source_sheet              VARCHAR(200),
    source_row                INTEGER,
    CONSTRAINT profile_uq UNIQUE (track_id, sort_order)
);
CREATE INDEX profile_track_kp_idx ON profile (track_id, kp);

CREATE TABLE cantilever (
    id                BIGINT PRIMARY KEY,
    profile_id        BIGINT  NOT NULL REFERENCES profile (id),
    slot              INTEGER NOT NULL,
    type              VARCHAR(200),
    stagger           DOUBLE PRECISION,
    catenary_height   DOUBLE PRECISION,
    cw_elevation      DOUBLE PRECISION,
    cw_height         DOUBLE PRECISION,
    wind_deflection   DOUBLE PRECISION,
    arm_angle         DOUBLE PRECISION,
    steady_arm_type   VARCHAR(200),
    steady_arm_length DOUBLE PRECISION,
    enabled           BOOLEAN NOT NULL,
    to_review         BOOLEAN NOT NULL,
    source_row        INTEGER,
    CONSTRAINT cantilever_uq UNIQUE (profile_id, slot)
);

CREATE TABLE disconnector (
    id                   BIGINT PRIMARY KEY,
    execution_package_id BIGINT       NOT NULL REFERENCES execution_package (id),
    station_id           BIGINT REFERENCES station (id),
    track_id             BIGINT REFERENCES track (id),
    connected_track_id   BIGINT REFERENCES track (id),
    pole_id              BIGINT REFERENCES profile (id),
    name                 VARCHAR(200) NOT NULL,
    kp                   DOUBLE PRECISION,
    on_load              BOOLEAN      NOT NULL,
    normally_open        BOOLEAN,
    drive_type           VARCHAR(40),
    function             VARCHAR(100),
    enabled              BOOLEAN      NOT NULL,
    CONSTRAINT disconnector_uq UNIQUE (execution_package_id, name)
);
CREATE INDEX disconnector_pole_idx ON disconnector (pole_id);

CREATE TABLE section_insulator (
    id                 BIGINT PRIMARY KEY,
    station_id         BIGINT       NOT NULL REFERENCES station (id),
    name               VARCHAR(200) NOT NULL,
    kp                 DOUBLE PRECISION,
    installation_type  VARCHAR(40),
    track_id           BIGINT REFERENCES track (id),
    connected_track_id BIGINT REFERENCES track (id),
    enabled            BOOLEAN      NOT NULL,
    CONSTRAINT section_insulator_uq UNIQUE (station_id, name)
);

CREATE TABLE section_insulator_switch (
    id           BIGINT PRIMARY KEY,
    insulator_id BIGINT      NOT NULL REFERENCES section_insulator (id),
    code         VARCHAR(40) NOT NULL,
    kp           DOUBLE PRECISION,
    tangent      INTEGER,
    track_id     BIGINT REFERENCES track (id),
    enabled      BOOLEAN     NOT NULL,
    CONSTRAINT section_insulator_switch_uq UNIQUE (insulator_id, code)
);

DELETE FROM master_load WHERE file_name = 'profile-master.xlsx';
