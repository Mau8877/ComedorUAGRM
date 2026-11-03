package com.comedoruagrm.backend.features.auth.dto;

import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;

/**
 * Sesion abierta: el token de acceso (header `Authorization: Bearer`), el
 * refresh token para renovarlo y el usuario con su rol. `expiresIn` son los
 * segundos de vida del token de acceso.
 */
public record SesionResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
        UsuarioResponse usuario) {

    public static final String BEARER = "Bearer";
}
