-- =====================================================================
-- El puntaje de la IA pasa de 0-100 a 0-70 (CU07).
-- El maximo de 100 puntos se reparte en hasta 70 que calcula la IA (CU03,
-- puntaje_postulacion.puntaje_total) y hasta 30 de rendimiento academico que
-- escribe el auditor al auditar el expediente (auditoria_expediente.puntaje_academico).
-- Aun no hay puntajes reales, solo de prueba: se reescalan (x 0.7) para que
-- el orden de la lista priorizada no cambie y la restriccion nueva se pueda crear.
-- =====================================================================

ALTER TABLE puntaje_postulacion DROP CONSTRAINT ck_puntaje_postulacion_total;

UPDATE puntaje_postulacion SET puntaje_total = round(puntaje_total * 0.7, 2);

ALTER TABLE puntaje_postulacion
    ADD CONSTRAINT ck_puntaje_postulacion_total CHECK (puntaje_total BETWEEN 0 AND 70);
