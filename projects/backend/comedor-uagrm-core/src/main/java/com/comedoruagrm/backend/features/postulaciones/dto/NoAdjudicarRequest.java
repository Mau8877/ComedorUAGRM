package com.comedoruagrm.backend.features.postulaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de «No adjudicar» (CU05). Obligatorio, hasta 500 caracteres. */
public record NoAdjudicarRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede pasar de 500 caracteres") String motivo) {
}
