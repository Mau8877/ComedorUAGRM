package com.comedoruagrm.backend.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Responde 401 con ERR_AUTH_01 cuando falta el token o no es valido. */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        errorWriter.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorCodes.ERR_AUTH_01,
                "Tu sesión no es válida. Inicia sesión de nuevo.");
    }
}
