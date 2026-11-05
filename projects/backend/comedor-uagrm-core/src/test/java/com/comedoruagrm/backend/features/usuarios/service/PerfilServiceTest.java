package com.comedoruagrm.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.auth.dto.CambiarContrasenaRequest;
import com.comedoruagrm.backend.features.auth.service.SesionService;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarPerfilRequest;
import com.comedoruagrm.backend.features.usuarios.dto.PerfilResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.model.Postulante;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.CarreraRepository;
import com.comedoruagrm.backend.features.usuarios.repository.PostulanteRepository;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.AuthErrorCodes;

class PerfilServiceTest {

    private static final String CORREO = "ana@uagrm.edu.bo";
    private static final String CLAVE = "clave-actual";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PostulanteRepository postulanteRepository = mock(PostulanteRepository.class);
    private final SesionService sesionService = mock(SesionService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UsuarioService usuarioService = new UsuarioService(usuarioRepository, postulanteRepository,
            mock(RolRepository.class), mock(CarreraRepository.class), passwordEncoder);
    private final PerfilService service = new PerfilService(usuarioRepository, postulanteRepository,
            usuarioService, sesionService);

    private final UUID codigo = UUID.randomUUID();
    private Usuario ana;
    private Postulante postulanteAna;

    @BeforeEach
    void preparar() {
        Rol rolPostulante = mock(Rol.class);
        when(rolPostulante.getNombre()).thenReturn("POSTULANTE");
        ana = new Usuario(rolPostulante, "1234567", CORREO, passwordEncoder.encode(CLAVE), "Ana", "Perez");
        Carrera carrera = mock(Carrera.class);
        when(carrera.getCodigo()).thenReturn(UUID.randomUUID());
        when(carrera.getNombre()).thenReturn("Ingeniería Informática");
        when(carrera.getFacultad()).thenReturn("Ciencias de la Computación");
        postulanteAna = new Postulante(ana, carrera, "219012345", "70000001");

        when(usuarioRepository.findByCodigo(codigo)).thenReturn(Optional.of(ana));
        when(postulanteRepository.findByUsuarioId(any())).thenReturn(Optional.of(postulanteAna));
    }

    private void comoAdministrador() {
        Rol rolAdmin = mock(Rol.class);
        when(rolAdmin.getNombre()).thenReturn("ADMINISTRADOR_DUBSS");
        ana = new Usuario(rolAdmin, "7654321", CORREO, passwordEncoder.encode(CLAVE), "Luis", "Rojas");
        when(usuarioRepository.findByCodigo(codigo)).thenReturn(Optional.of(ana));
        when(postulanteRepository.findByUsuarioId(any())).thenReturn(Optional.empty());
    }

    // ---- obtener ----

    @Test
    void obtener_deUnPostulante_traeRegistroCarreraYTelefono() {
        PerfilResponse perfil = service.obtener(codigo);

        assertThat(perfil.ci()).isEqualTo("1234567");
        assertThat(perfil.rol()).isEqualTo("POSTULANTE");
        assertThat(perfil.postulante().registroUniversitario()).isEqualTo("219012345");
        assertThat(perfil.postulante().telefono()).isEqualTo("70000001");
        assertThat(perfil.postulante().carrera().nombre()).isEqualTo("Ingeniería Informática");
    }

    @Test
    void obtener_deUnAdministrador_noTraeDatosDePostulante() {
        comoAdministrador();

        PerfilResponse perfil = service.obtener(codigo);

        assertThat(perfil.rol()).isEqualTo("ADMINISTRADOR_DUBSS");
        assertThat(perfil.postulante()).isNull();
    }

    // ---- actualizar ----

    @Test
    void actualizar_soloElTelefono_noPideContrasenaYAnotaSoloEseCampo() {
        ResultadoAuditado<PerfilResponse> resultado =
                service.actualizar(codigo, new ActualizarPerfilRequest(null, " 71111111 ", null));

        assertThat(postulanteAna.getTelefono()).isEqualTo("71111111");
        assertThat(resultado.datos().postulante().telefono()).isEqualTo("71111111");
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle()).containsEntry("campos", List.of("telefono"));
        verify(postulanteRepository).saveAndFlush(postulanteAna);
    }

