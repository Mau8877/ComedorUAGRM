package com.comedoruagrm.backend.features.convocatorias.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de `POST` y `PUT /api/v1/gestiones-academicas`: la gestion completa. */
public record GestionAcademicaRequest(
        @NotBlank(message = "La etiqueta es obligatoria")
        @Size(max = 50, message = "La etiqueta no puede pasar de 50 caracteres")
        String etiqueta,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin) {
}
