package com.comedoruagrm.backend.features.preseleccion.dto;

import java.util.UUID;

/**
 * Lo que se muestra antes de confirmar el reprocesamiento (CU26, paso 7): la convocatoria, la version vigente, cuantas
 * postulaciones calificadas hay, cuantas quedan por recalcular con esa version y, si lo hay, el lote en curso.
 * `resultadosPublicados` indica que ya no se puede reprocesar (A3).
 */
public record ResumenReprocesamientoResponse(
        UUID convocatoria,
        String nombre,
        String gestion,
        String modelo,
        int calificadas,
        int porReprocesar,
        boolean resultadosPublicados,
        LoteReprocesamientoResponse loteEnCurso) {
}
