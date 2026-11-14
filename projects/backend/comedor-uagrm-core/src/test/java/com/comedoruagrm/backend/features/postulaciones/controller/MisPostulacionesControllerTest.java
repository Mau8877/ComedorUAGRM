package com.comedoruagrm.backend.features.postulaciones.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.ConvocatoriaResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.FinalistaPostulanteResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.GestionAcademicaResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionDetalleResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ObservacionesResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PasoLineaTiempoResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.SituacionPaso;
import com.comedoruagrm.backend.features.postulaciones.service.MisPostulacionesService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de «Mis postulaciones» (CU04) con el service simulado. El `403` real por rol lo da
 * `@PreAuthorize` en el service: aqui se simula con la excepcion que lanza y se prueba de verdad en
 * `MisPostulacionesIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MisPostulacionesControllerTest {

    private static final Instant ENVIO = Instant.parse("2026-11-01T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MisPostulacionesService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID postulante = UUID.randomUUID();
    private final UUID postulacion = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(postulante))
                .thenReturn(Optional.of(new DatosUsuario(postulante, "POSTULANTE", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(postulante.toString()));
    }

    private MiPostulacionResumenResponse resumen() {
        return new MiPostulacionResumenResponse(postulacion, new ConvocatoriaResumenResponse(UUID.randomUUID(), "Beca"),
                new GestionAcademicaResumenResponse(UUID.randomUUID(), "2026-II"), ENVIO, "calificada", (short) 1);
    }

    // ---- GET /mias ----

    @Test
    void mias_da200ConArrayYMeta_yPasaLosParametrosAlService() throws Exception {
        UUID gestion = UUID.randomUUID();
        when(service.listar(eq(postulante), eq(gestion.toString()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resumen()), PageRequest.of(1, 10), 11));

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion()).param("filter[gestionAcademica]", gestion.toString())
                        .param("page", "2").param("pageSize", "10").param("sort", "fechaEnvio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].codigo").value(postulacion.toString()))
                .andExpect(jsonPath("$.data[0].convocatoria.nombre").value("Beca"))
                .andExpect(jsonPath("$.data[0].gestionAcademica.nombre").value("2026-II"))
                .andExpect(jsonPath("$.data[0].estado").value("calificada"))
                .andExpect(jsonPath("$.data[0].numeroEnvios").value(1))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(10))
                .andExpect(jsonPath("$.meta.totalItems").value(11))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(eq(postulante), eq(gestion.toString()), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().toList().get(0).getProperty()).isEqualTo("fechaEnvio");
        assertThat(pageable.getValue().getSort().toList().get(0).isAscending()).isTrue();
    }

    @Test
    void mias_porDefecto_pagina1Tamano20SinOrdenNiFiltro() throws Exception {
        when(service.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(eq(postulante), eq(null), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void mias_conSortOFiltroInvalidoDelService_da400ErrSys05() throws Exception {
        when(service.listar(any(), any(), any(Pageable.class)))
                .thenThrow(new ValidationException("ERR_SYS_05", "No se puede ordenar por «estado»"));

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion()).param("sort", "estado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void mias_conFiltroNoSoportadoOParametrosInvalidos_da400SinLlamarAlService() throws Exception {
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion()).param("filter[estado]", "pendiente"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion()).param("sort", "a,b"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion()).param("page", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verifyNoInteractions(service);
    }

    // ---- GET /{codigo} ----

    @Test
    void detalle_da200ConElSobreYLosBloquesQueCorresponden() throws Exception {
        when(service.detalle(postulante, postulacion)).thenReturn(new MiPostulacionDetalleResponse(postulacion,
                new ConvocatoriaResumenResponse(UUID.randomUUID(), "Beca"),
                new GestionAcademicaResumenResponse(UUID.randomUUID(), "2026-II"), ENVIO, "devuelta", (short) 2,
                List.of(new PasoLineaTiempoResponse("pendiente", ENVIO, SituacionPaso.COMPLETADO),
                        new PasoLineaTiempoResponse("resultado", null, SituacionPaso.PENDIENTE)),
                new ObservacionesResponse("Falta el CI", ENVIO), null,
                new FinalistaPostulanteResponse(LocalDate.parse("2026-11-20"), false, "CI"), null, true));

        mockMvc.perform(get("/api/v1/postulaciones/" + postulacion).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.codigo").value(postulacion.toString()))
                .andExpect(jsonPath("$.data.estado").value("devuelta"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[0].estado").value("pendiente"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[0].situacion").value("COMPLETADO"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[1].situacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.observaciones.texto").value("Falta el CI"))
                .andExpect(jsonPath("$.data.finalista.plazoEntrega").value("2026-11-20"))
                .andExpect(jsonPath("$.data.finalista.documentacionRecibida").value(false))
                .andExpect(jsonPath("$.data.puedeCorregir").value(true))
                .andExpect(jsonPath("$.data.usuario").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void detalle_deUnaPostulacionAjenaOInexistente_da404ErrPos01() throws Exception {
        when(service.detalle(postulante, postulacion))
                .thenThrow(new NotFoundException("ERR_POS_01", "La postulación no existe"));

        mockMvc.perform(get("/api/v1/postulaciones/" + postulacion).with(sesion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_POS_01"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void mias_noChocaConLaRutaDelDetalle() throws Exception {
        when(service.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion())).andExpect(status().isOk());

        verify(service).listar(any(), any(), any(Pageable.class));
        verify(service, org.mockito.Mockito.never()).detalle(any(), any());
    }

    // ---- permisos ----

    @Test
    void sinSesion_lasDosRutasDan401() throws Exception {
        mockMvc.perform(get("/api/v1/postulaciones/mias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/postulaciones/" + postulacion)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void unNoPostulante_da403ErrAuth07_enLasDosRutas() throws Exception {
        when(service.listar(any(), any(), any(Pageable.class))).thenThrow(new AccessDeniedException("sin rol"));
        when(service.detalle(postulante, postulacion)).thenThrow(new AccessDeniedException("sin rol"));

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(sesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get("/api/v1/postulaciones/" + postulacion).with(sesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }
}
