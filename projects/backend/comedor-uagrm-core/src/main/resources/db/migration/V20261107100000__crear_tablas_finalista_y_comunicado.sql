-- =====================================================================
-- Finalistas y comunicados (CU21: notificar finalistas, registrar la
-- entrega de documentacion fisica y publicar comunicados).
-- Tablas: finalista, comunicado (+ enum destinatarios_comunicado)
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- OJO: solo se crean estas dos tablas. auditoria_expediente, adjudicacion
-- y el resto de P4 los crean sus propios CU; el CU05 (que elige a los
-- finalistas) AMPLIA `finalista` sin recrearla.
-- notificacion gana postulacion_id y comunicado_id (las dos FK que el
-- diagrama ya traia y que la migracion de CU-EXTRA-01 dejo pendientes).
-- Mismas convenciones que V20261102100000: id interno, codigo_<tabla>
-- publico (UUID v7), columnas comunes y unicidad como indice parcial.
-- =====================================================================

CREATE TYPE destinatarios_comunicado AS ENUM ('TODOS', 'FINALISTAS', 'ADJUDICADOS');

CREATE TABLE finalista (
    id                        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_finalista          uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    postulacion_id            bigint      NOT NULL REFERENCES postulacion (id),
    posicion                  int         NOT NULL,
    plazo_entrega             date,
    notificado_en             timestamptz,
    documentacion_recibida_en timestamptz,
    creado_en                 timestamptz NOT NULL DEFAULT now(),
    actualizado_en            timestamptz NOT NULL DEFAULT now(),
    eliminado_en              timestamptz,
    CONSTRAINT ck_finalista_posicion CHECK (posicion > 0)
);

-- Una postulacion es finalista una sola vez.
CREATE UNIQUE INDEX uq_finalista_postulacion ON finalista (postulacion_id) WHERE eliminado_en IS NULL;

CREATE TABLE comunicado (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_comunicado  uuid                     NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id    bigint                   REFERENCES convocatoria (id),
    publicado_por      bigint                   NOT NULL REFERENCES usuario (id),
    titulo             varchar(150)             NOT NULL,
    contenido          text                     NOT NULL,
    destinatarios      destinatarios_comunicado NOT NULL,
    publicado_en       timestamptz              NOT NULL DEFAULT now(),
    creado_en          timestamptz              NOT NULL DEFAULT now(),
    actualizado_en     timestamptz              NOT NULL DEFAULT now(),
    eliminado_en       timestamptz
);

CREATE INDEX ix_comunicado_publicado_en ON comunicado (publicado_en DESC) WHERE eliminado_en IS NULL;
CREATE INDEX ix_comunicado_convocatoria_id ON comunicado (convocatoria_id) WHERE eliminado_en IS NULL;

ALTER TABLE notificacion
    ADD COLUMN postulacion_id bigint REFERENCES postulacion (id),
    ADD COLUMN comunicado_id  bigint REFERENCES comunicado (id);

-- Ultimo aviso de una postulacion (estado de la notificacion del finalista) y avisos de un comunicado.
CREATE INDEX ix_notificacion_postulacion_id ON notificacion (postulacion_id, creado_en DESC)
    WHERE postulacion_id IS NOT NULL AND eliminado_en IS NULL;
CREATE INDEX ix_notificacion_comunicado_id ON notificacion (comunicado_id)
    WHERE comunicado_id IS NOT NULL AND eliminado_en IS NULL;
