package com.comedoruagrm.backend.features.asistencias.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * El resultado de registrar la asistencia (CU10). `yaRegistrada` es `true` cuando ese dia ya existia un registro:
 * no se creo otro y `horaRegistro` es la hora del primero (idempotencia, RNF-03).
 */
public record AsistenciaResponse(
        UUID codigo,
        UUID becaCodigo,
        LocalDate fecha,
        Instant horaRegistro,
        boolean yaRegistrada) {
}
