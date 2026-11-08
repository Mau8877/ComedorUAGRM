-- =====================================================================
-- Historial de estados de la postulacion (CU19; lo amplian CU02, CU20 y CU05)
-- Tabla: postulacion_historial
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Append-only: una fila no se edita ni se borra. Cada cambio de estado de una
-- postulacion deja una fila con el estado anterior, el nuevo, quien lo hizo
-- (NULL si fue el sistema) y el motivo. El CU19 la crea; los demas CU la usan
-- sin recrearla. `motivo` es text: su limite lo valida cada servicio.
-- =====================================================================

CREATE TABLE postulacion_historial (
    id                           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_postulacion_historial uuid               NOT NULL DEFAULT uuidv7() UNIQUE,
    postulacion_id               bigint             NOT NULL REFERENCES postulacion (id),
    usuario_id                   bigint             REFERENCES usuario (id),
    estado_anterior              estado_postulacion NOT NULL,
    estado_nuevo                 estado_postulacion NOT NULL,
    motivo                       text,
    creado_en                    timestamptz        NOT NULL DEFAULT now(),
    actualizado_en               timestamptz        NOT NULL DEFAULT now(),
    eliminado_en                 timestamptz
);

-- El historial de una postulacion se lee en orden.
CREATE INDEX ix_postulacion_historial_postulacion_creado
    ON postulacion_historial (postulacion_id, creado_en) WHERE eliminado_en IS NULL;
CREATE INDEX ix_postulacion_historial_usuario_id
    ON postulacion_historial (usuario_id) WHERE eliminado_en IS NULL;
