package com.comedoruagrm.backend.features.usuarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioPersonalResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioPersonalService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioPersonalService.PaginaUsuariosPersonal;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de /api/v1/usuarios con el service simulado, recorriendo la cadena de seguridad real
 * (el token se simula con spring-security-test). La falta de rol (`@PreAuthorize`) no se prueba acá
 * porque con el service simulado esa anotacion no corre: ver UsuarioPersonalIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    private static final String BODY_CREAR = """
            {"nombres":"Ana","apellidos":"Perez","ci":"1234567","correo":"ana@uagrm.edu.bo","rol":"DIRECTIVO"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioPersonalService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID adminCodigo = UUID.randomUUID();
    private final UUID cuentaCodigo = UUID.randomUUID();
    private final UsuarioPersonalResponse cuenta = new UsuarioPersonalResponse(
            cuentaCodigo, "Ana", "Perez", "1234567", "ana@uagrm.edu.bo", "DIRECTIVO", true);

    @BeforeEach
    void sesionDelAdministrador() {
        when(provider.buscarPorCodigo(adminCodigo))
                .thenReturn(Optional.of(new DatosUsuario(adminCodigo, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor administrador() {
        return jwt().jwt(j -> j.subject(adminCodigo.toString()));
    }

    private ResultadoAuditado<UsuarioPersonalResponse> resultado(UsuarioPersonalResponse datos) {
        return new ResultadoAuditado<>(datos, 1L, adminCodigo, 5L, Map.of(), true);
    }

    // ---- GET /usuarios ----

    @Test
    void listar_devuelve200ConArrayYMeta() throws Exception {
        when(service.listar(1, 20, null, null, null, null)).thenReturn(
                new PaginaUsuariosPersonal(List.of(cuenta), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/usuarios").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Cuentas obtenidas correctamente"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].codigo").value(cuentaCodigo.toString()))
                .andExpect(jsonPath("$.data[0].rol").value("DIRECTIVO"))
                .andExpect(jsonPath("$.data[0].activo").value(true))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void listar_pasaPaginacionBusquedaFiltrosYOrdenAlService() throws Exception {
        when(service.listar(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(
                new PaginaUsuariosPersonal(List.of(), new PageMeta(2, 10, 0, 0)));

        mockMvc.perform(get("/api/v1/usuarios").with(administrador())
                        .param("page", "2").param("pageSize", "10").param("search", "ana")
                        .param("filter[rol]", "DIRECTIVO").param("filter[activo]", "false").param("sort", "-ci"))
                .andExpect(status().isOk());

        verify(service).listar(2, 10, "ana", "DIRECTIVO", false, "-ci");
    }

    @Test
    void listar_conUnFiltroDesconocido_devuelve400ErrUs11() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").with(administrador()).param("filter[correo]", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_US_11"));
        verify(service, never()).listar(anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void listar_conFilterActivoQueNoEsBooleano_devuelve400ErrUs11() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").with(administrador()).param("filter[activo]", "tal vez"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_US_11"));
    }

    @Test
    void listar_sinToken_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    // ---- POST /usuarios ----

    @Test
    void crear_devuelve201ConLaContrasenaTemporalYSinCache() throws Exception {
        var creada = new UsuarioCreadoResponse(cuentaCodigo, "Ana", "Perez", "1234567", "ana@uagrm.edu.bo",
                "DIRECTIVO", true, "Abc23456xyz9");
        when(service.crear(eq(adminCodigo), any())).thenReturn(
                new ResultadoAuditado<>(creada, 1L, adminCodigo, 5L, Map.of("rol", "DIRECTIVO"), true));

        mockMvc.perform(post("/api/v1/usuarios").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_CREAR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Cuenta creada correctamente"))
                .andExpect(jsonPath("$.data.contrasenaTemporal").value("Abc23456xyz9"))
                .andExpect(jsonPath("$.data.rol").value("DIRECTIVO"));
    }

    @Test
    void crear_conDatosInvalidos_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"\",\"apellidos\":\"P\",\"ci\":\"1\",\"correo\":\"x\",\"rol\":\"DIRECTIVO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(service, never()).crear(any(), any());
    }

    @Test
    void crear_conCiDuplicado_devuelve409ErrUs01() throws Exception {
        when(service.crear(any(), any())).thenThrow(
                new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));

        mockMvc.perform(post("/api/v1/usuarios").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_CREAR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_01"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ---- PATCH /usuarios/{codigo} ----

    @Test
    void editar_devuelve200ConLaCuentaActualizada() throws Exception {
        when(service.editar(eq(adminCodigo), eq(cuentaCodigo), any())).thenReturn(resultado(cuenta));

        mockMvc.perform(patch("/api/v1/usuarios/" + cuentaCodigo).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"apellidos\":\"Gomez\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta actualizada correctamente"))
                .andExpect(jsonPath("$.data.codigo").value(cuentaCodigo.toString()));
    }

    @Test
    void editar_conUnCampoEnBlanco_devuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/" + cuentaCodigo).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void editar_deUnaCuentaInexistente_devuelve404ErrUs06() throws Exception {
        when(service.editar(any(), any(), any())).thenThrow(
                new NotFoundException(UsuarioErrorCodes.ERR_US_06, "La cuenta indicada no existe"));

        mockMvc.perform(patch("/api/v1/usuarios/" + cuentaCodigo).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"apellidos\":\"Gomez\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_US_06"));
    }

    // ---- POST /usuarios/{codigo}/cambiar-rol, /desactivar, /reactivar ----

    @Test
    void cambiarRol_devuelve200() throws Exception {
        when(service.cambiarRol(eq(adminCodigo), eq(cuentaCodigo), any())).thenReturn(resultado(cuenta));

        mockMvc.perform(post("/api/v1/usuarios/" + cuentaCodigo + "/cambiar-rol").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"PERSONAL_COMEDOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rol cambiado correctamente"));
    }

    @Test
    void cambiarRol_sinRol_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/" + cuentaCodigo + "/cambiar-rol").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void desactivar_devuelve200() throws Exception {
        when(service.desactivar(adminCodigo, cuentaCodigo)).thenReturn(resultado(cuenta));

        mockMvc.perform(post("/api/v1/usuarios/" + cuentaCodigo + "/desactivar").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta desactivada correctamente"))
                .andExpect(jsonPath("$.data.codigo").value(cuentaCodigo.toString()));
    }

    @Test
    void reactivar_devuelve200() throws Exception {
        when(service.reactivar(adminCodigo, cuentaCodigo)).thenReturn(resultado(cuenta));

        mockMvc.perform(post("/api/v1/usuarios/" + cuentaCodigo + "/reactivar").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta reactivada correctamente"));
    }

    @Test
    void reactivar_unaCuentaYaActiva_devuelve409ErrUs07() throws Exception {
        when(service.reactivar(any(), any())).thenThrow(
                new ConflictException(UsuarioErrorCodes.ERR_US_07, "La cuenta ya está activa"));

        mockMvc.perform(post("/api/v1/usuarios/" + cuentaCodigo + "/reactivar").with(administrador()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_07"));
    }

    @Test
    void cualquierEndpoint_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/" + cuentaCodigo + "/desactivar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
        mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(BODY_CREAR))
                .andExpect(status().isUnauthorized());
    }
}
