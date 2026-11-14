package com.comedoruagrm.backend.features.expedientes.controller;

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
import java.util.List;
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
import com.comedoruagrm.backend.features.expedientes.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.expedientes.dto.AuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.ConvocatoriaExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.EntregaExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.FinalistaExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.PostulanteExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ResultadoAuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.expedientes.exception.ExpedienteErrorCodes;
import com.comedoruagrm.backend.features.expedientes.service.ExpedienteService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de /api/v1/finalistas/{codigo}/... (CU07) con el service simulado.
 *
 * * El `403` por rol lo comprueba el service (`@PreAuthorize`), que aqui esta simulado: se prueba en
 * * `ExpedienteIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpedienteControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpedienteService expedienteService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID finalista = UUID.randomUUID();
    private final UUID documento = UUID.randomUUID();
    private final String base = "/api/v1/finalistas/" + finalista;

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private ExpedienteResponse expediente() {
        return new ExpedienteResponse(
                new FinalistaExpediente(finalista, 1, null, new EntregaExpediente("PENDIENTE", null)),
                new PostulanteExpediente("Ana", "Rojas", "9802041", "Sistemas"),
                new ConvocatoriaExpediente(UUID.randomUUID(), "Conv"), null, List.of(), "calificada",
                new BigDecimal("55.50"), Map.of(), null, null, List.of());
    }

    private AuditoriaResponse auditoria() {
        return new AuditoriaResponse(UUID.randomUUID(), ResultadoAuditoria.VALIDADO, null,
                new BigDecimal("25.00"), Instant.now(), "Admin DUBSS");
    }

    @Test
    void obtener_sinSesion_da401() throws Exception {
        mockMvc.perform(get(base + "/expediente")).andExpect(status().isUnauthorized());
    }

    @Test
    void obtener_da200ConElSobreYSinIdsInternos() throws Exception {
        when(expedienteService.obtener(finalista)).thenReturn(expediente());

        mockMvc.perform(get(base + "/expediente").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.finalista.codigo").value(finalista.toString()))
                .andExpect(jsonPath("$.data.postulante.ci").value("9802041"))
                .andExpect(jsonPath("$.data.puntajeIa").value(55.5))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void obtener_conFinalistaInexistente_da404ErrFin01() throws Exception {
        when(expedienteService.obtener(finalista))
                .thenThrow(new NotFoundException("ERR_FIN_01", "El finalista no existe"));

        mockMvc.perform(get(base + "/expediente").with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    @Test
    void registrarAuditoria_creada_da201() throws Exception {
        when(expedienteService.registrarAuditoria(eq(admin), eq(finalista), any(), any())).thenReturn(
                new ResultadoAuditado<>(new ResultadoAuditoriaResponse(auditoria(), true), 1L, admin, 5L,
                        Map.of(), true));

        mockMvc.perform(post(base + "/auditorias").with(sesion()).contentType(JSON)
                .content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.creada").value(true))
                .andExpect(jsonPath("$.data.auditoria.resultado").value("VALIDADO"));
    }

    @Test
    void registrarAuditoria_repeticionIdempotente_da200() throws Exception {
        when(expedienteService.registrarAuditoria(eq(admin), eq(finalista), any(), any())).thenReturn(
                new ResultadoAuditado<>(new ResultadoAuditoriaResponse(auditoria(), false), 1L, admin, 5L,
                        Map.of(), false));

        mockMvc.perform(post(base + "/auditorias").with(sesion()).contentType(JSON)
                .content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.creada").value(false));
    }

    @Test
    void registrarAuditoria_pasaElHeaderIdempotencyKeyAlService() throws Exception {
        String clave = UUID.randomUUID().toString();
        when(expedienteService.registrarAuditoria(eq(admin), eq(finalista), eq(clave), any())).thenReturn(
                new ResultadoAuditado<>(new ResultadoAuditoriaResponse(auditoria(), false), 1L, admin, 5L,
                        Map.of(), false));

        mockMvc.perform(post(base + "/auditorias").with(sesion()).header("Idempotency-Key", clave)
                .contentType(JSON).content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.creada").value(false));
    }

    @Test
    void registrarAuditoria_conCuerpoVacioOSinResultado_da400ErrSys01() throws Exception {
        mockMvc.perform(post(base + "/auditorias").with(sesion()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(expedienteService, never()).registrarAuditoria(any(), any(), any(), any());
    }

    @Test
    void registrarAuditoria_conObservacionesDeMasDe500_da400ErrSys01() throws Exception {
        String larga = "x".repeat(501);

        mockMvc.perform(post(base + "/auditorias").with(sesion()).contentType(JSON)
                .content("{\"resultado\":\"OBSERVADO\",\"observaciones\":\"" + larga + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrarAuditoria_conUnConflicto_da409ConSuCodigo() throws Exception {
        when(expedienteService.registrarAuditoria(eq(admin), eq(finalista), any(), any())).thenThrow(
                new ConflictException(ExpedienteErrorCodes.ERR_EXP_01, "Falta la entrega física"));

        mockMvc.perform(post(base + "/auditorias").with(sesion()).contentType(JSON)
                .content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_01"));
    }

    @Test
    void registrarAuditoria_sinSesion_da401() throws Exception {
        mockMvc.perform(post(base + "/auditorias").contentType(JSON).content("{\"resultado\":\"VALIDADO\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoDocumento_da200ConUrlYExpiracion() throws Exception {
        when(expedienteService.accesoDocumento(finalista, documento))
                .thenReturn(new AccesoDocumentoResponse("http://localhost:9000/firmada", Instant.now()));

        mockMvc.perform(get(base + "/documentos/" + documento + "/acceso").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://localhost:9000/firmada"))
                .andExpect(jsonPath("$.data.expiraEn").exists());
    }

    @Test
    void accesoDocumento_conDocumentoAjeno_da404ErrExp05() throws Exception {
        when(expedienteService.accesoDocumento(finalista, documento)).thenThrow(
                new NotFoundException(ExpedienteErrorCodes.ERR_EXP_05, "El documento no existe"));

        mockMvc.perform(get(base + "/documentos/" + documento + "/acceso").with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_EXP_05"));
    }

    @Test
    void accesoDocumento_sinSesion_da401() throws Exception {
        mockMvc.perform(get(base + "/documentos/" + documento + "/acceso")).andExpect(status().isUnauthorized());
    }
}