    @Test
    void actualizar_elCorreoConLaContrasenaActual_loNormalizaYLoGuarda() {
        when(usuarioRepository.existsByCorreo("nuevo@uagrm.edu.bo")).thenReturn(false);

        ResultadoAuditado<PerfilResponse> resultado =
                service.actualizar(codigo, new ActualizarPerfilRequest("  Nuevo@UAGRM.edu.bo ", null, CLAVE));

        assertThat(ana.getCorreo()).isEqualTo("nuevo@uagrm.edu.bo");
        assertThat(resultado.detalle()).containsEntry("campos", List.of("correo"));
        assertThat(resultado.detalle().toString()).doesNotContain("nuevo@uagrm").doesNotContain(CLAVE);
    }

    @Test
    void actualizar_elCorreoConLaContrasenaIncorrectaOAusente_lanzaErrAuth09SinCambiarNada() {
        for (String intento : new String[] {"mala", null}) {
            assertThatThrownBy(() -> service.actualizar(codigo,
                    new ActualizarPerfilRequest("nuevo@uagrm.edu.bo", null, intento)))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCodes.ERR_AUTH_09));
        }

        assertThat(ana.getCorreo()).isEqualTo(CORREO);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizar_conUnCorreoDeOtraCuenta_lanzaErrUs02SinGuardarNada() {
        when(usuarioRepository.existsByCorreo("otro@uagrm.edu.bo")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(codigo,
                new ActualizarPerfilRequest("otro@uagrm.edu.bo", "72222222", CLAVE)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_02));

        assertThat(ana.getCorreo()).isEqualTo(CORREO);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(postulanteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizar_elTelefonoDeUnQueNoEsPostulante_lanzaErrUs13() {
        comoAdministrador();

        assertThatThrownBy(() -> service.actualizar(codigo, new ActualizarPerfilRequest(null, "70000001", null)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_13));
    }

    @Test
    void actualizar_sinCambiosReales_respondeElPerfilYNoAuditaNiGuarda() {
        // Mismo correo (sin pedir contraseña), mismo telefono y nada mas.
        ResultadoAuditado<PerfilResponse> resultado =
                service.actualizar(codigo, new ActualizarPerfilRequest(CORREO, "70000001", null));

        assertThat(resultado.auditar()).isFalse();
        assertThat(resultado.datos().correo()).isEqualTo(CORREO);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    // ---- cambiarContrasena ----

    @Test
    void cambiarContrasena_conLaActualCorrecta_guardaLaNuevaYCierraLasOtrasSesiones() {
        var request = new CambiarContrasenaRequest(CLAVE, "clave-nueva-1", "clave-nueva-1", null);

        ResultadoAuditado<Void> resultado = service.cambiarContrasena(codigo, request, "refresh-actual");

        assertThat(passwordEncoder.matches("clave-nueva-1", ana.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(CLAVE, ana.getPasswordHash())).isFalse();
        verify(sesionService).revocarOtras(ana, "refresh-actual");
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle().toString()).doesNotContain("clave").doesNotContain("refresh-actual");
    }

    @Test
    void cambiarContrasena_conLaActualIncorrecta_lanzaErrAuth09SinCambiarNiCerrarSesiones() {
        String hashAntes = ana.getPasswordHash();
        var request = new CambiarContrasenaRequest("mala", "clave-nueva-1", "clave-nueva-1", null);

        assertThatThrownBy(() -> service.cambiarContrasena(codigo, request, "refresh-actual"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCodes.ERR_AUTH_09));

        assertThat(ana.getPasswordHash()).isEqualTo(hashAntes);
        verify(sesionService, never()).revocarOtras(any(), any());
    }

    @Test
    void cambiarContrasena_sinRefreshToken_loPasaNuloParaQueSeCierrenTodas() {
        var request = new CambiarContrasenaRequest(CLAVE, "clave-nueva-1", "clave-nueva-1", null);

        service.cambiarContrasena(codigo, request, null);

        verify(sesionService).revocarOtras(ana, null);
    }
}
