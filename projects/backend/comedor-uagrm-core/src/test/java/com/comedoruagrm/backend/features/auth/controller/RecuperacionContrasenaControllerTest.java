package com.comedoruagrm.backend.features.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.auth.service.RestablecimientoContrasenaService;
import com.comedoruagrm.backend.features.usuarios.service.PerfilService;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

import jakarta.servlet.http.Cookie;

/**
 * Contrato HTTP de recuperar, restablecer y cambiar la contraseña (CU15), con los services
 * simulados pero recorriendo la cadena de seguridad real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecuperacionContrasenaControllerTest {

    private static final String RECUPERAR = "/api/v1/auth/recuperar-contrasena";
    private static final String RESTABLECER = "/api/v1/auth/restablecer-contrasena";
    private static final String CAMBIAR = "/api/v1/auth/cambiar-contrasena";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestablecimientoContrasenaService restablecimientoService;

    @MockitoBean
    private PerfilService perfilService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID codigo = UUID.randomUUID();

    // ---- POST /auth/recuperar-contrasena ----

    @Test
    void recuperar_conUnCorreo_devuelve200ConElMensajeNeutro() throws Exception {
        when(restablecimientoService.vigenciaMinutos()).thenReturn(60L);

        mockMvc.perform(post(RECUPERAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@uagrm.edu.bo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(
                        "Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(restablecimientoService).solicitar("ana@uagrm.edu.bo");
    }

    @Test
    void recuperar_conUnTokenVencidoEnElHeader_loIgnoraPorSerUnaRutaPublica() throws Exception {
        mockMvc.perform(post(RECUPERAR).header(HttpHeaders.AUTHORIZATION, "Bearer token-vencido")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"ana@uagrm.edu.bo\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void recuperar_conUnCorreoMalFormadoOVacio_devuelve400ErrSys01SinLlamarAlService() throws Exception {
        mockMvc.perform(post(RECUPERAR).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RECUPERAR).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(restablecimientoService, never()).solicitar(any());
    }

    // ---- POST /auth/restablecer-contrasena ----

    @Test
    void restablecer_conDatosValidos_devuelve200() throws Exception {
        mockMvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"contrasena\":\"secreta1\",\"confirmacionContrasena\":\"secreta1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Contraseña restablecida correctamente"));

        verify(restablecimientoService).restablecer("tok", "secreta1");
    }

    @Test
    void restablecer_conContrasenaCortaOSinCoincidir_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"contrasena\":\"123\",\"confirmacionContrasena\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"contrasena\":\"secreta1\",\"confirmacionContrasena\":\"otra-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(restablecimientoService, never()).restablecer(any(), any());
    }

    @Test
    void restablecer_conUnEnlaceInvalido_devuelve400ErrAuth08() throws Exception {
        doThrow(new ValidationException(AuthErrorCodes.ERR_AUTH_08, "El enlace ya no es válido. Solicita uno nuevo."))
                .when(restablecimientoService).restablecer(any(), any());

        mockMvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"contrasena\":\"secreta1\",\"confirmacionContrasena\":\"secreta1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }

    // ---- POST /auth/cambiar-contrasena ----

    @Test
    void cambiar_sinSesion_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(post(CAMBIAR).contentType(MediaType.APPLICATION_JSON).content(cuerpoCambio(null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));

        verify(perfilService, never()).cambiarContrasena(any(), any(), any());
    }

    @Test
    void cambiar_conSesionYRefreshTokenEnElCuerpo_elCuerpoGanaSobreLaCookie() throws Exception {
        autenticarAlUsuario();

        mockMvc.perform(post(CAMBIAR).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .cookie(new Cookie("refresh_token", "de-la-cookie"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoCambio("del-cuerpo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"));

        verify(perfilService).cambiarContrasena(eq(codigo), any(), eq("del-cuerpo"));
    }

    @Test
    void cambiar_sinRefreshTokenEnElCuerpo_usaLaCookieDeLaWeb() throws Exception {
        autenticarAlUsuario();

        mockMvc.perform(post(CAMBIAR).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .cookie(new Cookie("refresh_token", "de-la-cookie"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoCambio(null)))
                .andExpect(status().isOk());

        verify(perfilService).cambiarContrasena(eq(codigo), any(), eq("de-la-cookie"));
    }

    @Test
    void cambiar_sinNingunRefreshToken_loPasaNuloAlService() throws Exception {
        autenticarAlUsuario();

        mockMvc.perform(post(CAMBIAR).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoCambio(null)))
                .andExpect(status().isOk());

        verify(perfilService).cambiarContrasena(eq(codigo), any(), isNull());
    }

    @Test
    void cambiar_conLaContrasenaActualIncorrecta_devuelve400ErrAuth09YNoUn401() throws Exception {
        autenticarAlUsuario();
        when(perfilService.cambiarContrasena(any(), any(), any())).thenThrow(
                new ValidationException(AuthErrorCodes.ERR_AUTH_09, "La contraseña actual es incorrecta."));

        mockMvc.perform(post(CAMBIAR).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoCambio(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_09"));
    }

    @Test
    void cambiar_conLaNuevaCortaOSinCoincidir_devuelve400ErrSys01() throws Exception {
        autenticarAlUsuario();

        mockMvc.perform(post(CAMBIAR).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"actual\",\"contrasenaNueva\":\"123\",\"confirmacionContrasena\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    private void autenticarAlUsuario() {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
    }

    private String cuerpoCambio(String refreshToken) {
        String refresh = refreshToken == null ? "" : ",\"refreshToken\":\"" + refreshToken + "\"";
        return "{\"contrasenaActual\":\"actual-123\",\"contrasenaNueva\":\"nueva-1234\","
                + "\"confirmacionContrasena\":\"nueva-1234\"" + refresh + "}";
    }
}
