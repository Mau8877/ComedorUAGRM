package com.comedoruagrm.backend.features.asistencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El CI del becado que el Personal del Comedor quiere buscar (CU10). Viaja en el cuerpo, no en la URL. */
public record BuscarBecadoRequest(
        @NotBlank(message = "El CI es obligatorio")
        @Size(max = 20, message = "El CI no puede superar los 20 caracteres") String ci) {
}
