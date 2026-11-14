-- =====================================================================
-- Auditoria del expediente de un finalista (CU07) y las tablas de apoyo
-- que el expediente lee.
-- Tablas: auditoria_expediente (+ enum resultado_auditoria), tipo_documento,
--         convocatoria_documento, documento_adjunto, respuesta_socioeconomica
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- OJO: las cuatro tablas de apoyo las AMPLIAN despues los CU02 y CU16 (otro
-- integrante) sin recrearlas; aqui solo estan para poder probar el CU07.
-- `auditoria_expediente` lleva dos decisiones de este CU que el diseno
-- logico todavia no tenia: la columna `puntaje_academico` (0 a 30, lo escribe
-- el auditor) y `observaciones` acotado a 500 caracteres.
-- Mismas convenciones que V20261102100000: id interno, codigo_<tabla>
-- publico (UUID v7), columnas comunes y unicidad como indice parcial.
-- =====================================================================

CREATE TYPE resultado_auditoria AS ENUM ('VALIDADO', 'OBSERVADO', 'NO_VALIDO');

CREATE TABLE tipo_documento (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_tipo_documento uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    nombre                varchar(100) NOT NULL,
    descripcion           varchar(255),
    creado_en             timestamptz  NOT NULL DEFAULT now(),
    actualizado_en        timestamptz  NOT NULL DEFAULT now(),
    eliminado_en          timestamptz
);

CREATE UNIQUE INDEX uq_tipo_documento_nombre ON tipo_documento (nombre) WHERE eliminado_en IS NULL;

-- Que documentos exige cada convocatoria. PK compuesta (no tiene `id` propio).
CREATE TABLE convocatoria_documento (
    convocatoria_id               bigint      NOT NULL REFERENCES convocatoria (id),
    tipo_documento_id             bigint      NOT NULL REFERENCES tipo_documento (id),
    codigo_convocatoria_documento uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    obligatorio                   boolean     NOT NULL DEFAULT true,
    creado_en                     timestamptz NOT NULL DEFAULT now(),
    actualizado_en                timestamptz NOT NULL DEFAULT now(),
    eliminado_en                  timestamptz,
    PRIMARY KEY (convocatoria_id, tipo_documento_id)
);

CREATE INDEX ix_convocatoria_documento_tipo_documento_id
    ON convocatoria_documento (tipo_documento_id) WHERE eliminado_en IS NULL;

-- Lo que el postulante declaro. PK = FK a la postulacion (no tiene `id` propio).
CREATE TABLE respuesta_socioeconomica (
    postulacion_id                  bigint      PRIMARY KEY REFERENCES postulacion (id),
    codigo_respuesta_socioeconomica uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    ingresos_familiares             numeric(12, 2),
    dependientes                    smallint,
    patrimonio                      varchar(255),
    procedencia                     varchar(150),
    promedio_academico              numeric(5, 2),
    estado_vulnerabilidad           varchar(50),
    datos_extra                     jsonb,
    creado_en                       timestamptz NOT NULL DEFAULT now(),
    actualizado_en                  timestamptz NOT NULL DEFAULT now(),
    eliminado_en                    timestamptz
);

-- Los documentos digitales: el binario vive en MinIO, aqui solo su ruta, tipo y tamano.
CREATE TABLE documento_adjunto (
    id                       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_documento_adjunto uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    postulacion_id           bigint       NOT NULL REFERENCES postulacion (id),
    tipo_documento_id        bigint       NOT NULL REFERENCES tipo_documento (id),
    nombre_archivo           varchar(255) NOT NULL,
    mime_type                varchar(100) NOT NULL,
    storage_path             varchar(500) NOT NULL,
    tamano_bytes             bigint       NOT NULL,
    subido_en                timestamptz  NOT NULL DEFAULT now(),
    creado_en                timestamptz  NOT NULL DEFAULT now(),
    actualizado_en           timestamptz  NOT NULL DEFAULT now(),
    eliminado_en             timestamptz,
    CONSTRAINT ck_documento_adjunto_tamano CHECK (tamano_bytes >= 0)
);

CREATE INDEX ix_documento_adjunto_postulacion_id ON documento_adjunto (postulacion_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_documento_adjunto_tipo_documento_id
    ON documento_adjunto (tipo_documento_id) WHERE eliminado_en IS NULL;

-- Append-only: una auditoria no se edita ni se borra; corregir un resultado es registrar otra.
CREATE TABLE auditoria_expediente (
    id                         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_auditoria_expediente uuid               NOT NULL DEFAULT uuidv7() UNIQUE,
    finalista_id               bigint              NOT NULL REFERENCES finalista (id),
    auditor_id                 bigint              NOT NULL REFERENCES usuario (id),
    fecha_auditoria            timestamptz         NOT NULL DEFAULT now(),
    resultado                  resultado_auditoria NOT NULL,
    observaciones              varchar(500),
    puntaje_academico          numeric(5, 2),
    creado_en                  timestamptz         NOT NULL DEFAULT now(),
    actualizado_en             timestamptz         NOT NULL DEFAULT now(),
    eliminado_en               timestamptz,
    CONSTRAINT ck_auditoria_expediente_puntaje_academico CHECK (puntaje_academico BETWEEN 0 AND 30)
);

-- La auditoria vigente de un finalista es la mas reciente.
CREATE INDEX ix_auditoria_expediente_finalista_fecha
    ON auditoria_expediente (finalista_id, fecha_auditoria DESC) WHERE eliminado_en IS NULL;
CREATE INDEX ix_auditoria_expediente_auditor_id ON auditoria_expediente (auditor_id) WHERE eliminado_en IS NULL;
