package com.comedoruagrm.backend.security;

import java.util.Set;

import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Endpoints de negocio que no exigen sesion (ver SEGURIDAD_AUTH_BACKEND.md).
 * Los comparten SecurityConfig y el resolver del token: una peticion publica
 * ignora un `Authorization` vencido, para que un token viejo en el cliente no
 * impida iniciar sesion de nuevo.
 */
public final class RutasPublicas {

    public static final String CARRERAS = "/api/v1/carreras";
    public static final String REGISTRO = "/api/v1/auth/registro";
    public static final String LOGIN = "/api/v1/auth/login";
    public static final String REFRESH = "/api/v1/auth/refresh";
    public static final String LOGOUT = "/api/v1/auth/logout";

    private static final Set<String> POST_PUBLICOS = Set.of(REGISTRO, LOGIN, REFRESH, LOGOUT);

    private RutasPublicas() {
    }

    public static boolean esPublica(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        String metodo = request.getMethod();
        return (HttpMethod.GET.matches(metodo) && ruta.equals(CARRERAS))
                || (HttpMethod.POST.matches(metodo) && POST_PUBLICOS.contains(ruta));
    }
}
