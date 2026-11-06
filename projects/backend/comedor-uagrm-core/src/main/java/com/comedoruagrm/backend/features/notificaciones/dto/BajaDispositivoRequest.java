package com.comedoruagrm.backend.features.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Baja del telefono al cerrar sesion: el token que se registro. */
public record BajaDispositivoRequest(
        @NotBlank(message = "El token es obligatorio") @Size(max = 512, message = "El token es demasiado largo")
        String token) {
}
