package com.comedoruagrm.backend.features.convocatorias.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResumenResponse;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionResumenResponse;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaEstadoService;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaService;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaService.PaginaConvocatorias;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de /api/v1/convocatorias con los servicios simulados, recorriendo la cadena de seguridad real
 * (el token se simula con spring-security-test). La falta de rol (`@PreAuthorize`) no se prueba aca porque con
 * el servicio simulado esa anotacion no corre: ver ConvocatoriaIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConvocatoriaControllerTest {

    private static final String BODY = """
            {"nombre":"Beca Comedor 2026-II","gestionCodigo":"%s","fechaApertura":"2026-08-01",
             "fechaCierre":"2026-08-31","cupos":100,"puntajeMinimo":40.5,"cantidadFinalistas":120,
             "documentos":[{"tipoDocumentoCodigo":"%s","obligatorio":true}]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConvocatoriaService service;

    @MockitoBean
    private ConvocatoriaEstadoService estadoService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID adminCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final UUID gestionCodigo = UUID.randomUUID();
    private final ConvocatoriaResponse detalle = new ConvocatoriaResponse(convocatoriaCodigo, "Beca Comedor 2026-II",
            new GestionResumenResponse(gestionCodigo, "2026-II"), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
            100, new BigDecimal("40.50"), 120, EstadoConvocatoria.BORRADOR, false, List.of());

    @BeforeEach
    void sesionDelAdministrador() {
        when(provider.buscarPorCodigo(adminCodigo))
                .thenReturn(Optional.of(new DatosUsuario(adminCodigo, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor administrador() {
        return jwt().jwt(j -> j.subject(adminCodigo.toString()));
    }

    private <T> ResultadoAuditado<T> resultado(T datos) {
        return new ResultadoAuditado<>(datos, 1L, adminCodigo, 5L, Map.of(), true);
    }

    private String body() {
        return BODY.formatted(gestionCodigo, UUID.randomUUID());
    }

    // ---- GET /convocatorias ----

    @Test
    void listar_devuelve200ConArrayYMeta() throws Exception {
        ConvocatoriaResumenResponse fila = new ConvocatoriaResumenResponse(convocatoriaCodigo, "Beca",
                new GestionResumenResponse(gestionCodigo, "2026-II"), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), 100, null, 120, EstadoConvocatoria.ABIERTA);
        when(service.listar(1, 20, null, null, null, null)).thenReturn(
                new PaginaConvocatorias(List.of(fila), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/convocatorias").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].estado").value("ABIERTA"))
                .andExpect(jsonPath("$.data[0].gestion.etiqueta").value("2026-II"))
                .andExpect(jsonPath("$.data[0].fechaApertura").value("2026-08-01"))
                .andExpect(jsonPath("$.meta.totalItems").value(1));
    }

    @Test
    void listar_pasaLosFiltrosYElOrdenAlServicio() throws Exception {
        when(service.listar(2, 10, "beca", EstadoConvocatoria.CERRADA, gestionCodigo, "-fechaCierre"))
                .thenReturn(new PaginaConvocatorias(List.of(), new PageMeta(2, 10, 0, 0)));

        mockMvc.perform(get("/api/v1/convocatorias").with(administrador())
                        .param("page", "2").param("pageSize", "10").param("search", "beca")
                        .param("filter[estado]", "CERRADA").param("filter[gestion]", gestionCodigo.toString())
                        .param("sort", "-fechaCierre"))
                .andExpect(status().isOk());

        verify(service).listar(2, 10, "beca", EstadoConvocatoria.CERRADA, gestionCodigo, "-fechaCierre");
    }

    @Test
    void listar_conUnFiltroNoSoportado_devuelve400ErrSys05() throws Exception {
        mockMvc.perform(get("/api/v1/convocatorias").with(administrador()).param("filter[cupos]", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void listar_conUnEstadoQueNoEsDelEnum_devuelve400ErrSys05() throws Exception {
        mockMvc.perform(get("/api/v1/convocatorias").with(administrador()).param("filter[estado]", "ABIERTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void listar_conUnaGestionQueNoEsUnCodigo_devuelve400ErrSys05() throws Exception {
        mockMvc.perform(get("/api/v1/convocatorias").with(administrador()).param("filter[gestion]", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void listar_sinToken_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/convocatorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    // ---- GET /convocatorias/{codigo} ----

    @Test
    void obtener_devuelve200ConElDetalle() throws Exception {
        when(service.obtener(convocatoriaCodigo)).thenReturn(detalle);

        mockMvc.perform(get("/api/v1/convocatorias/" + convocatoriaCodigo).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value(convocatoriaCodigo.toString()))
                .andExpect(jsonPath("$.data.tienePostulaciones").value(false))
                .andExpect(jsonPath("$.data.cantidadFinalistas").value(120));
    }

    @Test
    void obtener_inexistente_devuelve404ErrCv01() throws Exception {
        when(service.obtener(convocatoriaCodigo)).thenThrow(
                new NotFoundException(ConvocatoriaErrorCodes.ERR_CV_01, "La convocatoria indicada no existe"));

        mockMvc.perform(get("/api/v1/convocatorias/" + convocatoriaCodigo).with(administrador()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_CV_01"));
    }

    // ---- POST /convocatorias ----

    @Test
    void crear_devuelve201ConLaConvocatoria() throws Exception {
        when(service.crear(eq(adminCodigo), any())).thenReturn(resultado(detalle));

        mockMvc.perform(post("/api/v1/convocatorias").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.estado").value("BORRADOR"));
    }

    @Test
    void crear_conCamposOpcionalesOmitidos_devuelve201() throws Exception {
        when(service.crear(eq(adminCodigo), any())).thenReturn(resultado(detalle));

        mockMvc.perform(post("/api/v1/convocatorias").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","gestionCodigo":"%s","fechaApertura":"2026-08-01",
                                 "fechaCierre":"2026-08-31",
                                 "documentos":[{"tipoDocumentoCodigo":"%s","obligatorio":true}]}"""
                                .formatted(gestionCodigo, UUID.randomUUID())))
                .andExpect(status().isCreated());
    }

    @Test
    void crear_conCuerpoInvalido_devuelve400ErrSys01SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post("/api/v1/convocatorias").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"documentos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    // ---- PUT /convocatorias/{codigo} ----

    @Test
    void editar_devuelve200() throws Exception {
        when(service.editar(eq(adminCodigo), eq(convocatoriaCodigo), any())).thenReturn(resultado(detalle));

        mockMvc.perform(put("/api/v1/convocatorias/" + convocatoriaCodigo).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value(convocatoriaCodigo.toString()));
    }

    @Test
    void editar_conPostulaciones_devuelve409ErrCv09() throws Exception {
        when(service.editar(eq(adminCodigo), eq(convocatoriaCodigo), any())).thenThrow(new ConflictException(
                ConvocatoriaErrorCodes.ERR_CV_09, "La convocatoria ya tiene postulaciones"));

        mockMvc.perform(put("/api/v1/convocatorias/" + convocatoriaCodigo).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_CV_09"));
    }

    // ---- POST /abrir y /cerrar ----

    @Test
    void abrir_devuelve200() throws Exception {
        when(estadoService.abrir(adminCodigo, convocatoriaCodigo)).thenReturn(resultado(detalle));

        mockMvc.perform(post("/api/v1/convocatorias/" + convocatoriaCodigo + "/abrir").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void abrir_conOtraAbiertaEnLaGestion_devuelve409ErrCv07() throws Exception {
        when(estadoService.abrir(adminCodigo, convocatoriaCodigo)).thenThrow(new ConflictException(
                ConvocatoriaErrorCodes.ERR_CV_07, "Ya hay otra convocatoria abierta en esa gestión académica"));

        mockMvc.perform(post("/api/v1/convocatorias/" + convocatoriaCodigo + "/abrir").with(administrador()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_CV_07"));
    }

    @Test
    void cerrar_devuelve200() throws Exception {
        when(estadoService.cerrar(adminCodigo, convocatoriaCodigo)).thenReturn(resultado(detalle));

        mockMvc.perform(post("/api/v1/convocatorias/" + convocatoriaCodigo + "/cerrar").with(administrador()))
                .andExpect(status().isOk());
    }

    @Test
    void cerrar_unaQueNoEstaAbierta_devuelve409ErrCv08() throws Exception {
        when(estadoService.cerrar(adminCodigo, convocatoriaCodigo)).thenThrow(new ConflictException(
                ConvocatoriaErrorCodes.ERR_CV_08, "Solo se puede cerrar una convocatoria abierta"));

        mockMvc.perform(post("/api/v1/convocatorias/" + convocatoriaCodigo + "/cerrar").with(administrador()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_CV_08"));
    }

    // ---- DELETE /convocatorias/{codigo} ----

    @Test
    void eliminar_devuelve200ConDataNula() throws Exception {
        when(service.eliminar(adminCodigo, convocatoriaCodigo)).thenReturn(resultado(null));

        mockMvc.perform(delete("/api/v1/convocatorias/" + convocatoriaCodigo).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void eliminar_conPostulaciones_devuelve409ErrCv10() throws Exception {
        when(service.eliminar(adminCodigo, convocatoriaCodigo)).thenThrow(new ConflictException(
                ConvocatoriaErrorCodes.ERR_CV_10, "La convocatoria tiene postulaciones"));

        mockMvc.perform(delete("/api/v1/convocatorias/" + convocatoriaCodigo).with(administrador()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_CV_10"));
    }
}
