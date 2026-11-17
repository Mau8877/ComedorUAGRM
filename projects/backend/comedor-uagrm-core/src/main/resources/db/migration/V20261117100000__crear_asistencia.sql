-- =====================================================================
-- Asistencia digital al comedor (CU10)
-- Tabla nueva: asistencia
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Una fila por beca y dia: el indice unico parcial es el contrato de idempotencia (RNF-03). Registrar dos
-- veces la asistencia de una beca el mismo dia no crea otra fila (el servicio inserta con ON CONFLICT DO
-- NOTHING sobre ese mismo indice).
-- `fecha` es la fecha de calendario en America/La_Paz; `hora_registro` es el instante en que se registro.
-- Las filas no se editan ni se eliminan desde la aplicacion.
-- =====================================================================

CREATE TABLE asistencia (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_asistencia uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    beca_id           bigint      NOT NULL REFERENCES beca (id),
    registrado_por    bigint      NOT NULL REFERENCES usuario (id),
    fecha             date        NOT NULL,
    hora_registro     timestamptz NOT NULL DEFAULT now(),
    creado_en         timestamptz NOT NULL DEFAULT now(),
    actualizado_en    timestamptz NOT NULL DEFAULT now(),
    eliminado_en      timestamptz
);

CREATE UNIQUE INDEX uq_asistencia_beca_fecha
    ON asistencia (beca_id, fecha) WHERE eliminado_en IS NULL;
CREATE INDEX ix_asistencia_fecha
    ON asistencia (fecha) WHERE eliminado_en IS NULL;
CREATE INDEX ix_asistencia_registrado_por
    ON asistencia (registrado_por) WHERE eliminado_en IS NULL;
