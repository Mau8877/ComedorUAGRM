package com.comedoruagrm.backend.features.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de inicio de sesion. No se valida el formato del correo ni el
 * largo de la contraseña: cualquier dato que no coincida da el mismo 401.
 */
public record LoginRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena) {
}
