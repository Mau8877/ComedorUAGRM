package com.comedoruagrm.backend.features.asistencias.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * El becado encontrado por CI (CU10): sus datos para que el Personal del Comedor confirme su identidad y el estado
 * de su beca. `fecha` es el dia de hoy (hora de Bolivia) y `horaRegistro` solo viene si la asistencia de hoy ya se
 * registro. No lleva ids internos ni el correo.
 */
public record BecadoAsistenciaResponse(
        UUID becaCodigo,
        String nombres,
        String apellidos,
        String ci,
        String carrera,
        String convocatoria,
        String estadoBeca,
        LocalDate fecha,
        Instant horaRegistro) {
}
