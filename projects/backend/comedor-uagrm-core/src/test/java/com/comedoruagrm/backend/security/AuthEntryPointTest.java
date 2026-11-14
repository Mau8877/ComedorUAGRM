package com.comedoruagrm.backend.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Los rechazos de autenticacion salen en el sobre estandar de la API con ERR_AUTH_01. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthEntryPointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void peticion_sinToken_devuelve401ConElSobreYErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/algo"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"))
                .andExpect(jsonPath("$.message").value("Tu sesión no es válida. Inicia sesión de nuevo."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void peticion_conTokenMalFirmado_devuelve401ConErrAuth01() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Signed JWT rejected: Invalid signature"));

        mockMvc.perform(get("/api/v1/algo").header(HttpHeaders.AUTHORIZATION, "Bearer token.mal.firmado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void peticion_conTokenVencido_devuelve401ConErrAuth01() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Jwt expired at 2020-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/v1/algo").header(HttpHeaders.AUTHORIZATION, "Bearer token.vencido.x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void health_sinToken_sigueSiendoPublico() throws Exception {
        mockMvc.perform(get("/health")).andExpect(result -> {
            int estado = result.getResponse().getStatus();
            if (estado == 401 || estado == 403) {
                throw new AssertionError("/health no debe exigir autenticacion, devolvio " + estado);
            }
        });
    }
}
