-- =====================================================================
-- Revision de las postulaciones que la IA no puede calificar (CU03 A1).
-- Tabla: postulacion (dos columnas)
--
-- `intentos_precalificacion` cuenta las veces que la IA rechazo los datos (la IA caida no cuenta).
-- Al llegar al maximo (3) se anota `precalificacion_revision_en` y el reintento automatico deja de
-- tomarla: sin esto, las que nunca se pueden calcular taparian a las demas en cada pasada. No es un
-- estado nuevo: sigue `pendiente`. Reenviar la postulacion (CU02) reinicia ambos.
-- =====================================================================

ALTER TABLE postulacion
    ADD COLUMN intentos_precalificacion   smallint    NOT NULL DEFAULT 0,
    ADD COLUMN precalificacion_revision_en timestamptz NULL;
