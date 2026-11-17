package com.comedoruagrm.backend.features.mibeca.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Una asistencia del historial del becado (CU24): el dia (hora de Bolivia) y la hora en que se registro. */
public record AsistenciaBecadoResponse(LocalDate fecha, Instant horaRegistro) {
}
