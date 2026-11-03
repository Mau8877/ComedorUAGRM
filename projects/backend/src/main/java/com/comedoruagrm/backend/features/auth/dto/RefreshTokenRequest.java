package com.comedoruagrm.backend.features.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Refresh token de la sesion de un dispositivo (para renovarla o cerrarla). */
public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken) {
}
