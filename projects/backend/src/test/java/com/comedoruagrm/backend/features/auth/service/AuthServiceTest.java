package com.comedoruagrm.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.LoginRequest;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.auth.service.SesionService.SesionAbierta;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.TokenService.TokenAcceso;

/**
 * La bitacora ya no se prueba acá: el alta y el inicio de sesion los anota
 * `@Auditar` (ver AuditoriaAspectTest y AuthIntegrationTest).
 */
class AuthServiceTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final SesionService sesionService = mock(SesionService.class);
    private final AuthService service = new AuthService(usuarioService, sesionService);

    private final Usuario usuario = mock(Usuario.class);
    private final UsuarioResponse respuesta = new UsuarioResponse(
            UUID.randomUUID(), "Ana", "Perez", "ana@uagrm.edu.bo", "POSTULANTE");
    private final SesionAbierta abierta = new SesionAbierta(usuario, new TokenAcceso("jwt", 900), "refresh");
    private final RegistroRequest request = new RegistroRequest("Ana", "Perez", "1234567", "219012345",
            UUID.randomUUID(), "70000001", "ana@uagrm.edu.bo", "secreta1", "secreta1");

    @BeforeEach
    void sesionDelUsuario() {
        when(usuarioService.aRespuesta(usuario)).thenReturn(respuesta);
        when(sesionService.abrir(usuario)).thenReturn(abierta);
    }

    @Test
    void registrar_conUnaCuentaNueva_abreLaSesionYMarcaElAltaComoCreada() {
        when(usuarioService.registrarPostulante(request)).thenReturn(new ResultadoRegistro(usuario, true));

        RegistroResultado resultado = service.registrar(request);

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.sesion().accessToken()).isEqualTo("jwt");
        assertThat(resultado.sesion().refreshToken()).isEqualTo("refresh");
        assertThat(resultado.sesion().tokenType()).isEqualTo("Bearer");
        assertThat(resultado.sesion().expiresIn()).isEqualTo(900);
        assertThat(resultado.sesion().usuario()).isEqualTo(respuesta);
        verify(sesionService).abrir(usuario);
    }

    @Test
    void registrar_conUnaCuentaQueYaExistia_abreLaSesionPeroNoLaMarcaComoCreada() {
        when(usuarioService.registrarPostulante(request)).thenReturn(new ResultadoRegistro(usuario, false));

        RegistroResultado resultado = service.registrar(request);

        assertThat(resultado.creado()).isFalse();
        verify(sesionService).abrir(usuario);
    }

    @Test
    void iniciarSesion_conCredencialesCorrectas_abreLaSesion() {
        when(usuarioService.validarCredenciales("ana@uagrm.edu.bo", "secreta1")).thenReturn(usuario);

        SesionResponse resultado = service.iniciarSesion(new LoginRequest("ana@uagrm.edu.bo", "secreta1"));

        assertThat(resultado.accessToken()).isEqualTo("jwt");
        assertThat(resultado.usuario()).isEqualTo(respuesta);
    }

    @Test
    void iniciarSesion_conCredencialesIncorrectas_propagaElErrorSinAbrirSesion() {
        when(usuarioService.validarCredenciales(anyString(), anyString()))
                .thenThrow(new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04, "Correo o contraseña incorrectos."));

        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest("ana@uagrm.edu.bo", "mala")))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(sesionService);
    }

    @Test
    void renovar_conUnRefreshTokenVigente_devuelveUnParNuevo() {
        when(sesionService.renovar("viejo")).thenReturn(abierta);

        SesionResponse resultado = service.renovar("viejo");

        assertThat(resultado.refreshToken()).isEqualTo("refresh");
        verify(sesionService, never()).abrir(usuario);
    }

    @Test
    void cerrarSesion_revocaElRefreshTokenRecibido() {
        service.cerrarSesion("token");

        verify(sesionService).cerrar("token");
    }

    @Test
    void usuarioActual_conUnUsuarioActivo_devuelveSusDatos() {
        UUID codigo = UUID.randomUUID();
        when(usuarioService.obtenerResumen(codigo)).thenReturn(respuesta);

        assertThat(service.usuarioActual(codigo)).isEqualTo(respuesta);
        verifyNoInteractions(sesionService);
    }
}
