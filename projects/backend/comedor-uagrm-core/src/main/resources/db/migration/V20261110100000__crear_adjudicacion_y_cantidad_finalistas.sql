-- =====================================================================
-- Adjudicacion de becas (CU05)
-- Tabla nueva: adjudicacion (+ enum resultado_adjudicacion)
-- Columna nueva: convocatoria.cantidad_finalistas
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Una fila por postulacion decidida (adjudicada o no adjudicada por decision final); inmutable
-- una vez creada. `puntaje_final` es el total (IA + academico) calculado al decidir y puede ser
-- NULL cuando se «no adjudica» una auditoria sin puntaje academico. No lleva motivo (va en
-- postulacion_historial) ni clave de idempotencia.
-- `convocatoria.cantidad_finalistas` la escribira el CU13; el CU05 solo la lee.
-- =====================================================================

CREATE TYPE resultado_adjudicacion AS ENUM ('ADJUDICADA', 'NO_ADJUDICADA');

CREATE TABLE adjudicacion (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_adjudicacion uuid                   NOT NULL DEFAULT uuidv7() UNIQUE,
    postulacion_id      bigint                 NOT NULL REFERENCES postulacion (id),
    resultado           resultado_adjudicacion NOT NULL,
    puntaje_final       numeric(5, 2),
    decidido_por        bigint                 NOT NULL REFERENCES usuario (id),
    fecha_adjudicacion  timestamptz            NOT NULL DEFAULT now(),
    creado_en           timestamptz            NOT NULL DEFAULT now(),
    actualizado_en      timestamptz            NOT NULL DEFAULT now(),
    eliminado_en        timestamptz,
    CONSTRAINT ck_adjudicacion_puntaje_final CHECK (puntaje_final IS NULL OR puntaje_final BETWEEN 0 AND 100)
);

-- Una sola decision por postulacion (idempotencia: RNF-03).
CREATE UNIQUE INDEX uq_adjudicacion_postulacion
    ON adjudicacion (postulacion_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_adjudicacion_decidido_por
    ON adjudicacion (decidido_por) WHERE eliminado_en IS NULL;

ALTER TABLE convocatoria
    ADD COLUMN cantidad_finalistas int,
    ADD CONSTRAINT ck_convocatoria_cantidad_finalistas
        CHECK (cantidad_finalistas IS NULL OR cantidad_finalistas > 0);
