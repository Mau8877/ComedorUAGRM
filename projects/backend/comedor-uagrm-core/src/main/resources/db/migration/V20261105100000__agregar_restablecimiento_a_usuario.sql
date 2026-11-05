-- =====================================================================
-- Recuperación de contraseña (CU15): el enlace vigente de cada cuenta.
--
-- Solo hay un enlace vigente por cuenta: pedir otro sobrescribe las dos
-- columnas y usarlo las deja en NULL. Se guarda el hash SHA-256 del token
-- (nunca el token). Las cuentas existentes quedan sin enlace.
-- =====================================================================

ALTER TABLE usuario
    ADD COLUMN restablecimiento_token_hash varchar(64),
    ADD COLUMN restablecimiento_expira_en  timestamptz;

CREATE UNIQUE INDEX uq_usuario_restablecimiento_token_hash
    ON usuario (restablecimiento_token_hash)
    WHERE restablecimiento_token_hash IS NOT NULL AND eliminado_en IS NULL;
