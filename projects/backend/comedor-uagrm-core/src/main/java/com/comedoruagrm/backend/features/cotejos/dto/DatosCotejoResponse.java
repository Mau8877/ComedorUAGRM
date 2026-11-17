package com.comedoruagrm.backend.features.cotejos.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lo que el Administrador necesita para cotejar una planilla (CU11): la planilla y cuantos registros digitales hay en
 * su periodo. `registrosDigitales` es 0 cuando no hay ninguno (A1).
 */
public record DatosCotejoResponse(
        UUID planilla,
        UUID convocatoria,
        String convocatoriaNombre,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int totalBecados,
        Instant generadaEn,
        long registrosDigitales,
        long becadosConAsistencia) {
}
