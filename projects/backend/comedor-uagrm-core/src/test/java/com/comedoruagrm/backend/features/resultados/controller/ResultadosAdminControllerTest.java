package com.comedoruagrm.backend.features.resultados.controller;

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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaResultadosResponse;
import com.comedoruagrm.backend.features.resultados.dto.DecisionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.dto.PublicacionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ResumenPublicacionResponse;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.FiltroDecisiones;
import com.comedoruagrm.backend.features.resultados.service.PublicacionResultadosService;
import com.comedoruagrm.backend.features.resultados.service.ResultadosAdminService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de los endpoints del Administrador del CU22 (resumen, decisiones y publicar) con los service
 * simulados. El `403` real por rol lo da `@PreAuthorize` en el service: aqui se simula con la excepcion que
 * lanza y se prueba de verdad en `PublicacionResultadosIntegracionTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResultadosAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultadosAdminService adminService;

    @MockitoBean
    private PublicacionResultadosService publicacionService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private String base() {
        return "/api/v1/convocatorias/" + convocatoria + "/resultados";
    }

    private ConvocatoriaResultadosResponse conv() {
        return new ConvocatoriaResultadosResponse(convocatoria, "Beca 2026-II", "CERRADA");
    }

    // ---- resumen ----

    @Test
    void resumen_da200ConElSobre() throws Exception {
        when(adminService.resumen(convocatoria)).thenReturn(new ResumenPublicacionResponse(conv(), 3, 2, 0, 6, 5,
                false, null, null, true));

        mockMvc.perform(get(base() + "/resumen").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.convocatoria.codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.adjudicados").value(3))
                .andExpect(jsonPath("$.data.noAdjudicados").value(2))
                .andExpect(jsonPath("$.data.calificadasPendientes").value(0))
                .andExpect(jsonPath("$.data.totalFinalistas").value(6))
                .andExpect(jsonPath("$.data.aAvisar").value(5))
                .andExpect(jsonPath("$.data.yaPublicada").value(false))
                .andExpect(jsonPath("$.data.puedePublicar").value(true))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void resumen_convocatoriaInexistente_da404ErrPri01() throws Exception {
        when(adminService.resumen(convocatoria)).thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(get(base() + "/resumen").with(sesion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- decisiones ----

    @Test
    void decisiones_da200ConArrayYMeta_yPasaLosParametrosAlService() throws Exception {
        UUID finalista = UUID.randomUUID();
        when(adminService.decisiones(eq(convocatoria), any(FiltroDecisiones.class), any(Pageable.class)))
                .thenReturn(new PaginaResultados<>(List.of(new DecisionResultadoResponse(finalista, "Ana", "Rojas",
                        "ADJUDICADA", new BigDecimal("80.5"), Instant.parse("2026-11-12T15:00:00Z"))),
                        new PageMeta(2, 10, 11, 2)));

        mockMvc.perform(get(base() + "/decisiones").with(sesion())
                        .param("filter[resultado]", "ADJUDICADA").param("search", "ana")
                        .param("page", "2").param("pageSize", "10").param("sort", "-apellidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].finalista").value(finalista.toString()))
                .andExpect(jsonPath("$.data[0].resultado").value("ADJUDICADA"))
                .andExpect(jsonPath("$.data[0].puntajeFinal").value(80.5))
                .andExpect(jsonPath("$.data[0].ci").doesNotExist())
                .andExpect(jsonPath("$.data[0].correo").doesNotExist())
                .andExpect(jsonPath("$.data[0].motivo").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(10))
                .andExpect(jsonPath("$.meta.totalItems").value(11))
                .andExpect(jsonPath("$.meta.totalPages").value(2));

        ArgumentCaptor<FiltroDecisiones> filtro = ArgumentCaptor.forClass(FiltroDecisiones.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(adminService).decisiones(eq(convocatoria), filtro.capture(), pageable.capture());
        org.assertj.core.api.Assertions.assertThat(filtro.getValue())
                .isEqualTo(new FiltroDecisiones("ADJUDICADA", "ana"));
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getSort().toList().get(0).isDescending())
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getSort().toList().get(0).getProperty())
                .isEqualTo("apellidos");
    }

    @Test
    void decisiones_porDefecto_pagina1Tamano20SinOrden() throws Exception {
        when(adminService.decisiones(eq(convocatoria), any(FiltroDecisiones.class), any(Pageable.class)))
                .thenReturn(new PaginaResultados<>(List.of(), new PageMeta(1, 20, 0, 0)));

        mockMvc.perform(get(base() + "/decisiones").with(sesion())).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(adminService).decisiones(eq(convocatoria), any(FiltroDecisiones.class), pageable.capture());
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageNumber()).isZero();
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void decisiones_conSortInvalidoDelService_da400ErrSys05() throws Exception {
        when(adminService.decisiones(eq(convocatoria), any(FiltroDecisiones.class), any(Pageable.class)))
                .thenThrow(new ValidationException("ERR_SYS_05", "No se puede ordenar por «ci»"));

        mockMvc.perform(get(base() + "/decisiones").with(sesion()).param("sort", "ci"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void decisiones_conFiltroNoSoportadoOParametrosInvalidos_da400SinLlamarAlService() throws Exception {
        mockMvc.perform(get(base() + "/decisiones").with(sesion()).param("filter[ci]", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(base() + "/decisiones").with(sesion()).param("sort", "a,b"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(base() + "/decisiones").with(sesion()).param("page", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get(base() + "/decisiones").with(sesion()).param("pageSize", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(adminService, never()).decisiones(any(), any(), any());
    }

    // ---- publicar ----

    @Test
    void publicar_da201ConElSobre() throws Exception {
        when(publicacionService.publicar(admin, convocatoria)).thenReturn(new ResultadoAuditado<>(
                new PublicacionResultadoResponse(
                        new ConvocatoriaResultadosResponse(convocatoria, "Beca 2026-II", "PUBLICADA"),
                        Instant.parse("2026-11-12T15:00:00Z"), "Admin DUBSS", 3, 2, 5),
                1L, admin, 9L, Map.of(), true));

        mockMvc.perform(post(base() + "/publicar").with(sesion()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Resultados publicados correctamente"))
                .andExpect(jsonPath("$.data.convocatoria.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.data.publicadoPor").value("Admin DUBSS"))
                .andExpect(jsonPath("$.data.adjudicados").value(3))
                .andExpect(jsonPath("$.data.noAdjudicados").value(2))
                .andExpect(jsonPath("$.data.avisados").value(5))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void publicar_convocatoriaInexistente_da404ErrPri01() throws Exception {
        when(publicacionService.publicar(admin, convocatoria))
                .thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(post(base() + "/publicar").with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    @Test
    void publicar_conflictos_da409ConCadaCodigo() throws Exception {
        for (String codigo : new String[] {"ERR_PUB_01", "ERR_PUB_02", "ERR_PUB_03"}) {
            org.mockito.Mockito.doThrow(new ConflictException(codigo, "Conflicto de publicación"))
                    .when(publicacionService).publicar(admin, convocatoria);

            mockMvc.perform(post(base() + "/publicar").with(sesion()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value(codigo))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ---- permisos ----

    @Test
    void sinSesion_las3RutasDan401() throws Exception {
        mockMvc.perform(get(base() + "/resumen")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base() + "/decisiones")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(base() + "/publicar")).andExpect(status().isUnauthorized());

        verify(adminService, never()).resumen(any());
        verify(publicacionService, never()).publicar(any(), any());
    }

    @Test
    void unNoAdministrador_da403ErrAuth07_enLas3Rutas() throws Exception {
        when(adminService.resumen(convocatoria)).thenThrow(new AccessDeniedException("sin rol"));
        when(adminService.decisiones(eq(convocatoria), any(FiltroDecisiones.class), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("sin rol"));
        when(publicacionService.publicar(admin, convocatoria)).thenThrow(new AccessDeniedException("sin rol"));

        mockMvc.perform(get(base() + "/resumen").with(sesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get(base() + "/decisiones").with(sesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(post(base() + "/publicar").with(sesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }
}
