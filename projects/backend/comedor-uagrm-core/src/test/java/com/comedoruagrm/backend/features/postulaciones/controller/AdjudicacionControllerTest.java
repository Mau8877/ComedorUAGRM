package com.comedoruagrm.backend.features.postulaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.comedoruagrm.backend.features.postulaciones.dto.DecisionAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PanelAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PanelAdjudicacionResponse.ConvocatoriaPanel;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.service.AdjudicacionService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP del panel de adjudicacion y de adjudicar / no adjudicar (CU05) con el service simulado.
 *
 * * El `403` por rol lo comprueba el service (`@PreAuthorize`), simulado aqui: se prueba en
 * * `AdjudicacionIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdjudicacionControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdjudicacionService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();
    private final UUID finalista = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private String rutaPanel() {
        return "/api/v1/convocatorias/" + convocatoria + "/adjudicacion";
    }

    private String ruta(String accion) {
        return "/api/v1/finalistas/" + finalista + "/" + accion;
    }

    private DecisionAdjudicacionResponse decision(String resultado, String estado) {
        return new DecisionAdjudicacionResponse(finalista, UUID.randomUUID(), resultado, estado,
                new BigDecimal("72.50"), Instant.now(), "Admin DUBSS", 4);
    }

    // ---- panel ----

    @Test
    void panel_sinSesion_da401() throws Exception {
        mockMvc.perform(get(rutaPanel())).andExpect(status().isUnauthorized());
    }

    @Test
    void panel_conCuposYMinimoDefinidos_da200ConElSobre() throws Exception {
        when(service.panel(convocatoria)).thenReturn(new PanelAdjudicacionResponse(
                new ConvocatoriaPanel(convocatoria, "Beca 2026", "CERRADA"), 10, new BigDecimal("55.00"), 3, 1, 7,
                12, 20, 30, false, true));

        mockMvc.perform(get(rutaPanel()).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Panel de adjudicación obtenido correctamente"))
                .andExpect(jsonPath("$.data.convocatoria.codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.convocatoria.estado").value("CERRADA"))
                .andExpect(jsonPath("$.data.cupos").value(10))
                .andExpect(jsonPath("$.data.puntajeMinimo").value(55.0))
                .andExpect(jsonPath("$.data.adjudicados").value(3))
                .andExpect(jsonPath("$.data.noAdjudicados").value(1))
                .andExpect(jsonPath("$.data.cuposDisponibles").value(7))
                .andExpect(jsonPath("$.data.cantidadFinalistas").value(12))
                .andExpect(jsonPath("$.data.totalFinalistas").value(20))
                .andExpect(jsonPath("$.data.calificadasSinSeleccionar").value(30))
                .andExpect(jsonPath("$.data.resultadosPublicados").value(false))
                .andExpect(jsonPath("$.data.decisionesHabilitadas").value(true))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void panel_sinCuposNiMinimo_losEntregaNulosYNoHabilitaLasDecisiones() throws Exception {
        when(service.panel(convocatoria)).thenReturn(new PanelAdjudicacionResponse(
                new ConvocatoriaPanel(convocatoria, "Beca 2026", "ABIERTA"), null, null, 0, 0, null, null, 0, 5,
                false, false));

        mockMvc.perform(get(rutaPanel()).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cupos").doesNotExist())
                .andExpect(jsonPath("$.data.puntajeMinimo").doesNotExist())
                .andExpect(jsonPath("$.data.cuposDisponibles").doesNotExist())
                .andExpect(jsonPath("$.data.decisionesHabilitadas").value(false));
    }

    @Test
    void panel_conConvocatoriaInexistente_da404ErrPri01() throws Exception {
        when(service.panel(convocatoria)).thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(get(rutaPanel()).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- adjudicar ----

    @Test
    void adjudicar_sinSesion_da401() throws Exception {
        mockMvc.perform(post(ruta("adjudicar"))).andExpect(status().isUnauthorized());
        verify(service, never()).adjudicar(any(), any());
    }

    @Test
    void adjudicar_da200ConElSobreYSinIdsInternosNiCuerpo() throws Exception {
        when(service.adjudicar(eq(admin), eq(finalista))).thenReturn(new ResultadoAuditado<>(
                decision("ADJUDICADA", "adjudicada"), 1L, admin, 5L, Map.of(), true));

        mockMvc.perform(post(ruta("adjudicar")).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Beca adjudicada correctamente"))
                .andExpect(jsonPath("$.data.finalista").value(finalista.toString()))
                .andExpect(jsonPath("$.data.resultado").value("ADJUDICADA"))
                .andExpect(jsonPath("$.data.estado").value("adjudicada"))
                .andExpect(jsonPath("$.data.puntajeFinal").value(72.5))
                .andExpect(jsonPath("$.data.decididaPor").value("Admin DUBSS"))
                .andExpect(jsonPath("$.data.cuposDisponibles").value(4))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void adjudicar_conFinalistaInexistente_da404ErrFin01() throws Exception {
        when(service.adjudicar(eq(admin), eq(finalista)))
                .thenThrow(new NotFoundException("ERR_FIN_01", "El finalista no existe"));

        mockMvc.perform(post(ruta("adjudicar")).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    @Test
    void adjudicar_conCadaConflicto_da409ConSuCodigo() throws Exception {
        for (String codigo : new String[] {AdjudicacionErrorCodes.ERR_ADJ_01, AdjudicacionErrorCodes.ERR_ADJ_02,
            AdjudicacionErrorCodes.ERR_ADJ_03, AdjudicacionErrorCodes.ERR_ADJ_04,
            AdjudicacionErrorCodes.ERR_ADJ_05}) {
            when(service.adjudicar(eq(admin), eq(finalista))).thenThrow(new ConflictException(codigo, "Conflicto"));

            mockMvc.perform(post(ruta("adjudicar")).with(sesion()))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value(codigo));
        }
    }

    // ---- no adjudicar ----

    @Test
    void noAdjudicar_sinSesion_da401() throws Exception {
        mockMvc.perform(post(ruta("no-adjudicar")).contentType(JSON).content("{\"motivo\":\"No cumple\"}"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).noAdjudicar(any(), any(), any());
    }

    @Test
    void noAdjudicar_da200ConElSobre() throws Exception {
        when(service.noAdjudicar(eq(admin), eq(finalista), any())).thenReturn(new ResultadoAuditado<>(
                decision("NO_ADJUDICADA", "no_adjudicada"), 1L, admin, 5L, Map.of(), true));

        mockMvc.perform(post(ruta("no-adjudicar")).with(sesion()).contentType(JSON)
                .content("{\"motivo\":\"No cumple\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Postulación marcada como no adjudicada correctamente"))
                .andExpect(jsonPath("$.data.resultado").value("NO_ADJUDICADA"))
                .andExpect(jsonPath("$.data.estado").value("no_adjudicada"))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void noAdjudicar_sinMotivoSoloEspaciosOConMasDe500_da400ErrSys01() throws Exception {
        String[] cuerpos = {"{}", "{\"motivo\":\"  \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}"};
        for (String cuerpo : cuerpos) {
            mockMvc.perform(post(ruta("no-adjudicar")).with(sesion()).contentType(JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(service, never()).noAdjudicar(any(), any(), any());
    }

    @Test
    void noAdjudicar_conFinalistaInexistente_da404ErrFin01() throws Exception {
        when(service.noAdjudicar(eq(admin), eq(finalista), any()))
                .thenThrow(new NotFoundException("ERR_FIN_01", "El finalista no existe"));

        mockMvc.perform(post(ruta("no-adjudicar")).with(sesion()).contentType(JSON)
                .content("{\"motivo\":\"No cumple\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    @Test
    void noAdjudicar_conConflicto_da409ConSuCodigo() throws Exception {
        for (String codigo : new String[] {AdjudicacionErrorCodes.ERR_ADJ_01, AdjudicacionErrorCodes.ERR_ADJ_02,
            AdjudicacionErrorCodes.ERR_ADJ_05}) {
            when(service.noAdjudicar(eq(admin), eq(finalista), any()))
                    .thenThrow(new ConflictException(codigo, "Conflicto"));

            mockMvc.perform(post(ruta("no-adjudicar")).with(sesion()).contentType(JSON)
                    .content("{\"motivo\":\"No cumple\"}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value(codigo));
        }
    }
}
