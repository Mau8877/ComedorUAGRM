package com.comedoruagrm.backend.features.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contraseña desde el perfil (CU15, flujo E).
 *
 * * `refreshToken` es opcional: mobile lo manda para que el backend sepa cual es la sesion
 * * actual y la conserve; la web lo manda por cookie, no en el cuerpo.
 * ! Ni las contraseñas ni el refresh token se loguean.
 */
public record CambiarContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String contrasenaNueva,

        @NotBlank(message = "Confirma tu contraseña")
        String confirmacionContrasena,

        String refreshToken) {

    @AssertTrue(message = "La contraseña y su confirmación no coinciden")
    public boolean isConfirmacionCoincide() {
        return contrasenaNueva == null || contrasenaNueva.equals(confirmacionContrasena);
    }
}
