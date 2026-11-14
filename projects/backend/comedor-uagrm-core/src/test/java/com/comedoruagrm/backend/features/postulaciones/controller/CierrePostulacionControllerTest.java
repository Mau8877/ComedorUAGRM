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
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.CierrePostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.service.CierrePostulacionService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de POST /api/v1/finalistas/{codigo}/archivar y /rechazar (CU20) con el service simulado.
 *
 * * El `403` por rol lo comprueba el service (`@PreAuthorize`), simulado aqui: se prueba en
 * * `CierrePostulacionIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CierrePostulacionControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String CUERPO = "{\"motivo\":\"No entregó sus documentos\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CierrePostulacionService cierreService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID finalista = UUID.randomUUID();
    private final String[] acciones = {"archivar", "rechazar"};

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private String ruta(String accion) {
        return "/api/v1/finalistas/" + finalista + "/" + accion;
    }

    private void simular(String accion, ResultadoAuditado<CierrePostulacionResponse> r) {
        if ("archivar".equals(accion)) {
            when(cierreService.archivar(eq(admin), eq(finalista), any())).thenReturn(r);
        } else {
            when(cierreService.rechazar(eq(admin), eq(finalista), any())).thenReturn(r);
        }
    }

    private void simularError(String accion, RuntimeException e) {
        if ("archivar".equals(accion)) {
            when(cierreService.archivar(eq(admin), eq(finalista), any())).thenThrow(e);
        } else {
            when(cierreService.rechazar(eq(admin), eq(finalista), any())).thenThrow(e);
        }
    }

    @Test
    void sinSesion_da401() throws Exception {
        for (String accion : acciones) {
            mockMvc.perform(post(ruta(accion)).contentType(JSON).content(CUERPO))
                    .andExpect(status().isUnauthorized());
        }
        verify(cierreService, never()).archivar(any(), any(), any());
        verify(cierreService, never()).rechazar(any(), any(), any());
    }

    @Test
    void da200ConElSobreYSinIdsInternos() throws Exception {
        for (String accion : acciones) {
            UUID postulacion = UUID.randomUUID();
            String estado = "archivar".equals(accion) ? "archivada" : "no_adjudicada";
            String mensaje = "archivar".equals(accion) ? "Postulación archivada correctamente"
                    : "Postulación rechazada correctamente";
            simular(accion, new ResultadoAuditado<>(
                    new CierrePostulacionResponse(postulacion, estado, "No entregó sus documentos", Instant.now(),
                            "Admin DUBSS"), 1L, admin, 5L, Map.of(), true));

            mockMvc.perform(post(ruta(accion)).with(sesion()).contentType(JSON).content(CUERPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value(mensaje))
                    .andExpect(jsonPath("$.data.postulacion").value(postulacion.toString()))
                    .andExpect(jsonPath("$.data.estado").value(estado))
                    .andExpect(jsonPath("$.data.motivo").value("No entregó sus documentos"))
                    .andExpect(jsonPath("$.data.decididaPor").value("Admin DUBSS"))
                    .andExpect(jsonPath("$.data.id").doesNotExist());
        }
    }

    @Test
    void sinMotivoOConMasDe500Caracteres_da400ErrSys01() throws Exception {
        String[] cuerpos = {"{}", "{\"motivo\":\"  \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}"};
        for (String accion : acciones) {
            for (String cuerpo : cuerpos) {
                mockMvc.perform(post(ruta(accion)).with(sesion()).contentType(JSON).content(cuerpo))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("failed"))
                        .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
            }
        }
        verify(cierreService, never()).archivar(any(), any(), any());
        verify(cierreService, never()).rechazar(any(), any(), any());
    }

    @Test
    void conFinalistaInexistente_da404ErrFin01() throws Exception {
        for (String accion : acciones) {
            simularError(accion, new NotFoundException("ERR_FIN_01", "El finalista no existe"));

            mockMvc.perform(post(ruta(accion)).with(sesion()).contentType(JSON).content(CUERPO))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
        }
    }

    @Test
    void conConflicto_da409ErrArc01() throws Exception {
        for (String accion : acciones) {
            simularError(accion, new ConflictException(CierrePostulacionErrorCodes.ERR_ARC_01,
                    "La postulación ya no está calificada (estado actual: archivada)"));

            mockMvc.perform(post(ruta(accion)).with(sesion()).contentType(JSON).content(CUERPO))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ARC_01"));
        }
    }
}
