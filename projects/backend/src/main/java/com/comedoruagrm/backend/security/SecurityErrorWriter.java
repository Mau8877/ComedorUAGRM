package com.comedoruagrm.backend.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escribe a mano el sobre de error de la API para los rechazos de seguridad.
 *
 * * Estos rechazos ocurren en filtros, antes del DispatcherServlet, asi que
 * * el @RestControllerAdvice de GlobalExceptionHandler nunca llega a
 * * intervenir (mismo motivo que en RateLimitFilter). Igual se respeta el
 * * contrato de RESPONSES_BACKEND.md para que el cliente lo trate como
 * * cualquier otro error de la API.
 */
@Component
public class SecurityErrorWriter {

    public void escribir(HttpServletResponse response, int status, String errorCode, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = """
                {"status":"failed","data":null,"message":"%s","timestamp":"%s","error":"%s"}"""
                .formatted(escapar(message), Instant.now().toString(), errorCode);
        response.getWriter().write(body);
    }

    private String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
