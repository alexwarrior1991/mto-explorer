-- Copia de lectura de los dos maestros. Cada carga borra y vuelve a escribir: el Excel manda.
-- Cada tabla con su secuencia <tabla>_seq de INCREMENT BY 50, que es lo que espera PanacheEntity.

CREATE SEQUENCE execution_package_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE execution_package
(
    id              BIGINT PRIMARY KEY,
    code            VARCHAR(40)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    initial_package BOOLEAN      NOT NULL,
    length_m        INTEGER,
    start_date      DATE,
    end_date        DATE,
    company_id      VARCHAR(40),
    enabled         BOOLEAN      NOT NULL
);

CREATE SEQUENCE station_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE station
(
    id   BIGINT PRIMARY KEY,
    ep   VARCHAR(40)  NOT NULL,
    name VARCHAR(200) NOT NULL
);
CREATE INDEX station_ep_idx ON station (ep);

CREATE SEQUENCE track_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE track
(
    id           BIGINT PRIMARY KEY,
    ep           VARCHAR(40)  NOT NULL,
    name         VARCHAR(200) NOT NULL,
    stations     VARCHAR(400),
    enabled      BOOLEAN      NOT NULL,
    source_sheet VARCHAR(200)
);
CREATE INDEX track_ep_idx ON track (ep);

-- PROFILE_ID se repite dentro de una vía: lo que identifica un perfil es (ep, track, sort_order).
CREATE SEQUENCE profile_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE profile
(
    id                        BIGINT PRIMARY KEY,
    ep                        VARCHAR(40)  NOT NULL,
    track                     VARCHAR(200) NOT NULL,
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
    source_row                INTEGER
);
CREATE INDEX profile_track_kp_idx ON profile (ep, track, kp);
CREATE INDEX profile_track_order_idx ON profile (ep, track, sort_order);

CREATE SEQUENCE cantilever_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE cantilever
(
    id                BIGINT PRIMARY KEY,
    ep                VARCHAR(40)  NOT NULL,
    track             VARCHAR(200) NOT NULL,
    profile_id        VARCHAR(80)  NOT NULL,
    profile_order     INTEGER      NOT NULL,
    slot              INTEGER      NOT NULL,
    type              VARCHAR(200),
    stagger           DOUBLE PRECISION,
    catenary_height   DOUBLE PRECISION,
    cw_elevation      DOUBLE PRECISION,
    cw_height         DOUBLE PRECISION,
    wind_deflection   DOUBLE PRECISION,
    arm_angle         DOUBLE PRECISION,
    steady_arm_type   VARCHAR(200),
    steady_arm_length DOUBLE PRECISION,
    enabled           BOOLEAN      NOT NULL,
    to_review         BOOLEAN      NOT NULL,
    source_row        INTEGER
);
CREATE INDEX cantilever_profile_idx ON cantilever (ep, track, profile_order);

-- Seccionador. Clave natural: (ep, name). La estación es opcional ("SIN ESTACION").
CREATE SEQUENCE disconnector_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE disconnector
(
    id              BIGINT PRIMARY KEY,
    ep              VARCHAR(40)  NOT NULL,
    station         VARCHAR(200),
    track           VARCHAR(200),
    connected_track VARCHAR(200),
    profile_id      VARCHAR(80),
    pole_kp         DOUBLE PRECISION,
    name            VARCHAR(200) NOT NULL,
    kp              DOUBLE PRECISION,
    on_load         BOOLEAN      NOT NULL,
    normally_open   BOOLEAN,
    drive_type      VARCHAR(40),
    function        VARCHAR(100),
    enabled         BOOLEAN      NOT NULL
);
CREATE INDEX disconnector_ep_idx ON disconnector (ep, station, name);

-- Aislador de sección. Clave natural: (ep, station, name): el mismo nombre se repite en estaciones distintas.
CREATE SEQUENCE section_insulator_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE section_insulator
(
    id                BIGINT PRIMARY KEY,
    ep                VARCHAR(40)  NOT NULL,
    station           VARCHAR(200) NOT NULL,
    name              VARCHAR(200) NOT NULL,
    kp                DOUBLE PRECISION,
    installation_type VARCHAR(40),
    track             VARCHAR(200),
    connected_track   VARCHAR(200),
    enabled           BOOLEAN      NOT NULL
);
CREATE INDEX section_insulator_ep_idx ON section_insulator (ep, station, name);

-- Una aguja de un aislador: cuelga de él por (ep, station, insulator).
CREATE SEQUENCE section_insulator_switch_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE section_insulator_switch
(
    id        BIGINT PRIMARY KEY,
    ep        VARCHAR(40)  NOT NULL,
    station   VARCHAR(200) NOT NULL,
    insulator VARCHAR(200) NOT NULL,
    code      VARCHAR(40)  NOT NULL,
    kp        DOUBLE PRECISION,
    tangent   INTEGER,
    track     VARCHAR(200),
    enabled   BOOLEAN      NOT NULL
);
CREATE INDEX section_insulator_switch_idx ON section_insulator_switch (ep, station, insulator);

CREATE SEQUENCE lov_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE lov
(
    id             BIGINT PRIMARY KEY,
    entity         VARCHAR(100) NOT NULL,
    code           VARCHAR(200) NOT NULL,
    description_en VARCHAR(500),
    description_es VARCHAR(500),
    type           VARCHAR(200),
    plan_number    INTEGER,
    enabled        BOOLEAN      NOT NULL,
    origin         VARCHAR(100),
    boq_categories VARCHAR(1000),
    eps            VARCHAR(1000),
    uses_in_tracks INTEGER,
    to_review      BOOLEAN      NOT NULL
);
CREATE INDEX lov_entity_idx ON lov (entity);

-- Hoja NO_RECONOCIDO de los dos maestros: lo que el generador no supo mapear.
CREATE SEQUENCE unrecognized_value_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE unrecognized_value
(
    id          BIGINT PRIMARY KEY,
    master      VARCHAR(20)  NOT NULL,
    kind        VARCHAR(200) NOT NULL,
    value       VARCHAR(1000),
    ep          VARCHAR(40),
    sheet       VARCHAR(200),
    first_row   INTEGER,
    occurrences INTEGER
);

-- Una fila por fichero cargado: permite saber si el Excel ha cambiado desde la última carga.
CREATE SEQUENCE master_load_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE master_load
(
    id            BIGINT PRIMARY KEY,
    file_name     VARCHAR(200)             NOT NULL,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL,
    size_bytes    BIGINT                   NOT NULL,
    loaded_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    row_count     INTEGER                  NOT NULL,
    duration_ms   BIGINT                   NOT NULL
);
CREATE INDEX master_load_file_idx ON master_load (file_name, loaded_at DESC);