package com.comedoruagrm.backend.features.notificaciones.dto;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registro del telefono: el token que entrega Firebase y su plataforma. */
public record RegistrarDispositivoRequest(
        @NotBlank(message = "El token es obligatorio") @Size(max = 512, message = "El token es demasiado largo")
        String token,
        @NotNull(message = "La plataforma es obligatoria") PlataformaDispositivo plataforma) {
}
