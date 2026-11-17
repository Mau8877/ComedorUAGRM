-- =====================================================================
-- Padron de becados (CU23)
-- Tablas nuevas: beca, movimiento_beca (+ enums estado_beca y tipo_movimiento_beca)
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Una beca por postulante y convocatoria. El padron vigente es `beca WHERE estado = 'VIGENTE'`.
-- `adjudicacion_id` es NULL en una beca de reemplazo: el reemplazante no tiene adjudicacion propia
-- (decision pendiente de confirmar con la DUBSS, ver CU23). Es unico cuando existe.
-- `fecha_fin` queda NULL: ningun CU define de donde sale.
-- `movimiento_beca` es append-only: la aplicacion solo inserta. El movimiento REEMPLAZO vincula la beca
-- liberada (`beca_id`) con la beca que ocupa el cupo (`beca_reemplazante_id`).
-- =====================================================================

CREATE TYPE estado_beca AS ENUM ('VIGENTE', 'SUSPENDIDA', 'BAJA');
CREATE TYPE tipo_movimiento_beca AS ENUM ('BAJA', 'SUSPENSION', 'REACTIVACION', 'REEMPLAZO');

CREATE TABLE beca (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_beca     uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id bigint      NOT NULL REFERENCES convocatoria (id),
    postulante_id   bigint      NOT NULL REFERENCES postulante (usuario_id),
    adjudicacion_id bigint      REFERENCES adjudicacion (id),
    estado          estado_beca NOT NULL DEFAULT 'VIGENTE',
    fecha_inicio    date        NOT NULL,
    fecha_fin       date,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    eliminado_en    timestamptz,
    CONSTRAINT ck_beca_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- Un postulante tiene una sola beca por convocatoria, y una adjudicacion origina a lo sumo una beca
-- (idempotencia: RNF-03; repetir el alta no duplica).
CREATE UNIQUE INDEX uq_beca_postulante_convocatoria
    ON beca (postulante_id, convocatoria_id) WHERE eliminado_en IS NULL;
CREATE UNIQUE INDEX uq_beca_adjudicacion
    ON beca (adjudicacion_id) WHERE adjudicacion_id IS NOT NULL AND eliminado_en IS NULL;
CREATE INDEX ix_beca_convocatoria_estado
    ON beca (convocatoria_id, estado) WHERE eliminado_en IS NULL;

CREATE TABLE movimiento_beca (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_movimiento_beca uuid      NOT NULL DEFAULT uuidv7() UNIQUE,
    beca_id              bigint      NOT NULL REFERENCES beca (id),
    beca_reemplazante_id bigint      REFERENCES beca (id),
    registrado_por       bigint      NOT NULL REFERENCES usuario (id),
    tipo                 tipo_movimiento_beca NOT NULL,
    fecha                date        NOT NULL,
    motivo               text        NOT NULL,
    creado_en            timestamptz NOT NULL DEFAULT now(),
    actualizado_en       timestamptz NOT NULL DEFAULT now(),
    eliminado_en         timestamptz,
    -- La beca reemplazante solo existe en un REEMPLAZO.
    CONSTRAINT ck_movimiento_beca_reemplazante
        CHECK ((tipo = 'REEMPLAZO') = (beca_reemplazante_id IS NOT NULL))
);

-- Una beca liberada se reemplaza una sola vez.
CREATE UNIQUE INDEX uq_movimiento_beca_reemplazo
    ON movimiento_beca (beca_id) WHERE tipo = 'REEMPLAZO' AND eliminado_en IS NULL;
CREATE INDEX ix_movimiento_beca_beca
    ON movimiento_beca (beca_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_movimiento_beca_registrado_por
    ON movimiento_beca (registrado_por) WHERE eliminado_en IS NULL;
