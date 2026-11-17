-- =====================================================================
-- Cotejo del registro digital contra la planilla fisica (CU11)
-- Tabla nueva: cotejo_asistencia
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Un cotejo es el resultado que el Administrador registra tras comparar a mano la planilla firmada (CU09) con los
-- registros digitales de asistencia (CU10). Se pueden registrar varios cotejos de una misma planilla: cada uno es un
-- registro de lo que se constato y no se edita ni se elimina.
-- `resultado` es varchar (asi esta en el diagrama) con un CHECK; las reglas de consistencia que valida el servicio
-- (coincidencias + discrepancias = total, resultado acorde a las discrepancias y observaciones obligatorias cuando
-- hay discrepancias) tambien se exigen aqui como ultima barrera.
-- No hay `cotejo_detalle` (opcional en el diseno): el detalle por becado y dia lo traera la lectura automatica de la
-- planilla firmada, que todavia no existe.
-- =====================================================================

CREATE TABLE cotejo_asistencia (
    id                        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_cotejo_asistencia  uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    planilla_id               bigint      NOT NULL REFERENCES planilla_asistencia (id),
    realizado_por             bigint      NOT NULL REFERENCES usuario (id),
    fecha_cotejo              date        NOT NULL,
    total_registros           integer     NOT NULL,
    coincidencias             integer     NOT NULL,
    discrepancias             integer     NOT NULL,
    resultado                 varchar(20) NOT NULL,
    observaciones             text,
    creado_en                 timestamptz NOT NULL DEFAULT now(),
    actualizado_en            timestamptz NOT NULL DEFAULT now(),
    eliminado_en              timestamptz,
    CONSTRAINT ck_cotejo_asistencia_cantidades
        CHECK (total_registros >= 0 AND coincidencias >= 0 AND discrepancias >= 0
               AND coincidencias + discrepancias = total_registros),
    CONSTRAINT ck_cotejo_asistencia_resultado CHECK (
        (resultado = 'CONFORME' AND discrepancias = 0)
        OR (resultado = 'CON_DISCREPANCIAS' AND discrepancias > 0
            AND observaciones IS NOT NULL AND btrim(observaciones) <> ''))
);

CREATE INDEX ix_cotejo_asistencia_planilla
    ON cotejo_asistencia (planilla_id, creado_en DESC) WHERE eliminado_en IS NULL;
CREATE INDEX ix_cotejo_asistencia_realizado_por
    ON cotejo_asistencia (realizado_por) WHERE eliminado_en IS NULL;
