package com.comedoruagrm.backend.features.postulaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.postulaciones.dto.CarreraPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ExportarListaRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ResumenListaPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaPdfService;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaPdfService.PdfGenerado;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaService;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaService.PaginaListaPriorizada;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de /api/v1/lista-priorizada (CU06) con los services simulados. */
@SpringBootTest
@AutoConfigureMockMvc
class ListaPriorizadaControllerTest {

    private static final String RUTA = "/api/v1/lista-priorizada";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListaPriorizadaService listaService;

    @MockitoBean
    private ListaPriorizadaPdfService pdfService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    @Test
    void listar_sinSesion_da401() throws Exception {
        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void listar_devuelveElSobreConLaListaYSuMeta_yUsaLosValoresPorDefecto() throws Exception {
        PostulacionPriorizadaResponse fila = new PostulacionPriorizadaResponse(UUID.randomUUID(), 1, "Ana", "Rojas",
                "9802041", new CarreraPriorizadaResponse(UUID.randomUUID(), "Ing. Sistemas"),
                new BigDecimal("95.0"), Instant.parse("2026-09-01T10:00:00Z"), "calificada");
        when(listaService.listarPriorizados(eq(convocatoria), any(), any(), eq(1), eq(20), any()))
                .thenReturn(new PaginaListaPriorizada(List.of(fila), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].posicion").value(1))
                .andExpect(jsonPath("$.data[0].carrera.nombre").value("Ing. Sistemas"))
                .andExpect(jsonPath("$.data[0].puntaje").value(95.0))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20));
    }

    @Test
    void listar_sinLaConvocatoria_da400ErrSys05() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void listar_conUnFiltroNoSoportado_da400ErrPri02() throws Exception {
        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria + "&filter[estado]=calificada")
                .with(sesion()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PRI_02"));
    }

    @Test
    void listar_conUnaConvocatoriaInexistente_da404ErrPri01() throws Exception {
        when(listaService.listarPriorizados(eq(convocatoria), any(), any(), eq(1), eq(20), any()))
                .thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    @Test
    void resumen_devuelveLosTotales() throws Exception {
        when(listaService.resumir(convocatoria)).thenReturn(new ResumenListaPriorizadaResponse(140, 30));

        mockMvc.perform(get(RUTA + "/resumen?filter[convocatoriaId]=" + convocatoria).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCalificadas").value(140))
                .andExpect(jsonPath("$.data.totalPendientes").value(30));
    }

    @Test
    void convocatorias_devuelveLaLista() throws Exception {
        when(listaService.listarConvocatorias()).thenReturn(List.of());

        mockMvc.perform(get(RUTA + "/convocatorias").with(sesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void exportar_devuelveElPdfFueraDelSobreConSusCabeceras() throws Exception {
        byte[] pdf = "%PDF-1.4 prueba".getBytes(StandardCharsets.ISO_8859_1);
        when(pdfService.exportar(eq(admin), any(ExportarListaRequest.class))).thenReturn(
                new ResultadoAuditado<>(new PdfGenerado("lista-priorizada-x-20261106.pdf", pdf), 1L, admin, 2L,
                        java.util.Map.of(), true));

        mockMvc.perform(post(RUTA + "/exportaciones").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\"}".formatted(convocatoria)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("lista-priorizada-x-20261106.pdf")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().bytes(pdf));
        verify(pdfService).exportar(eq(admin), any(ExportarListaRequest.class));
    }

    @Test
    void exportar_sinLaConvocatoria_da400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/exportaciones").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void exportar_conLaListaVacia_da400ErrPri03EnElSobreDeError() throws Exception {
        when(pdfService.exportar(eq(admin), any(ExportarListaRequest.class)))
                .thenThrow(new ValidationException("ERR_PRI_03", "No hay postulaciones para exportar"));

        mockMvc.perform(post(RUTA + "/exportaciones").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\"}".formatted(convocatoria)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PRI_03"));
    }
}
