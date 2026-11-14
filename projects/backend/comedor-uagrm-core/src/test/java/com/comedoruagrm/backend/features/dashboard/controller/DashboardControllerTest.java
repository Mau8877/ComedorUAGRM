package com.comedoruagrm.backend.features.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse.ConvocatoriaFiltro;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse.GestionFiltro;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.GestionResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.Totales;
import com.comedoruagrm.backend.features.dashboard.service.FiltrosDashboardService;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresPdfService;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresPdfService.PdfGenerado;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de los endpoints del dashboard (CU12) con los service simulados. El `403` real por rol lo da
 * `@PreAuthorize` en el service: aqui se simula con la excepcion que lanza y se prueba de verdad en
 * `DashboardIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    private static final String BASE = "/api/v1/dashboard";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FiltrosDashboardService filtros;

    @MockitoBean
    private IndicadoresService indicadores;

    @MockitoBean
    private IndicadoresPdfService pdf;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID directivo = UUID.randomUUID();
    private final UUID gestion = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(directivo))
                .thenReturn(Optional.of(new DatosUsuario(directivo, "DIRECTIVO", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(directivo.toString()));
    }

    private IndicadoresResponse respuesta() {
        return new IndicadoresResponse(new GestionResumen(gestion, "2026-II"), null, new Totales(10, 4, 2, null),
                List.of(new IndicadorMensual("2026-09", 10, 4, 2, null)), Instant.parse("2026-11-13T06:00:00Z"),
                false);
    }

    @Test
    void filtros_da200ConElCatalogoComoObjeto() throws Exception {
        when(filtros.obtener()).thenReturn(new FiltrosDashboardResponse(List.of(new GestionFiltro(gestion, "2026-II",
                true, List.of(new ConvocatoriaFiltro(convocatoria, "Beca 2026"))))));

        mockMvc.perform(get(BASE + "/filtros").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.gestiones[0].codigo").value(gestion.toString()))
                .andExpect(jsonPath("$.data.gestiones[0].etiqueta").value("2026-II"))
                .andExpect(jsonPath("$.data.gestiones[0].vigente").value(true))
                .andExpect(jsonPath("$.data.gestiones[0].convocatorias[0].codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.gestiones[0].convocatorias[0].nombre").value("Beca 2026"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void indicadores_da200_yPasaLosFiltrosAlService() throws Exception {
        when(indicadores.consultar(gestion, convocatoria)).thenReturn(respuesta());

        mockMvc.perform(get(BASE + "/indicadores").with(sesion()).param("filter[gestion]", gestion.toString())
                        .param("filter[convocatoria]", convocatoria.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.gestion.etiqueta").value("2026-II"))
                .andExpect(jsonPath("$.data.convocatoria").doesNotExist())
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(10))
                .andExpect(jsonPath("$.data.totales.finalistas").value(4))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(2))
                .andExpect(jsonPath("$.data.porMes[0].mes").value("2026-09"))
                .andExpect(jsonPath("$.data.porMes[0].asistencia").doesNotExist())
                .andExpect(jsonPath("$.data.actualizadoEn").value("2026-11-13T06:00:00Z"))
                .andExpect(jsonPath("$.data.sinDatos").value(false))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void indicadores_sinFiltros_losPasaNulos() throws Exception {
        when(indicadores.consultar(null, null)).thenReturn(respuesta());

        mockMvc.perform(get(BASE + "/indicadores").with(sesion())).andExpect(status().isOk());

        verify(indicadores).consultar(isNull(), isNull());
    }

    @Test
    void indicadores_conUuidInvalido_da400ErrSys05_enGestionYConvocatoria() throws Exception {
        mockMvc.perform(get(BASE + "/indicadores").with(sesion()).param("filter[gestion]", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(BASE + "/indicadores").with(sesion()).param("filter[convocatoria]", "123"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(BASE + "/indicadores/pdf").with(sesion()).param("filter[gestion]", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));

        verifyNoInteractions(indicadores, pdf);
    }

    @Test
    void indicadores_conUnFiltroNoSoportado_da400ErrSys05() throws Exception {
        mockMvc.perform(get(BASE + "/indicadores").with(sesion()).param("filter[rol]", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void indicadores_conGestionOConvocatoriaInexistente_da404ErrDas01() throws Exception {
        when(indicadores.consultar(eq(gestion), any())).thenThrow(new NotFoundException("ERR_DAS_01", "No existe"));

        mockMvc.perform(get(BASE + "/indicadores").with(sesion()).param("filter[gestion]", gestion.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").value("ERR_DAS_01"));
    }

    @Test
    void pdf_da200ConElArchivoAdjunto_yElUsuarioDelToken() throws Exception {
        byte[] bytes = {'%', 'P', 'D', 'F', '-'};
        when(pdf.generar(directivo, gestion, null))
                .thenReturn(new PdfGenerado("indicadores-2026-ii-20261113.pdf", bytes));

        mockMvc.perform(get(BASE + "/indicadores/pdf").with(sesion()).param("filter[gestion]", gestion.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"indicadores-2026-ii-20261113.pdf\""))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void pdf_sinDatos_da400ErrDas02ConElSobre() throws Exception {
        when(pdf.generar(any(), any(), any())).thenThrow(new ValidationException("ERR_DAS_02", "Sin datos"));

        mockMvc.perform(get(BASE + "/indicadores/pdf").with(sesion()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_DAS_02"));
    }

    @Test
    void pdf_conGestionInexistente_da404ErrDas01() throws Exception {
        when(pdf.generar(any(), any(), any())).thenThrow(new NotFoundException("ERR_DAS_01", "No existe"));

        mockMvc.perform(get(BASE + "/indicadores/pdf").with(sesion()).param("filter[gestion]", gestion.toString()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_DAS_01"));
    }

    @Test
    void sinSesion_todasLasRutasDan401() throws Exception {
        mockMvc.perform(get(BASE + "/filtros")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/indicadores")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/indicadores/pdf")).andExpect(status().isUnauthorized());

        verifyNoInteractions(filtros, indicadores, pdf);
    }

    @Test
    void quienNoEsDirectivo_da403ErrAuth07_enTodasLasRutas() throws Exception {
        when(filtros.obtener()).thenThrow(new AccessDeniedException("sin rol"));
        when(indicadores.consultar(any(), any())).thenThrow(new AccessDeniedException("sin rol"));
        when(pdf.generar(any(), any(), any())).thenThrow(new AccessDeniedException("sin rol"));

        for (String ruta : List.of("/filtros", "/indicadores", "/indicadores/pdf")) {
            mockMvc.perform(get(BASE + ruta).with(sesion()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
    }

    @Test
    void soloHayLecturas_noHayRutasParaEscribirNiRefrescar() throws Exception {
        mockMvc.perform(post(BASE + "/indicadores").with(sesion())).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post(BASE + "/refrescar").with(sesion()))
                .andExpect(status().is4xxClientError());

        verify(indicadores, never()).consultar(any(), any());
    }
}
