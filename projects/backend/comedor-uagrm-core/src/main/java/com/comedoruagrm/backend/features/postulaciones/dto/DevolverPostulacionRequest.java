package com.comedoruagrm.backend.features.postulaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de la devolucion (CU19): lo que el postulante debe corregir. Obligatorio, hasta 500 caracteres. */
public record DevolverPostulacionRequest(
        @NotBlank(message = "El motivo de la devolución es obligatorio")
        @Size(max = 500, message = "El motivo no puede pasar de 500 caracteres") String motivo) {
}
