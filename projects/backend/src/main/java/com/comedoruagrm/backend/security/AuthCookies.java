package com.comedoruagrm.backend.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Cookies de sesion para el cliente web: el token de acceso y el refresh
 * token viajan en cookies `HttpOnly`, asi el JavaScript de la pagina nunca
 * los ve (un XSS no puede robarlos). Mobile no usa cookies: manda el token
 * en `Authorization: Bearer` y guarda el refresh token en su almacenamiento
 * seguro (ver SEGURIDAD_AUTH_BACKEND.md).
 *
 * * `SameSite=Strict`: el navegador no manda las cookies en peticiones que
 * * se originan en otro sitio, que es la defensa contra CSRF (el backend
 * * tiene CSRF deshabilitado a proposito, ver SecurityConfig).
 * * El refresh token solo se manda a `/api/v1/auth` (login, refresh y
 * * logout), no a cada peticion; el token de acceso, a toda la API.
 * * `Secure` se enciende con COOKIE_SECURE=true (obligatorio en produccion,
 * * detras de HTTPS; en desarrollo local por http no se podria usar).
 * ! Ni el valor de las cookies ni los tokens se loguean.
 */
@Component
public class AuthCookies {

    public static final String ACCESO = "access_token";
    public static final String REFRESH = "refresh_token";

    private static final String RUTA_ACCESO = "/api/v1";
    private static final String RUTA_REFRESH = "/api/v1/auth";

    private final boolean secure;
    private final Duration vidaRefresh;

    public AuthCookies(@Value("${app.cookies.secure}") boolean secure,
            @Value("${app.jwt.refresh-ttl-days}") long vidaRefreshDias) {
        this.secure = secure;
        this.vidaRefresh = Duration.ofDays(vidaRefreshDias);
    }

    public void escribir(HttpServletResponse response, String accessToken, long accessExpiraEnSegundos,
            String refreshToken) {
        agregar(response, cookie(ACCESO, accessToken, RUTA_ACCESO, Duration.ofSeconds(accessExpiraEnSegundos)));
        agregar(response, cookie(REFRESH, refreshToken, RUTA_REFRESH, vidaRefresh));
    }

    /** Las vence en el navegador (cerrar sesion). */
    public void borrar(HttpServletResponse response) {
        agregar(response, cookie(ACCESO, "", RUTA_ACCESO, Duration.ZERO));
        agregar(response, cookie(REFRESH, "", RUTA_REFRESH, Duration.ZERO));
    }

    private ResponseCookie cookie(String nombre, String valor, String ruta, Duration vida) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(ruta)
                .maxAge(vida)
                .build();
    }

    private void agregar(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
