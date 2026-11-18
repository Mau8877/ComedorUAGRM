package com.comedoruagrm.backend.features.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Indicadores del dashboard del Directivo (CU12).
 *
 * * `gestion` es null si todavia no hay gestiones; `convocatoria` es null cuando se piden «Todas».
 * * `actualizadoEn` es null si el lote nunca se ejecuto; `sinDatos` es verdadero si todos los totales son cero.
 * * `mes` va como `yyyy-MM`.
 * * asistenciaPromedio y asistencia (en %) salen del registro de asistencia (CU10); valen null si no hubo asistencia que medir.
 */
public record IndicadoresResponse(GestionResumen gestion, ConvocatoriaResumen convocatoria, Totales totales,
        List<IndicadorMensual> porMes, Instant actualizadoEn, boolean sinDatos) {

    /** La gestion consultada. */
    public record GestionResumen(UUID codigo, String etiqueta) {
    }

    /** La convocatoria consultada. */
    public record ConvocatoriaResumen(UUID codigo, String nombre) {
    }

    /** Totales del periodo consultado. */
    public record Totales(long postulacionesRecibidas, long finalistas, long becasAdjudicadas,
            Double asistenciaPromedio) {
    }

    /** Una fila de la serie mensual. */
    public record IndicadorMensual(String mes, long postulaciones, long finalistas, long adjudicadas,
            Double asistencia) {
    }
}
