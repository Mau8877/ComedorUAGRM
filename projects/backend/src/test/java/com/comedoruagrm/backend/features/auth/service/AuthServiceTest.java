package com.comedoruagrm.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.LoginRequest;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.auth.service.SesionService.SesionAbierta;
import com.comedoruagrm.backend.features.bitacora.BitacoraAcciones;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraService;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.TokenService.TokenAcceso;

class AuthServiceTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final SesionService sesionService = mock(SesionService.class);
    private final BitacoraService bitacoraService = mock(BitacoraService.class);
    private final AuthService service = new AuthService(usuarioService, sesionService, bitacoraService);

    private final Usuario usuario = mock(Usuario.class);
    private final Rol rol = mock(Rol.class);
    private final UsuarioResponse respuesta = new UsuarioResponse(
            UUID.randomUUID(), "Ana", "Perez", "ana@uagrm.edu.bo", "POSTULANTE");
    private final SesionAbierta abierta = new SesionAbierta(usuario, new TokenAcceso("jwt", 900), "refresh");
    private final RegistroRequest request = new RegistroRequest("Ana", "Perez", "1234567", "219012345",
            UUID.randomUUID(), "70000001", "ana@uagrm.edu.bo", "secreta1", "secreta1");

    @BeforeEach
    void usuarioConRol() {
        when(usuario.getId()).thenReturn(5L);
        when(usuario.getRol()).thenReturn(rol);
        when(rol.getNombre()).thenReturn("POSTULANTE");
        when(usuarioService.aRespuesta(usuario)).thenReturn(respuesta);
        when(sesionService.abrir(usuario)).thenReturn(abierta);
    }

    @Test
    void registrar_conUnaCuentaNueva_anotaRegistroEInicioDeSesionYAbreLaSesion() {
        when(usuarioService.registrarPostulante(request)).thenReturn(new ResultadoRegistro(usuario, true));

        RegistroResultado resultado = service.registrar(request, "203.0.113.10");

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.sesion().accessToken()).isEqualTo("jwt");
        assertThat(resultado.sesion().refreshToken()).isEqualTo("refresh");
        assertThat(resultado.sesion().tokenType()).isEqualTo("Bearer");
        assertThat(resultado.sesion().expiresIn()).isEqualTo(900);
        assertThat(resultado.sesion().usuario()).isEqualTo(respuesta);
        verify(bitacoraService).registrar(eq(usuario), eq(BitacoraAcciones.REGISTRO_USUARIO), eq("usuario"),
                eq(5L), eq(Map.of("rol", "POSTULANTE")), eq("203.0.113.10"));
        verify(bitacoraService).registrar(eq(usuario), eq(BitacoraAcciones.INICIO_SESION), eq("usuario"),
                eq(5L), any(), eq("203.0.113.10"));
    }

    @Test
    void registrar_conUnaCuentaQueYaExistia_noAnotaElRegistroPeroAbreLaSesion() {
        when(usuarioService.registrarPostulante(request)).thenReturn(new ResultadoRegistro(usuario, false));

        RegistroResultado resultado = service.registrar(request, "203.0.113.10");

        assertThat(resultado.creado()).isFalse();
        verify(bitacoraService, never()).registrar(any(), eq(BitacoraAcciones.REGISTRO_USUARIO), any(), any(), any(), any());
        verify(bitacoraService).registrar(eq(usuario), eq(BitacoraAcciones.INICIO_SESION), any(), any(), any(), any());
    }

    @Test
    void iniciarSesion_conCredencialesCorrectas_abreLaSesionYAnotaUnInicioDeSesion() {
        when(usuarioService.validarCredenciales("ana@uagrm.edu.bo", "secreta1")).thenReturn(usuario);

        SesionResponse resultado = service.iniciarSesion(new LoginRequest("ana@uagrm.edu.bo", "secreta1"), "203.0.113.10");

        assertThat(resultado.accessToken()).isEqualTo("jwt");
        assertThat(resultado.usuario()).isEqualTo(respuesta);
        verify(bitacoraService).registrar(eq(usuario), eq(BitacoraAcciones.INICIO_SESION), eq("usuario"),
                eq(5L), eq(Map.of("rol", "POSTULANTE")), eq("203.0.113.10"));
    }

    @Test
    void iniciarSesion_conCredencialesIncorrectas_propagaElErrorSinAbrirSesionNiAnotar() {
        when(usuarioService.validarCredenciales(anyString(), anyString()))
                .thenThrow(new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04, "Correo o contraseña incorrectos."));

        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest("ana@uagrm.edu.bo", "mala"), "203.0.113.10"))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(sesionService, bitacoraService);
    }

    @Test
    void renovar_conUnRefreshTokenVigente_devuelveUnParNuevoSinAnotarNada() {
        when(sesionService.renovar("viejo")).thenReturn(abierta);

        SesionResponse resultado = service.renovar("viejo");

        assertThat(resultado.refreshToken()).isEqualTo("refresh");
        verifyNoInteractions(bitacoraService);
    }

    @Test
    void cerrarSesion_revocaElRefreshTokenRecibido() {
        service.cerrarSesion("token");

        verify(sesionService).cerrar("token");
    }

    @Test
    void usuarioActual_conUnUsuarioActivo_devuelveSusDatosSinAnotarNada() {
        UUID codigo = UUID.randomUUID();
        when(usuarioService.obtenerResumen(codigo)).thenReturn(respuesta);

        assertThat(service.usuarioActual(codigo)).isEqualTo(respuesta);
        verify(bitacoraService, never()).registrar(any(), anyString(), any(), any(), any(), any());
    }
}
