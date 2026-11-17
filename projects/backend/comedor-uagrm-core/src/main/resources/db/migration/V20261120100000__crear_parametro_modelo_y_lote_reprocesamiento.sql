-- =====================================================================
-- Parametros del modelo de preseleccion y lotes de reprocesamiento (CU26).
-- Tablas: parametro_modelo, lote_reprocesamiento
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Cada version del modelo (`modelo_preseleccion`, ya existe) tiene un conjunto de parametros: una
-- ponderacion por variable (puntos maximos, suman 100). `valor` queda libre (jsonb) para los criterios o rangos
-- que defina la DUBSS. Un lote de reprocesamiento recalcula, en segundo plano, los puntajes de una convocatoria
-- con una version del modelo. Se agregan `procesadas` y `fallidas` al diseno para mostrar el avance y las
-- postulaciones que fallaron (A5) sin una tabla de detalle.
-- =====================================================================

CREATE TABLE parametro_modelo (
    id                      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_parametro_modelo uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    modelo_id               bigint       NOT NULL REFERENCES modelo_preseleccion (id),
    clave                   varchar(60)  NOT NULL,
    ponderacion             numeric(5, 2) NOT NULL,
    valor                   jsonb,
    creado_en               timestamptz  NOT NULL DEFAULT now(),
    actualizado_en          timestamptz  NOT NULL DEFAULT now(),
    eliminado_en            timestamptz,
    CONSTRAINT ck_parametro_modelo_ponderacion CHECK (ponderacion BETWEEN 0 AND 100)
);

-- Una ponderacion por variable y version.
CREATE UNIQUE INDEX uq_parametro_modelo_clave ON parametro_modelo (modelo_id, clave) WHERE eliminado_en IS NULL;

CREATE TABLE lote_reprocesamiento (
    id                          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_lote_reprocesamiento uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    convocatoria_id             bigint      NOT NULL REFERENCES convocatoria (id),
    modelo_id                   bigint      NOT NULL REFERENCES modelo_preseleccion (id),
    solicitado_por              bigint      NOT NULL REFERENCES usuario (id),
    estado                      varchar(20) NOT NULL,
    total_postulaciones         int         NOT NULL,
    procesadas                  int         NOT NULL DEFAULT 0,
    fallidas                    int         NOT NULL DEFAULT 0,
    iniciado_en                 timestamptz,
    terminado_en                timestamptz,
    creado_en                   timestamptz NOT NULL DEFAULT now(),
    actualizado_en              timestamptz NOT NULL DEFAULT now(),
    eliminado_en                timestamptz,
    CONSTRAINT ck_lote_reprocesamiento_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'FALLIDO')),
    CONSTRAINT ck_lote_reprocesamiento_total CHECK (total_postulaciones >= 0 AND procesadas >= 0 AND fallidas >= 0)
);

-- Un solo reprocesamiento en curso por convocatoria (A4): el segundo falla al insertar.
CREATE UNIQUE INDEX uq_lote_reprocesamiento_en_curso
    ON lote_reprocesamiento (convocatoria_id)
    WHERE estado IN ('PENDIENTE', 'EN_PROCESO') AND eliminado_en IS NULL;
CREATE INDEX ix_lote_reprocesamiento_convocatoria_id ON lote_reprocesamiento (convocatoria_id) WHERE eliminado_en IS NULL;
