package com.comedoruagrm.backend.features.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.AuthService;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.usuarios.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de /auth/registro, /login, /refresh, /logout y
 * /usuario-actual, con el AuthService simulado pero recorriendo la cadena de
 * seguridad real (los tokens de acceso se simulan con spring-security-test).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String BODY_VALIDO = """
            {"nombres":"Ana","apellidos":"Perez","ci":"1234567","registroUniversitario":"219012345",
             "carreraCodigo":"018f0c9a-0000-7000-8000-000000000001","telefono":"70000001",
             "correo":"ana@uagrm.edu.bo","contrasena":"secreta1","confirmacionContrasena":"secreta1"}""";
    private static final String BODY_LOGIN = """
            {"correo":"ana@uagrm.edu.bo","contrasena":"secreta1"}""";
    private static final String BODY_REFRESH = """
            {"refreshToken":"un-refresh-token"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID codigo = UUID.randomUUID();
    private final UsuarioResponse usuario = new UsuarioResponse(
            codigo, "Ana", "Perez", "ana@uagrm.edu.bo", "POSTULANTE");
    private final SesionResponse sesion = new SesionResponse("jwt", "refresh", "Bearer", 900, usuario);

    // ---- POST /auth/registro ----

    @Test
    void registro_conUnaCuentaNueva_devuelve201ConLaSesion() throws Exception {
        when(authService.registrar(any(), any())).thenReturn(new RegistroResultado(sesion, true));

        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.usuario.rol").value("POSTULANTE"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void registro_conUnaCuentaQueYaExistia_devuelve200() throws Exception {
        when(authService.registrar(any(), any())).thenReturn(new RegistroResultado(sesion, false));

        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void registro_conUnTokenVencidoEnElHeader_loIgnoraPorSerUnaRutaPublica() throws Exception {
        when(authService.registrar(any(), any())).thenReturn(new RegistroResultado(sesion, true));

        mockMvc.perform(post("/api/v1/auth/registro").header(HttpHeaders.AUTHORIZATION, "Bearer token-vencido")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    void registro_conUnRegistroUniversitarioInvalido_devuelve400ErrSys01() throws Exception {
        String invalido = BODY_VALIDO.replace("219012345", "abc");

        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
                .andExpect(jsonPath("$.message").value("El registro universitario debe tener 9 dígitos y no empezar por 0"));
    }

    @Test
    void registro_conUnaConfirmacionQueNoCoincide_devuelve400ErrSys01() throws Exception {
        String distinta = BODY_VALIDO.replace("\"confirmacionContrasena\":\"secreta1\"", "\"confirmacionContrasena\":\"otra123\"");

        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(distinta))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
                .andExpect(jsonPath("$.message").value("La contraseña y su confirmación no coinciden"));
    }

    @Test
    void registro_conElCiYaRegistrado_devuelve409ConErrUs01() throws Exception {
        when(authService.registrar(any(), any())).thenThrow(
                new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));

        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_US_01"))
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese CI"));
    }

    // ---- POST /auth/login ----

    @Test
    void login_conCredencialesCorrectas_devuelve200ConLaSesion() throws Exception {
        when(authService.iniciarSesion(any(), any())).thenReturn(sesion);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt"))
                .andExpect(jsonPath("$.data.usuario.correo").value("ana@uagrm.edu.bo"));
    }

    @Test
    void login_conCredencialesIncorrectas_devuelve401ErrAuth04() throws Exception {
        when(authService.iniciarSesion(any(), any())).thenThrow(
                new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04, "Correo o contraseña incorrectos."));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY_LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));
    }

    @Test
    void login_conLaCuentaBloqueada_devuelve403ErrAuth05() throws Exception {
        when(authService.iniciarSesion(any(), any())).thenThrow(
                new ForbiddenException(AuthErrorCodes.ERR_AUTH_05, "Cuenta bloqueada."));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY_LOGIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_05"));
    }

    @Test
    void login_sinContrasena_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@uagrm.edu.bo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    // ---- POST /auth/refresh y /auth/logout ----

    @Test
    void refresh_conUnRefreshTokenVigente_devuelve200ConUnParNuevo() throws Exception {
        when(authService.renovar("un-refresh-token")).thenReturn(sesion);

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(BODY_REFRESH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void refresh_conUnRefreshTokenInvalido_devuelve401ErrAuth06() throws Exception {
        when(authService.renovar(any())).thenThrow(
                new UnauthorizedException(AuthErrorCodes.ERR_AUTH_06, "Tu sesión expiró."));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(BODY_REFRESH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_06"));
    }

    @Test
    void logout_cierraLaSesionDelRefreshTokenSinExigirUnTokenDeAcceso() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(BODY_REFRESH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).cerrarSesion(eq("un-refresh-token"));
    }

    // ---- GET /auth/usuario-actual ----

    @Test
    void usuarioActual_conUnUsuarioActivo_devuelve200() throws Exception {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
        when(authService.usuarioActual(codigo)).thenReturn(usuario);

        mockMvc.perform(get("/api/v1/auth/usuario-actual").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"));
    }

    @Test
    void usuarioActual_sinToken_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/auth/usuario-actual"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void usuarioActual_conLaCuentaDesactivada_devuelve403ErrAuth03() throws Exception {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", false)));

        mockMvc.perform(get("/api/v1/auth/usuario-actual").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
    }

    @Test
    void usuarioActual_conUnUsuarioQueYaNoExiste_devuelve401ErrAuth02() throws Exception {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/auth/usuario-actual").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_02"));
    }
}
