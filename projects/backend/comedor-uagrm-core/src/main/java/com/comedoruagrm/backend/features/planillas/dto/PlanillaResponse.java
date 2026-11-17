package com.comedoruagrm.backend.features.planillas.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una planilla de asistencia generada (CU09). `generadaPor` es solo el nombre para mostrar. Nunca lleva la ruta
 * del archivo en el almacenamiento ni la direccion de descarga.
 */
public record PlanillaResponse(
        UUID codigo,
        UUID convocatoria,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int totalBecados,
        Instant generadaEn,
        String generadaPor) {
}
