-- =====================================================================
-- Planilla de asistencia imprimible (CU09)
-- Tabla nueva: planilla_asistencia
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Una planilla es el PDF del padron vigente de una convocatoria para un periodo. Se puede generar mas de una
-- por convocatoria (una por periodo, o una nueva tras cambios del padron): no hay unicidad. El PDF vive en
-- MinIO; la base solo guarda la ruta (`storage_path`).
-- `total_becados` (columna que el diagrama 091 no dibuja) guarda cuantos becados vigentes incluyo: la lista de
-- planillas lo muestra y el cotejo (CU11) lo compara con lo firmado, sin releer el PDF.
-- =====================================================================

CREATE TABLE planilla_asistencia (
    id                         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_planilla_asistencia uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id            bigint       NOT NULL REFERENCES convocatoria (id),
    generada_por               bigint       NOT NULL REFERENCES usuario (id),
    fecha_desde                date         NOT NULL,
    fecha_hasta                date         NOT NULL,
    total_becados              integer      NOT NULL,
    storage_path               varchar(500) NOT NULL,
    generada_en                timestamptz  NOT NULL DEFAULT now(),
    creado_en                  timestamptz  NOT NULL DEFAULT now(),
    actualizado_en             timestamptz  NOT NULL DEFAULT now(),
    eliminado_en               timestamptz,
    CONSTRAINT ck_planilla_asistencia_periodo CHECK (fecha_hasta >= fecha_desde),
    CONSTRAINT ck_planilla_asistencia_total CHECK (total_becados > 0)
);

CREATE INDEX ix_planilla_asistencia_convocatoria
    ON planilla_asistencia (convocatoria_id, generada_en DESC) WHERE eliminado_en IS NULL;
CREATE INDEX ix_planilla_asistencia_generada_por
    ON planilla_asistencia (generada_por) WHERE eliminado_en IS NULL;
