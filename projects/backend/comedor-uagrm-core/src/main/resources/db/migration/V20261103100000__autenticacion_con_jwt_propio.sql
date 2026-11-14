-- =====================================================================
-- Autenticación propia con JWT (reemplaza a Firebase Auth).
--
-- usuario: se quita firebase_uid y se agregan la contraseña (hash BCrypt) y
-- el control de bloqueo por intentos fallidos. Las cuentas que ya existían
-- quedan con un hash inválido ('!'): no pueden iniciar sesión hasta que se
-- les asigne una contraseña (recuperación de contraseña, CU15).
--
-- sesion: un refresh token por dispositivo. Se guarda solo el hash SHA-256
-- del token (nunca el token), y cerrar sesión lo marca como revocado.
-- =====================================================================

ALTER TABLE usuario DROP COLUMN firebase_uid;

ALTER TABLE usuario
    ADD COLUMN password_hash          varchar(100) NOT NULL DEFAULT '!',
    ADD COLUMN intentos_fallidos      integer      NOT NULL DEFAULT 0,
    ADD COLUMN bloqueos_consecutivos  integer      NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta        timestamptz;

ALTER TABLE usuario ALTER COLUMN password_hash DROP DEFAULT;

CREATE TABLE sesion (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_sesion  uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    usuario_id     bigint       NOT NULL REFERENCES usuario (id),
    token_hash     varchar(64)  NOT NULL,
    expira_en      timestamptz  NOT NULL,
    revocada_en    timestamptz,
    creado_en      timestamptz  NOT NULL DEFAULT now(),
    actualizado_en timestamptz  NOT NULL DEFAULT now(),
    eliminado_en   timestamptz
);

CREATE UNIQUE INDEX uq_sesion_token_hash ON sesion (token_hash) WHERE eliminado_en IS NULL;
CREATE INDEX ix_sesion_usuario_id ON sesion (usuario_id) WHERE eliminado_en IS NULL;
