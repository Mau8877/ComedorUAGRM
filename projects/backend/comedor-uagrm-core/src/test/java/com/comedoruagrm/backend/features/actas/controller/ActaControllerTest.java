package com.comedoruagrm.backend.features.actas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.actas.dto.AccesoActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaPendienteActaResponse;
import com.comedoruagrm.backend.features.actas.service.ActaAdjudicacionService;
import com.comedoruagrm.backend.features.actas.service.ActaAdjudicacionService.VistaPreviaActa;
import com.comedoruagrm.backend.features.actas.service.ConsultaActasService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de los endpoints del acta de adjudicacion (CU08) con los service simulados. El `403` real por
 * rol lo da `@PreAuthorize` en el service: aqui se simula con la excepcion que lanza y se prueba de verdad en
 * `ActaAdjudicacionIntegracionTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActaControllerTest {

    private static final Instant GENERADA = Instant.parse("2026-11-13T15:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActaAdjudicacionService generacionService;

    @MockitoBean
    private ConsultaActasService consultaService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();
    private final UUID acta = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private String rutaActa() {
        return "/api/v1/convocatorias/" + convocatoria + "/acta";
    }

    private ActaResponse respuesta() {
        return new ActaResponse(acta, "001/2026", new ConvocatoriaActaResponse(convocatoria, "Beca 2026-II"),
                "2026-II", 3, GENERADA, "Admin DUBSS");
    }

    // ---- listar ----

    @Test
    void listar_da200ConArrayYMeta_yPasaLosParametrosAlService() throws Exception {
        when(consultaService.listar(eq("001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(1, 10), 11));

        mockMvc.perform(get("/api/v1/actas").with(sesion()).param("search", "001").param("page", "2")
                        .param("pageSize", "10").param("sort", "-numero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].codigo").value(acta.toString()))
                .andExpect(jsonPath("$.data[0].numero").value("001/2026"))
                .andExpect(jsonPath("$.data[0].convocatoria.codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data[0].convocatoria.nombre").value("Beca 2026-II"))
                .andExpect(jsonPath("$.data[0].gestion").value("2026-II"))
                .andExpect(jsonPath("$.data[0].adjudicados").value(3))
                .andExpect(jsonPath("$.data[0].generadaPor").value("Admin DUBSS"))
                .andExpect(jsonPath("$.data[0].storagePath").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(10))
                .andExpect(jsonPath("$.meta.totalItems").value(11))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(consultaService).listar(eq("001"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().toList().get(0).getProperty()).isEqualTo("numero");
        assertThat(pageable.getValue().getSort().toList().get(0).isDescending()).isTrue();
    }

    @Test
    void listar_porDefecto_pagina1Tamano20SinOrden() throws Exception {
        when(consultaService.listar(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/actas").with(sesion())).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(consultaService).listar(eq(null), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void listar_conSortNoSoportadoDelService_da400ErrSys05() throws Exception {
        when(consultaService.listar(any(), any(Pageable.class)))
                .thenThrow(new ValidationException("ERR_SYS_05", "No se puede ordenar por «ci»"));

        mockMvc.perform(get("/api/v1/actas").with(sesion()).param("sort", "ci"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void listarYPendientes_conFiltroOParametrosInvalidos_da400SinLlamarAlService() throws Exception {
        mockMvc.perform(get("/api/v1/actas").with(sesion()).param("filter[numero]", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/actas/pendientes").with(sesion()).param("filter[nombre]", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/actas").with(sesion()).param("sort", "a,b"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/actas").with(sesion()).param("page", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get("/api/v1/actas/pendientes").with(sesion()).param("pageSize", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verifyNoInteractions(consultaService);
    }

    // ---- pendientes ----

    @Test
    void pendientes_da200ConArrayYMeta() throws Exception {
        when(consultaService.pendientes(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(new ConvocatoriaPendienteActaResponse(convocatoria, "Beca 2026-II",
                        Instant.parse("2026-11-12T12:00:00Z"), 3)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/actas/pendientes").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data[0].nombre").value("Beca 2026-II"))
                .andExpect(jsonPath("$.data[0].publicadoEn").exists())
                .andExpect(jsonPath("$.data[0].adjudicados").value(3))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    // ---- obtener ----

    @Test
    void obtener_da200ConElActa() throws Exception {
        when(consultaService.obtener(convocatoria)).thenReturn(respuesta());

        mockMvc.perform(get(rutaActa()).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.numero").value("001/2026"))
                .andExpect(jsonPath("$.data.generadaEn").exists())
                .andExpect(jsonPath("$.data.url").doesNotExist())
                .andExpect(jsonPath("$.data.storagePath").doesNotExist());
    }

    @Test
    void obtener_sinActa_da404ErrAct03() throws Exception {
        when(consultaService.obtener(convocatoria))
                .thenThrow(new NotFoundException("ERR_ACT_03", "La convocatoria todavía no tiene acta"));

        mockMvc.perform(get(rutaActa()).with(sesion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_ACT_03"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void obtener_convocatoriaInexistente_da404ErrPri01() throws Exception {
        when(consultaService.obtener(convocatoria))
                .thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(get(rutaActa()).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- descargar ----

    @Test
    void descargar_da200ConLaDireccionTemporal() throws Exception {
        when(consultaService.descargar(admin, convocatoria)).thenReturn(new ResultadoAuditado<>(
                new AccesoActaResponse("http://minio.local/firmada", Instant.parse("2026-11-13T15:35:00Z")),
                1L, admin, 50L, Map.of(), true));

        mockMvc.perform(get(rutaActa() + "/descarga").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.url").value("http://minio.local/firmada"))
                .andExpect(jsonPath("$.data.expiraEn").exists());
    }

    @Test
    void descargar_sinActaOArchivoAusente_da404ConCadaCodigo() throws Exception {
        for (String codigo : new String[] {"ERR_ACT_03", "ERR_ACT_04", "ERR_PRI_01"}) {
            org.mockito.Mockito.doThrow(new NotFoundException(codigo, "No encontrado"))
                    .when(consultaService).descargar(admin, convocatoria);

            mockMvc.perform(get(rutaActa() + "/descarga").with(sesion()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value(codigo));
        }
    }

    // ---- vista previa ----

    @Test
    void vistaPrevia_da200ConElPdfEnLineaYSinSobre() throws Exception {
        byte[] pdf = {'%', 'P', 'D', 'F', '-'};
        when(generacionService.vistaPrevia(convocatoria)).thenReturn(new VistaPreviaActa("acta-borrador-x.pdf", pdf));

        mockMvc.perform(get(rutaActa() + "/vista-previa").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"acta-borrador-x.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void vistaPrevia_enError_respondeConElSobre() throws Exception {
        for (String codigo : new String[] {"ERR_ACT_01", "ERR_ACT_02"}) {
            org.mockito.Mockito.doThrow(new ConflictException(codigo, "Conflicto"))
                    .when(generacionService).vistaPrevia(convocatoria);

            mockMvc.perform(get(rutaActa() + "/vista-previa").with(sesion()))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value(codigo));
        }
        org.mockito.Mockito.doThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"))
                .when(generacionService).vistaPrevia(convocatoria);
        mockMvc.perform(get(rutaActa() + "/vista-previa").with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- generar ----

    @Test
    void generar_da201ConElSobre() throws Exception {
        when(generacionService.generar(admin, convocatoria))
                .thenReturn(new ResultadoAuditado<>(respuesta(), 1L, admin, 50L, Map.of(), true));

        mockMvc.perform(post(rutaActa()).with(sesion()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Acta generada correctamente"))
                .andExpect(jsonPath("$.data.numero").value("001/2026"))
                .andExpect(jsonPath("$.data.convocatoria.nombre").value("Beca 2026-II"))
                .andExpect(jsonPath("$.data.adjudicados").value(3))
                .andExpect(jsonPath("$.data.storagePath").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void generar_conflictosYNoEncontrada_dan409Y404ConCadaCodigo() throws Exception {
        for (String codigo : new String[] {"ERR_ACT_01", "ERR_ACT_02"}) {
            org.mockito.Mockito.doThrow(new ConflictException(codigo, "Conflicto"))
                    .when(generacionService).generar(admin, convocatoria);

            mockMvc.perform(post(rutaActa()).with(sesion()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value(codigo))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
        org.mockito.Mockito.doThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"))
                .when(generacionService).generar(admin, convocatoria);
        mockMvc.perform(post(rutaActa()).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- el acta no se regenera ni se elimina ----

    @Test
    void putPatchYDeleteSobreElActa_dan405() throws Exception {
        mockMvc.perform(put(rutaActa()).with(sesion())).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch(rutaActa()).with(sesion())).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(rutaActa()).with(sesion())).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/actas/" + acta).with(sesion()))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(generacionService, consultaService);
    }

    // ---- permisos ----

    @Test
    void sinSesion_todasLasRutasDan401() throws Exception {
        mockMvc.perform(get("/api/v1/actas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/actas/pendientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaActa())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaActa() + "/descarga")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaActa() + "/vista-previa")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(rutaActa())).andExpect(status().isUnauthorized());

        verify(generacionService, never()).generar(any(), any());
        verifyNoInteractions(consultaService);
    }

    @Test
    void unNoAdministrador_da403ErrAuth07_enTodasLasRutas() throws Exception {
        when(consultaService.listar(any(), any(Pageable.class))).thenThrow(new AccessDeniedException("sin rol"));
        when(consultaService.pendientes(any(Pageable.class))).thenThrow(new AccessDeniedException("sin rol"));
        when(consultaService.obtener(convocatoria)).thenThrow(new AccessDeniedException("sin rol"));
        when(consultaService.descargar(admin, convocatoria)).thenThrow(new AccessDeniedException("sin rol"));
        when(generacionService.vistaPrevia(convocatoria)).thenThrow(new AccessDeniedException("sin rol"));
        when(generacionService.generar(admin, convocatoria)).thenThrow(new AccessDeniedException("sin rol"));

        for (var peticion : List.of(get("/api/v1/actas"), get("/api/v1/actas/pendientes"), get(rutaActa()),
                get(rutaActa() + "/descarga"), get(rutaActa() + "/vista-previa"))) {
            mockMvc.perform(peticion.with(sesion()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
        mockMvc.perform(post(rutaActa()).with(sesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }
}
