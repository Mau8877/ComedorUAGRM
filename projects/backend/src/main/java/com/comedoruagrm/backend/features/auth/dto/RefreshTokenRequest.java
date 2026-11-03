package com.comedoruagrm.backend.features.auth.dto;

/**
 * Refresh token de la sesion de un dispositivo (para renovarla o cerrarla).
 * Es el cuerpo que usa mobile; la web no lo manda: el navegador envia el
 * refresh token en su cookie `HttpOnly`. Si vienen las dos cosas gana el cuerpo.
 */
public record RefreshTokenRequest(String refreshToken) {
}
