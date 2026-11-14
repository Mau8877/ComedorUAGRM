package com.comedoruagrm.backend.security;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Responde 403 con ERR_AUTH_07 cuando un usuario autenticado llama a algo que
 * exige un rol que no tiene (un `@PreAuthorize` de un `service`).
 *
 * * Un `AccessDeniedException` lanzado dentro de un controller no pasa por
 * * GlobalExceptionHandler: sube hasta el filtro de seguridad, que lo entrega
 * * a este handler. Por eso el sobre se escribe a mano, igual que en
 * * ApiAuthenticationEntryPoint.
 * ! Nunca se loguea el token: solo el codigo del usuario (el `name` del Authentication).
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiAccessDeniedHandler.class);

    private final SecurityErrorWriter errorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        LOG.warn("Acceso denegado por falta de rol",
                kv("module", "auth"),
                kv("errorCode", AuthErrorCodes.ERR_AUTH_07),
                kv("userId", autenticacion != null ? autenticacion.getName() : null));
        errorWriter.escribir(response, HttpServletResponse.SC_FORBIDDEN, AuthErrorCodes.ERR_AUTH_07,
                "No tienes permiso para realizar esta acción.");
    }
}
