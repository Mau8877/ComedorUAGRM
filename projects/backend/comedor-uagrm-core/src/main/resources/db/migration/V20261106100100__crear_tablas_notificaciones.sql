-- =====================================================================
-- Notificaciones push y correo (CU-EXTRA-01).
--
-- notificacion: la bandeja de avisos de cada usuario. Una fila por aviso,
--   sin importar por cuantas vias se envie (push, correo).
--   OJO: el diagrama tambien trae comunicado_id y postulacion_id, pero
--   apuntan a tablas que todavia no existen; las agrega el CU que cree
--   cada una.
-- dispositivo_fcm: los telefonos (token de Firebase) donde cada usuario
--   recibe push. Un token pertenece a un solo usuario a la vez.
-- =====================================================================

CREATE TYPE tipo_notificacion AS ENUM ('SUCCESS', 'INFO', 'WARNING', 'ERROR');

CREATE TABLE notificacion (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_notificacion uuid              NOT NULL DEFAULT uuidv7() UNIQUE,
    usuario_id          bigint            NOT NULL REFERENCES usuario (id),
    titulo              varchar(150)      NOT NULL,
    mensaje             text              NOT NULL,
    tipo                tipo_notificacion NOT NULL,
    leida               boolean           NOT NULL DEFAULT false,
    enviada_en          timestamptz,
    creado_en           timestamptz       NOT NULL DEFAULT now(),
    actualizado_en      timestamptz       NOT NULL DEFAULT now(),
    eliminado_en        timestamptz
);

CREATE INDEX ix_notificacion_usuario_creado
    ON notificacion (usuario_id, creado_en DESC) WHERE eliminado_en IS NULL;

CREATE TABLE dispositivo_fcm (
    id                     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_dispositivo_fcm uuid          NOT NULL DEFAULT uuidv7() UNIQUE,
    usuario_id             bigint        NOT NULL REFERENCES usuario (id),
    token                  varchar(512)  NOT NULL,
    plataforma             varchar(20)   NOT NULL,
    creado_en              timestamptz   NOT NULL DEFAULT now(),
    actualizado_en         timestamptz   NOT NULL DEFAULT now(),
    eliminado_en           timestamptz
);

CREATE UNIQUE INDEX uq_dispositivo_fcm_token ON dispositivo_fcm (token) WHERE eliminado_en IS NULL;
CREATE INDEX ix_dispositivo_fcm_usuario_id ON dispositivo_fcm (usuario_id) WHERE eliminado_en IS NULL;
