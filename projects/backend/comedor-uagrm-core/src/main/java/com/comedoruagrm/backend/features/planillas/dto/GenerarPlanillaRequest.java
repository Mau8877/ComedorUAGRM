package com.comedoruagrm.backend.features.planillas.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Periodo de la planilla (CU09): de `fechaDesde` a `fechaHasta`, ambas incluidas. */
public record GenerarPlanillaRequest(
        @NotNull(message = "La fecha inicial es obligatoria") LocalDate fechaDesde,
        @NotNull(message = "La fecha final es obligatoria") LocalDate fechaHasta) {
}
