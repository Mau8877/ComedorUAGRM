package com.comedoruagrm.backend.features.mibeca.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * La beca del usuario autenticado (CU24): su estado, la convocatoria y gestion a la que pertenece y el total de
 * asistencias registradas. `motivo` solo viene cuando la beca esta suspendida o dada de baja (el del ultimo
 * movimiento del padron). `fechaAdjudicacion` es nula en una beca de reemplazo (no tiene adjudicacion propia).
 * No lleva ids internos.
 */
public record MiBecaResponse(
        UUID codigo,
        String estado,
        String convocatoria,
        String gestion,
        Instant fechaAdjudicacion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String motivo,
        long totalAsistencias) {
}
