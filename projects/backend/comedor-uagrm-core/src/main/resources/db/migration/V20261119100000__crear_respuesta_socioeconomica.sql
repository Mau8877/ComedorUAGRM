-- =====================================================================
-- Rangos de la respuesta socioeconomica de una postulacion (CU02: enviar postulacion).
-- Tabla: respuesta_socioeconomica (1:1 con postulacion)
-- Fuente: diseno logico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- La tabla ya la creo `V20261107110000` (CU07, expedientes): PK = FK a postulacion (no tiene `id`
-- propio, igual que `postulante`) y todas las columnas de datos NULLABLE. Esta migracion solo le
-- agrega los CHECK de rango del CU02. Antes intentaba crearla otra vez y fallaba con
-- `relation "respuesta_socioeconomica" already exists` en cualquier base nueva.
-- Las columnas de datos son NULLABLE a proposito: el borrador (CU17) guarda un avance parcial y el
-- servicio del CU02 exige que esten todas completas recien al ENVIAR la postulacion. Los CHECK
-- validan el rango solo cuando hay valor. Los catalogos de procedencia, patrimonio y
-- vulnerabilidad estan por definir con la DUBSS: se validan en el servicio, no con un enum de BD.
-- =====================================================================

ALTER TABLE respuesta_socioeconomica
    ADD CONSTRAINT ck_respuesta_socioeconomica_ingresos
        CHECK (ingresos_familiares IS NULL OR ingresos_familiares >= 0),
    ADD CONSTRAINT ck_respuesta_socioeconomica_dependientes
        CHECK (dependientes IS NULL OR dependientes >= 0),
    ADD CONSTRAINT ck_respuesta_socioeconomica_promedio
        CHECK (promedio_academico IS NULL OR promedio_academico BETWEEN 0 AND 100);
