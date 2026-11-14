-- =====================================================================
-- Publicacion de resultados de adjudicacion (CU22)
-- Tabla nueva: publicacion_resultado
-- Valor nuevo del enum estado_convocatoria: PUBLICADA
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Una fila por convocatoria: es la marca de que sus resultados son oficiales. La fila y el cambio de
-- estado de la convocatoria a PUBLICADA se guardan en la misma transaccion (el valor nuevo no se usa
-- en esta migracion, solo se agrega).
-- =====================================================================

ALTER TYPE estado_convocatoria ADD VALUE IF NOT EXISTS 'PUBLICADA';

CREATE TABLE publicacion_resultado (
    id                           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publicacion_resultado uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id              bigint      NOT NULL REFERENCES convocatoria (id),
    publicado_por                bigint      NOT NULL REFERENCES usuario (id),
    publicado_en                 timestamptz NOT NULL DEFAULT now(),
    creado_en                    timestamptz NOT NULL DEFAULT now(),
    actualizado_en               timestamptz NOT NULL DEFAULT now(),
    eliminado_en                 timestamptz
);

-- Una sola publicacion por convocatoria (la segunda red de seguridad tras el bloqueo de la fila).
CREATE UNIQUE INDEX uq_publicacion_resultado_convocatoria
    ON publicacion_resultado (convocatoria_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_publicacion_resultado_publicado_por
    ON publicacion_resultado (publicado_por) WHERE eliminado_en IS NULL;
