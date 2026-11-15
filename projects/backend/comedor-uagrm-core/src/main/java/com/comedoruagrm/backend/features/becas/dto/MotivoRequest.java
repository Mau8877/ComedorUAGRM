package com.comedoruagrm.backend.features.becas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El motivo obligatorio de una baja o una suspension (CU23, A1). */
public record MotivoRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo) {
}
