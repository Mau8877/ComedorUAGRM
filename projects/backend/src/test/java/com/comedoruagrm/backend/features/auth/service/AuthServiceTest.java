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

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.bitacora.BitacoraAcciones;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraService;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;
import com.comedoruagrm.backend.security.FirebaseSinRegistro;

class AuthServiceTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final BitacoraService bitacoraService = mock(BitacoraService.class);
    private final AuthService service = new AuthService(usuarioService, bitacoraService);

    private final Usuario usuario = mock(Usuario.class);
    private final UsuarioResponse respuesta = new UsuarioResponse(
            UUID.randomUUID(), "Ana", "Perez", "ana@uagrm.edu.bo", "POSTULANTE");
    private final RegistroRequest request = new RegistroRequest(
            "Ana", "Perez", "1234567", "219012345", UUID.randomUUID(), "70000001");
    private final FirebaseSinRegistro token = new FirebaseSinRegistro("uid-ana", "ana@uagrm.edu.bo");

    @Test
    void registrar_conUnaCuentaNueva_anotaElRegistroEnLaBitacoraYDevuelveElUsuario() {
        when(usuario.getId()).thenReturn(5L);
        when(usuarioService.registrarPostulante("uid-ana", "ana@uagrm.edu.bo", request))
                .thenReturn(new ResultadoRegistro(usuario, true));
        when(usuarioService.aRespuesta(usuario)).thenReturn(respuesta);

        RegistroResultado resultado = service.registrar(token, request, "203.0.113.10");

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.usuario()).isEqualTo(respuesta);
        verify(bitacoraService).registrar(eq(usuario), eq(BitacoraAcciones.REGISTRO_USUARIO), eq("usuario"),
                eq(5L), eq(Map.of("rol", "POSTULANTE")), eq("203.0.113.10"));
    }

    @Test
    void registrar_conUnaCuentaQueYaExistia_noAnotaNadaEnLaBitacora() {
        when(usuarioService.registrarPostulante("uid-ana", "ana@uagrm.edu.bo", request))
                .thenReturn(new ResultadoRegistro(usuario, false));
        when(usuarioService.aRespuesta(usuario)).thenReturn(respuesta);

        RegistroResultado resultado = service.registrar(token, request, "203.0.113.10");

        assertThat(resultado.creado()).isFalse();
        verifyNoInteractions(bitacoraService);
    }

    @Test
    void registrar_conUnTokenSinCorreo_lanzaUnauthorizedExceptionSinCrearNada() {
        FirebaseSinRegistro sinCorreo = new FirebaseSinRegistro("uid-x", null);

        assertThatThrownBy(() -> service.registrar(sinCorreo, request, "203.0.113.10"))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(usuarioService, bitacoraService);
    }

    @Test
    void iniciarSesion_conUnUsuarioActivo_anotaExactamenteUnInicioDeSesion() {
        UUID codigo = UUID.randomUUID();
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn("POSTULANTE");
        when(usuario.getId()).thenReturn(5L);
        when(usuario.getRol()).thenReturn(rol);
        when(usuarioService.obtenerPorCodigo(codigo)).thenReturn(usuario);
        when(usuarioService.aRespuesta(usuario)).thenReturn(respuesta);

        UsuarioResponse resultado = service.iniciarSesion(codigo, "203.0.113.10");

        assertThat(resultado).isEqualTo(respuesta);
        verify(bitacoraService).registrar(eq(usuario), eq(BitacoraAcciones.INICIO_SESION), eq("usuario"),
                eq(5L), any(), eq("203.0.113.10"));
    }

    @Test
    void usuarioActual_conUnUsuarioActivo_devuelveSusDatosSinAnotarNada() {
        UUID codigo = UUID.randomUUID();
        when(usuarioService.obtenerResumen(codigo)).thenReturn(respuesta);

        assertThat(service.usuarioActual(codigo)).isEqualTo(respuesta);
        verify(bitacoraService, never()).registrar(any(), anyString(), any(), any(), any(), any());
    }
}
