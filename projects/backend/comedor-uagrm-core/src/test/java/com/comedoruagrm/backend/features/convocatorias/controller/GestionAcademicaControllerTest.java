package com.comedoruagrm.backend.features.convocatorias.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaResponse;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.service.GestionAcademicaService;
import com.comedoruagrm.backend.features.convocatorias.service.GestionAcademicaService.PaginaGestiones;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de /api/v1/gestiones-academicas con el servicio simulado (ver ConvocatoriaControllerTest). */
@SpringBootTest
@AutoConfigureMockMvc
class GestionAcademicaControllerTest {

    private static final String BODY = """
            {"etiqueta":"2026-II","fechaInicio":"2026-07-01","fechaFin":"2026-12-20"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionAcademicaService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID adminCodigo = UUID.randomUUID();
    private final UUID gestionCodigo = UUID.randomUUID();
    private final GestionAcademicaResponse gestion = new GestionAcademicaResponse(
            gestionCodigo, "2026-II", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));

    @BeforeEach
    void sesionDelAdministrador() {
        when(provider.buscarPorCodigo(adminCodigo))
                .thenReturn(Optional.of(new DatosUsuario(adminCodigo, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor administrador() {
        return jwt().jwt(j -> j.subject(adminCodigo.toString()));
    }

    private ResultadoAuditado<GestionAcademicaResponse> resultado() {
        return new ResultadoAuditado<>(gestion, 1L, adminCodigo, 5L, Map.of(), true);
    }

    @Test
    void listar_devuelve200ConArrayYMeta() throws Exception {
        when(service.listar(1, 20, null, null)).thenReturn(
                new PaginaGestiones(List.of(gestion), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/gestiones-academicas").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].etiqueta").value("2026-II"))
                .andExpect(jsonPath("$.data[0].fechaInicio").value("2026-07-01"))
                .andExpect(jsonPath("$.meta.page").value(1));
    }

    @Test
    void listar_conUnFiltro_devuelve400PorqueNoAdmiteFiltros() throws Exception {
        mockMvc.perform(get("/api/v1/gestiones-academicas").with(administrador()).param("filter[rol]", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        verify(service, never()).listar(1, 20, null, null);
    }

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/gestiones-academicas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(service.crear(eq(adminCodigo), any())).thenReturn(resultado());

        mockMvc.perform(post("/api/v1/gestiones-academicas").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.codigo").value(gestionCodigo.toString()));
    }

    @Test
    void crear_conEtiquetaRepetida_devuelve409ErrCv12() throws Exception {
        when(service.crear(eq(adminCodigo), any())).thenThrow(
                new ConflictException(ConvocatoriaErrorCodes.ERR_CV_12, "Ya existe una gestión con esa etiqueta"));

        mockMvc.perform(post("/api/v1/gestiones-academicas").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_CV_12"));
    }

    @Test
    void crear_conCuerpoInvalido_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post("/api/v1/gestiones-academicas").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etiqueta\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void editar_devuelve200() throws Exception {
        when(service.editar(eq(adminCodigo), eq(gestionCodigo), any())).thenReturn(resultado());

        mockMvc.perform(put("/api/v1/gestiones-academicas/" + gestionCodigo).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.etiqueta").value("2026-II"));
    }
}
