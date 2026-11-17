package com.comedoruagrm.backend.features.preseleccion.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * El estado de un reprocesamiento: `PENDIENTE`, `EN_PROCESO`, `COMPLETADO` o `FALLIDO`, con su avance. `fallidas`
 * son las que el modulo de IA no pudo calcular; volver a reprocesar toma solo las que faltan.
 */
public record LoteReprocesamientoResponse(
        UUID codigo,
        UUID convocatoria,
        String modelo,
        String estado,
        int total,
        int procesadas,
        int fallidas,
        Instant iniciadoEn,
        Instant terminadoEn) {
}
