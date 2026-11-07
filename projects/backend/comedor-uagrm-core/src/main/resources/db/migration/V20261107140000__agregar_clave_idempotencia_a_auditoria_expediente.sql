-- =====================================================================
-- CU07: clave de idempotencia de la auditoria del expediente (RNF-03)
-- =====================================================================
-- El cliente genera un UUID por intento de auditoria y lo manda en el header
-- `Idempotency-Key`; repetir la peticion con la misma clave (por ejemplo, tras
-- perder la respuesta) devuelve la auditoria ya guardada en vez de crear otra.
-- La clave es opcional: una auditoria sin clave siempre se crea.
-- El indice unico parcial es la ultima barrera si dos peticiones con la misma
-- clave llegaran a la vez (el servicio ya las serializa con un bloqueo).
-- =====================================================================

ALTER TABLE auditoria_expediente ADD COLUMN clave_idempotencia uuid;

CREATE UNIQUE INDEX uq_auditoria_expediente_clave_idempotencia
    ON auditoria_expediente (finalista_id, clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;
