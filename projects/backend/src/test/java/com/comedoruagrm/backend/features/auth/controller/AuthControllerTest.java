package com.comedoruagrm.backend.features.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.features.auth.service.AuthService;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.usuarios.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.security.FirebaseSinRegistro;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de /auth/registro, /auth/sesion y /auth/usuario-actual, con
 * el AuthService simulado pero recorriendo el filtro de usuario real (los
 * tokens se simulan con spring-security-test).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String BODY_VALIDO = """
            {"nombres":"Ana","apellidos":"Perez","ci":"1234567","registroUniversitario":"219012345",
             "carreraCodigo":"018f0c9a-0000-7000-8000-000000000001","telefono":"70000001"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID codigo = UUID.randomUUID();
    private final UsuarioResponse respuesta = new UsuarioResponse(
            UUID.randomUUID(), "Ana", "Perez", "ana@uagrm.edu.bo", "POSTULANTE");

    @BeforeEach
    void sinCuentaPorDefecto() {
        when(provider.buscarPorFirebaseUid(any())).thenReturn(Optional.empty());
    }

    // ---- POST /auth/registro ----

    @Test
    void registro_conUnaCuentaNueva_devuelve201ConElUsuario() throws Exception {
        when(authService.registrar(any(), any(), any())).thenReturn(new RegistroResultado(respuesta, true));

        mockMvc.perform(post("/api/v1/auth/registro").with(tokenDeFirebase("uid-nuevo", "ana@uagrm.edu.bo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void registro_conUnaCuentaQueYaExistia_devuelve200() throws Exception {
        when(authService.registrar(any(), any(), any())).thenReturn(new RegistroResultado(respuesta, false));

        mockMvc.perform(post("/api/v1/auth/registro").with(tokenDeFirebase("uid-repetido", "ana@uagrm.edu.bo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void registro_conUnaCuentaActivaYaRegistrada_devuelve200SinVolverACrear() throws Exception {
        when(provider.buscarPorFirebaseUid("uid-existente"))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
        when(authService.registroExistente(codigo)).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/auth/registro").with(tokenDeFirebase("uid-existente", "ana@uagrm.edu.bo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"));
        verify(authService, never()).registrar(any(), any(), any());
    }

    @Test
    void registro_conUnBodyInvalido_devuelve400ErrSys01() throws Exception {
        String invalido = BODY_VALIDO.replace("219012345", "abc");

        mockMvc.perform(post("/api/v1/auth/registro").with(tokenDeFirebase("uid-nuevo", "ana@uagrm.edu.bo"))
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
                .andExpect(jsonPath("$.message").value("El registro universitario debe tener 9 dígitos y no empezar por 0"));
    }

    @Test
    void registro_conElCiYaRegistrado_devuelve409ConErrUs01() throws Exception {
        when(authService.registrar(any(), any(), any())).thenThrow(
                new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));

        mockMvc.perform(post("/api/v1/auth/registro").with(tokenDeFirebase("uid-nuevo", "ana@uagrm.edu.bo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_US_01"))
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese CI"));
    }

    @Test
    void registro_sinToken_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void registro_conRolYCorreoEnElBody_losIgnoraYUsaElCorreoDelToken() throws Exception {
        when(authService.registrar(any(), any(), any())).thenReturn(new RegistroResultado(respuesta, true));
        String conExtras = BODY_VALIDO.replace("{", "{\"rol\":\"ADMINISTRADOR_DUBSS\",\"correo\":\"otro@x.com\",");

        mockMvc.perform(post("/api/v1/auth/registro").with(tokenDeFirebase("uid-nuevo", "del-token@uagrm.edu.bo"))
                        .contentType(MediaType.APPLICATION_JSON).content(conExtras))
                .andExpect(status().isCreated());

        verify(authService).registrar(
                eq(new FirebaseSinRegistro("uid-nuevo", "del-token@uagrm.edu.bo")), any(), any());
    }

    // ---- POST /auth/sesion y GET /auth/usuario-actual ----

    @Test
    void sesion_conUnUsuarioActivo_devuelve200YAnotaElAccesoUnaVez() throws Exception {
        usuarioActivo("uid-activo");
        when(authService.iniciarSesion(eq(codigo), any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/auth/sesion").with(tokenDeFirebase("uid-activo", "ana@uagrm.edu.bo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"));

        verify(authService, times(1)).iniciarSesion(eq(codigo), any());
    }

    @Test
    void usuarioActual_conUnUsuarioActivo_devuelve200SinAnotarElAcceso() throws Exception {
        usuarioActivo("uid-activo");
        when(authService.usuarioActual(codigo)).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/auth/usuario-actual").with(tokenDeFirebase("uid-activo", "ana@uagrm.edu.bo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"));

        verify(authService, never()).iniciarSesion(any(), any());
    }

    @Test
    void sesion_sinToken_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sesion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void sesion_conLaCuentaDesactivada_devuelve403ErrAuth03() throws Exception {
        when(provider.buscarPorFirebaseUid("uid-baja"))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", false)));

        mockMvc.perform(post("/api/v1/auth/sesion").with(tokenDeFirebase("uid-baja", "baja@uagrm.edu.bo")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
        verify(authService, never()).iniciarSesion(any(), any());
    }

    @Test
    void usuarioActual_sinCuentaEnElSistema_devuelve401ErrAuth02() throws Exception {
        mockMvc.perform(get("/api/v1/auth/usuario-actual").with(tokenDeFirebase("uid-sin-cuenta", "x@uagrm.edu.bo")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_02"));
    }

    private void usuarioActivo(String firebaseUid) {
        when(provider.buscarPorFirebaseUid(firebaseUid))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
    }

    private RequestPostProcessor tokenDeFirebase(String uid, String correo) {
        return jwt().jwt(j -> j.subject(uid).claim("email", correo));
    }
}
