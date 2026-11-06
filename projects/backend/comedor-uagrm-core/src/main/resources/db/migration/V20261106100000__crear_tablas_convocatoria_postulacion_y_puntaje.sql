-- =====================================================================
-- Convocatoria, postulación y puntaje (CU06: lista priorizada).
-- Tablas: gestion_academica, convocatoria, postulacion,
--         modelo_preseleccion, puntaje_postulacion
-- Fuente: diseño lógico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Solo se crean estas cinco tablas; el resto de las de los paquetes P2 y P3
-- (respuesta_socioeconomica, documento_adjunto, parametro_modelo, etc.) las
-- crea el caso de uso que las necesita. Mismas convenciones que
-- V20261102100000: id interno, codigo_<tabla> público (UUID v7), columnas
-- comunes y unicidades como índices únicos parciales (borrado lógico).
-- =====================================================================

CREATE TYPE estado_convocatoria AS ENUM ('BORRADOR', 'ABIERTA', 'CERRADA');

CREATE TYPE estado_postulacion AS ENUM (
    'borrador', 'pendiente', 'devuelta', 'archivada', 'calificada', 'adjudicada', 'no_adjudicada'
);

CREATE TABLE gestion_academica (
    id                       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_gestion_academica uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    etiqueta                 varchar(50)  NOT NULL,
    fecha_inicio             date         NOT NULL,
    fecha_fin                date         NOT NULL,
    creado_en                timestamptz  NOT NULL DEFAULT now(),
    actualizado_en           timestamptz  NOT NULL DEFAULT now(),
    eliminado_en             timestamptz,
    CONSTRAINT ck_gestion_academica_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE UNIQUE INDEX uq_gestion_academica_etiqueta ON gestion_academica (etiqueta) WHERE eliminado_en IS NULL;

CREATE TABLE convocatoria (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_convocatoria uuid                NOT NULL DEFAULT uuidv7() UNIQUE,
    gestion_id          bigint              NOT NULL REFERENCES gestion_academica (id),
    creado_por          bigint              REFERENCES usuario (id),
    nombre              varchar(150)        NOT NULL,
    fecha_apertura      timestamptz         NOT NULL,
    fecha_cierre        timestamptz         NOT NULL,
    cupos               int,
    puntaje_minimo      numeric(5, 2),
    estado              estado_convocatoria NOT NULL,
    creado_en           timestamptz         NOT NULL DEFAULT now(),
    actualizado_en      timestamptz         NOT NULL DEFAULT now(),
    eliminado_en        timestamptz,
    CONSTRAINT ck_convocatoria_fechas CHECK (fecha_cierre > fecha_apertura),
    CONSTRAINT ck_convocatoria_cupos CHECK (cupos IS NULL OR cupos > 0),
    CONSTRAINT ck_convocatoria_puntaje_minimo CHECK (puntaje_minimo IS NULL OR puntaje_minimo BETWEEN 0 AND 100)
);

-- Una sola convocatoria abierta por gestión.
CREATE UNIQUE INDEX uq_convocatoria_abierta_por_gestion
    ON convocatoria (gestion_id) WHERE estado = 'ABIERTA' AND eliminado_en IS NULL;
CREATE INDEX ix_convocatoria_gestion_id ON convocatoria (gestion_id) WHERE eliminado_en IS NULL;

CREATE TABLE postulacion (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_postulacion uuid               NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id    bigint             NOT NULL REFERENCES convocatoria (id),
    postulante_id      bigint             NOT NULL REFERENCES postulante (usuario_id),
    estado             estado_postulacion NOT NULL,
    fecha_envio        timestamptz,
    numero_envios      smallint           NOT NULL DEFAULT 0,
    creado_en          timestamptz        NOT NULL DEFAULT now(),
    actualizado_en     timestamptz        NOT NULL DEFAULT now(),
    eliminado_en       timestamptz
);

-- Una postulación por postulante y convocatoria (idempotencia, RNF-03).
CREATE UNIQUE INDEX uq_postulacion_postulante_convocatoria
    ON postulacion (postulante_id, convocatoria_id) WHERE eliminado_en IS NULL;
-- Lista priorizada: postulaciones de una convocatoria por estado.
CREATE INDEX ix_postulacion_convocatoria_estado
    ON postulacion (convocatoria_id, estado) WHERE eliminado_en IS NULL;

CREATE TABLE modelo_preseleccion (
    id                         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_modelo_preseleccion uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    creado_por                 bigint       REFERENCES usuario (id),
    version                    varchar(50)  NOT NULL,
    descripcion                varchar(255),
    activo                     boolean      NOT NULL DEFAULT false,
    creado_en                  timestamptz  NOT NULL DEFAULT now(),
    actualizado_en             timestamptz  NOT NULL DEFAULT now(),
    eliminado_en               timestamptz
);

CREATE UNIQUE INDEX uq_modelo_preseleccion_version ON modelo_preseleccion (version) WHERE eliminado_en IS NULL;
-- Un solo modelo activo.
CREATE UNIQUE INDEX uq_modelo_preseleccion_activo
    ON modelo_preseleccion (activo) WHERE activo AND eliminado_en IS NULL;

CREATE TABLE puntaje_postulacion (
    id                         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_puntaje_postulacion uuid          NOT NULL DEFAULT uuidv7() UNIQUE,
    modelo_id                  bigint        NOT NULL REFERENCES modelo_preseleccion (id),
    postulacion_id             bigint        NOT NULL REFERENCES postulacion (id),
    puntaje_total              numeric(5, 2) NOT NULL,
    detalle                    jsonb,
    fecha_calculo              timestamptz   NOT NULL,
    vigente                    boolean       NOT NULL DEFAULT true,
    creado_en                  timestamptz   NOT NULL DEFAULT now(),
    actualizado_en             timestamptz   NOT NULL DEFAULT now(),
    eliminado_en               timestamptz,
    CONSTRAINT ck_puntaje_postulacion_total CHECK (puntaje_total BETWEEN 0 AND 100)
);

-- Un solo puntaje vigente por postulación; los anteriores quedan como historial (RF-03, RF-21).
CREATE UNIQUE INDEX uq_puntaje_postulacion_vigente
    ON puntaje_postulacion (postulacion_id) WHERE vigente AND eliminado_en IS NULL;
-- Lista priorizada rápida: puntajes vigentes de mayor a menor.
CREATE INDEX ix_puntaje_postulacion_vigente_total
    ON puntaje_postulacion (puntaje_total DESC) WHERE vigente AND eliminado_en IS NULL;
CREATE INDEX ix_puntaje_postulacion_modelo_id ON puntaje_postulacion (modelo_id) WHERE eliminado_en IS NULL;
