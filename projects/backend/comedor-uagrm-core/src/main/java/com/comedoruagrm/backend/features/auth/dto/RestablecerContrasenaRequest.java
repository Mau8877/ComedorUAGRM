package com.comedoruagrm.backend.features.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contraseña nueva definida con el enlace del correo (CU15, flujo B).
 *
 * * Misma política que el registro (CU01): de 6 a 72 caracteres (72 es el maximo de BCrypt)
 * * y coincidir con su confirmacion.
 * ! Ni el token ni la contraseña se loguean.
 */
public record RestablecerContrasenaRequest(
        @NotBlank(message = "El enlace no es válido")
        String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String contrasena,

        @NotBlank(message = "Confirma tu contraseña")
        String confirmacionContrasena) {

    @AssertTrue(message = "La contraseña y su confirmación no coinciden")
    public boolean isConfirmacionCoincide() {
        return contrasena == null || contrasena.equals(confirmacionContrasena);
    }
}
