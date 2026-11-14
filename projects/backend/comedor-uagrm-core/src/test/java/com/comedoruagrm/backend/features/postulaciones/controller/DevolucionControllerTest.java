package com.comedoruagrm.backend.features.postulaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.postulaciones.dto.DevolucionResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.DevolucionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.service.DevolucionService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de POST /api/v1/finalistas/{codigo}/devolver (CU19) con el service simulado.
 *
 * * El `403` por rol lo comprueba el service (`@PreAuthorize`), simulado aqui: se prueba en
 * * `DevolucionIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DevolucionControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DevolucionService devolucionService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID finalista = UUID.randomUUID();
    private final String ruta = "/api/v1/finalistas/" + finalista + "/devolver";

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    @Test
    void devolver_sinSesion_da401() throws Exception {
        mockMvc.perform(post(ruta).contentType(JSON).content("{\"motivo\":\"Falta el carnet\"}"))
                .andExpect(status().isUnauthorized());
        verify(devolucionService, never()).devolver(any(), any(), any());
    }

    @Test
    void devolver_da200ConElSobreYSinIdsInternos() throws Exception {
        UUID postulacion = UUID.randomUUID();
        when(devolucionService.devolver(eq(admin), eq(finalista), any())).thenReturn(new ResultadoAuditado<>(
                new DevolucionResponse(postulacion, "devuelta", "Falta el carnet", Instant.now(), "Admin DUBSS"),
                1L, admin, 5L, Map.of(), true));

        mockMvc.perform(post(ruta).with(sesion()).contentType(JSON).content("{\"motivo\":\"Falta el carnet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.postulacion").value(postulacion.toString()))
                .andExpect(jsonPath("$.data.estado").value("devuelta"))
                .andExpect(jsonPath("$.data.motivo").value("Falta el carnet"))
                .andExpect(jsonPath("$.data.devueltaPor").value("Admin DUBSS"))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void devolver_sinMotivoOConMasDe500Caracteres_da400ErrSys01() throws Exception {
        String[] cuerpos = {"{}", "{\"motivo\":\"  \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}"};
        for (String cuerpo : cuerpos) {
            mockMvc.perform(post(ruta).with(sesion()).contentType(JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(devolucionService, never()).devolver(any(), any(), any());
    }

    @Test
    void devolver_conFinalistaInexistente_da404ErrFin01() throws Exception {
        when(devolucionService.devolver(eq(admin), eq(finalista), any()))
                .thenThrow(new NotFoundException("ERR_FIN_01", "El finalista no existe"));

        mockMvc.perform(post(ruta).with(sesion()).contentType(JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    @Test
    void devolver_conConflicto_da409ConSuCodigo() throws Exception {
        when(devolucionService.devolver(eq(admin), eq(finalista), any())).thenThrow(
                new ConflictException(DevolucionErrorCodes.ERR_DEV_01, "Solo se puede devolver una postulación calificada"));

        mockMvc.perform(post(ruta).with(sesion()).contentType(JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_01"));

        when(devolucionService.devolver(eq(admin), eq(finalista), any())).thenThrow(
                new ConflictException(DevolucionErrorCodes.ERR_DEV_02, "Primero se debe registrar la auditoría"));

        mockMvc.perform(post(ruta).with(sesion()).contentType(JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_02"));
    }
}
