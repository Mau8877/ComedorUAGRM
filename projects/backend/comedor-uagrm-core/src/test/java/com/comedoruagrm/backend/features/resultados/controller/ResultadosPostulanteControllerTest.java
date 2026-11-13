package com.comedoruagrm.backend.features.resultados.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.resultados.dto.AdjudicadoPublicoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaPublicadaResponse;
import com.comedoruagrm.backend.features.resultados.dto.MiResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.service.ResultadosPostulanteService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de las consultas del postulante del CU22 con el service simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class ResultadosPostulanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultadosPostulanteService postulanteService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID usuario = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(usuario))
                .thenReturn(Optional.of(new DatosUsuario(usuario, "POSTULANTE", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(usuario.toString()));
    }

    private String lista() {
        return "/api/v1/convocatorias/" + convocatoria + "/resultados";
    }

    // ---- convocatorias publicadas ----

    @Test
    void convocatorias_da200ConArrayYMeta() throws Exception {
        when(postulanteService.convocatoriasPublicadas(any(Pageable.class))).thenReturn(new PaginaResultados<>(
                List.of(new ConvocatoriaPublicadaResponse(convocatoria, "Beca 2026-II",
                        Instant.parse("2026-11-12T15:00:00Z"))), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/resultados/convocatorias").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data[0].nombre").value("Beca 2026-II"))
                .andExpect(jsonPath("$.data[0].publicadoEn").exists())
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20));
    }

    @Test
    void convocatorias_conPaginaInvalida_da400ErrSys01() throws Exception {
        mockMvc.perform(get("/api/v1/resultados/convocatorias").with(sesion()).param("page", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(postulanteService, never()).convocatoriasPublicadas(any());
    }

    // ---- lista de adjudicados ----

    @Test
    void adjudicados_da200ConSoloLosCincoCamposYMeta_yPasaSearchYOrden() throws Exception {
        when(postulanteService.adjudicados(eq(convocatoria), eq("ana"), any(Pageable.class)))
                .thenReturn(new PaginaResultados<>(List.of(new AdjudicadoPublicoResponse("Ana", "Rojas",
                        new BigDecimal("88.5"), "Ingeniería Informática", "Facultad de Ciencias")),
                        new PageMeta(1, 5, 1, 1)));

        mockMvc.perform(get(lista()).with(sesion()).param("search", "ana").param("pageSize", "5")
                        .param("sort", "-nota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.data[0].apellidos").value("Rojas"))
                .andExpect(jsonPath("$.data[0].nota").value(88.5))
                .andExpect(jsonPath("$.data[0].carrera").value("Ingeniería Informática"))
                .andExpect(jsonPath("$.data[0].facultad").value("Facultad de Ciencias"))
                .andExpect(jsonPath("$.data[0].ci").doesNotExist())
                .andExpect(jsonPath("$.data[0].correo").doesNotExist())
                .andExpect(jsonPath("$.meta.pageSize").value(5));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postulanteService).adjudicados(eq(convocatoria), eq("ana"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().toList().get(0).getProperty()).isEqualTo("nota");
        assertThat(pageable.getValue().getSort().toList().get(0).isDescending()).isTrue();
    }

    @Test
    void adjudicados_antesDePublicar_da404ErrPub04() throws Exception {
        when(postulanteService.adjudicados(eq(convocatoria), any(), any(Pageable.class)))
                .thenThrow(new NotFoundException("ERR_PUB_04", "Los resultados todavía no fueron publicados"));

        mockMvc.perform(get(lista()).with(sesion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_PUB_04"));
    }

    @Test
    void adjudicados_convocatoriaInexistente_da404ErrPri01() throws Exception {
        when(postulanteService.adjudicados(eq(convocatoria), any(), any(Pageable.class)))
                .thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(get(lista()).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    @Test
    void adjudicados_conSortInvalido_da400ErrSys05() throws Exception {
        when(postulanteService.adjudicados(eq(convocatoria), any(), any(Pageable.class)))
                .thenThrow(new ValidationException("ERR_SYS_05", "No se puede ordenar por «ci»"));

        mockMvc.perform(get(lista()).with(sesion()).param("sort", "ci"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void adjudicados_conFiltroNoSoportado_da400SinLlamarAlService() throws Exception {
        mockMvc.perform(get(lista()).with(sesion()).param("filter[resultado]", "NO_ADJUDICADA"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));

        verify(postulanteService, never()).adjudicados(any(), any(), any());
    }

    // ---- mi resultado ----

    @Test
    void mio_da200ConElResultadoDelUsuarioDelToken() throws Exception {
        when(postulanteService.miResultado(convocatoria, usuario)).thenReturn(new MiResultadoResponse(
                "NO_ADJUDICADA", new BigDecimal("40"), Instant.parse("2026-11-12T15:00:00Z")));

        mockMvc.perform(get(lista() + "/mio").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.resultado").value("NO_ADJUDICADA"))
                .andExpect(jsonPath("$.data.nota").value(40))
                .andExpect(jsonPath("$.data.motivo").doesNotExist());
    }

    @Test
    void mio_siElUsuarioNoEsFinalista_daResultadoNulo() throws Exception {
        when(postulanteService.miResultado(convocatoria, usuario))
                .thenReturn(new MiResultadoResponse(null, null, Instant.parse("2026-11-12T15:00:00Z")));

        mockMvc.perform(get(lista() + "/mio").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultado").isEmpty())
                .andExpect(jsonPath("$.data.publicadoEn").exists());
    }

    @Test
    void mio_antesDePublicar_da404ErrPub04() throws Exception {
        when(postulanteService.miResultado(convocatoria, usuario))
                .thenThrow(new NotFoundException("ERR_PUB_04", "Los resultados todavía no fueron publicados"));

        mockMvc.perform(get(lista() + "/mio").with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PUB_04"));
    }

    // ---- sesion ----

    @Test
    void sinSesion_las3RutasDan401() throws Exception {
        mockMvc.perform(get("/api/v1/resultados/convocatorias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(lista())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(lista() + "/mio")).andExpect(status().isUnauthorized());

        verify(postulanteService, never()).miResultado(any(), any());
    }
}
