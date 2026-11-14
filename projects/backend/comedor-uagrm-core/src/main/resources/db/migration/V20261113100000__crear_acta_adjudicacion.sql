-- =====================================================================
-- Acta de adjudicacion (CU08)
-- Tabla nueva: acta_adjudicacion
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Una sola acta por convocatoria: es definitiva, no se regenera ni se elimina. `numero` (NNN/AAAA) es
-- unico en todo el sistema, incluso entre filas eliminadas, y no se reutiliza. El PDF vive en MinIO; la
-- base solo guarda la ruta (`storage_path`).
-- =====================================================================

CREATE TABLE acta_adjudicacion (
    id                       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_acta_adjudicacion uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id          bigint       NOT NULL REFERENCES convocatoria (id),
    numero                   varchar(30)  NOT NULL,
    storage_path             varchar(500) NOT NULL,
    generada_por             bigint       NOT NULL REFERENCES usuario (id),
    generada_en              timestamptz  NOT NULL DEFAULT now(),
    creado_en                timestamptz  NOT NULL DEFAULT now(),
    actualizado_en           timestamptz  NOT NULL DEFAULT now(),
    eliminado_en             timestamptz,
    CONSTRAINT uq_acta_adjudicacion_numero UNIQUE (numero)
);

-- Una sola acta por convocatoria (la segunda red de seguridad tras el bloqueo de la fila).
CREATE UNIQUE INDEX uq_acta_adjudicacion_convocatoria
    ON acta_adjudicacion (convocatoria_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_acta_adjudicacion_generada_por
    ON acta_adjudicacion (generada_por) WHERE eliminado_en IS NULL;
