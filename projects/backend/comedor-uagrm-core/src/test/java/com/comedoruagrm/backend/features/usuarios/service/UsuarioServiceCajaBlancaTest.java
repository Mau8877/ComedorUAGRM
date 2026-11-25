package com.comedoruagrm.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.model.Postulante;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.CarreraRepository;
import com.comedoruagrm.backend.features.usuarios.repository.PostulanteRepository;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;
import com.comedoruagrm.backend.security.AuthErrorCodes;

/**
 * Pruebas de caja blanca de CU-01 (capitulo 9, 9.3.2): un test por camino basico de
 * `registrarPostulante` (C1 a C7; C8 no es factible) y de `validarCredenciales` (C1 a C6).
 * El nombre de cada test lleva el identificador del camino de la tabla del documento.
 */
class UsuarioServiceCajaBlancaTest {

    private static final String CORREO = "ana@uagrm.edu.bo";
    private static final String CONTRASENA = "secreta1";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PostulanteRepository postulanteRepository = mock(PostulanteRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final CarreraRepository carreraRepository = mock(CarreraRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UsuarioService service = new UsuarioService(
            usuarioRepository, postulanteRepository, rolRepository, carreraRepository, passwordEncoder);

    private final Rol rolPostulante = mock(Rol.class);
    private final Carrera carrera = mock(Carrera.class);
    private final UUID carreraCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        when(rolPostulante.getNombre()).thenReturn(RolNombres.POSTULANTE);
        when(rolRepository.findByNombre(RolNombres.POSTULANTE)).thenReturn(Optional.of(rolPostulante));
        when(carreraRepository.findByCodigo(carreraCodigo)).thenReturn(Optional.of(carrera));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postulanteRepository.saveAndFlush(any(Postulante.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------------------------------------------------------------- registrarPostulante

    @Test
    void registrarPostulante_C1_repeticionIdempotente_devuelveLaCuentaExistenteSinCrearNada() {
        Usuario existente = usuarioExistente("1234567", CONTRASENA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(existente));

        ResultadoRegistro r = service.registrarPostulante(request());

        assertThat(r.creado()).isFalse();
        assertThat(r.usuario()).isSameAs(existente);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(postulanteRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPostulante_C2_mismoCorreoYCiConOtraContrasena_lanzaErrUs01() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioExistente("1234567", "otra-clave")));
        when(usuarioRepository.existsByCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
    }

    @Test
    void registrarPostulante_C3_correoRegistradoConOtroCi_lanzaErrUs02() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioExistente("999", "otra-clave")));

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_02);
    }

    @Test
    void registrarPostulante_C4_correoNuevoYCiRegistrado_lanzaErrUs01() {
        when(usuarioRepository.existsByCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
    }

    @Test
    void registrarPostulante_C5_registroUniversitarioRegistrado_lanzaErrUs03() {
        when(postulanteRepository.existsByRegistroUniversitario("219012345")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_03);
    }

    @Test
    void registrarPostulante_C6_datosNuevos_creaUsuarioYPostulante() {
        ResultadoRegistro r = service.registrarPostulante(request());

        assertThat(r.creado()).isTrue();
        assertThat(r.usuario().getRol()).isSameAs(rolPostulante);
        verify(usuarioRepository).saveAndFlush(any(Usuario.class));
        verify(postulanteRepository).saveAndFlush(any(Postulante.class));
    }

    @Test
    void registrarPostulante_C7_errorDeIntegridad_seTraduceAlConflictoDeLaRestriccion() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException(
                "violacion", new RuntimeException("duplicate key value violates unique constraint \"uq_usuario_ci\"")));

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
    }

    // ---------------------------------------------------------------- validarCredenciales

    @Test
    void validarCredenciales_C1_correoInexistente_lanzaCredencialesIncorrectas() {
        assertThatThrownBy(() -> service.validarCredenciales(CORREO, CONTRASENA))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_04);
    }

    @Test
    void validarCredenciales_C2_cuentaBloqueada_laNiegaAunqueLaContrasenaSeaCorrecta() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setBloqueadoHasta(Instant.now().plus(Duration.ofMinutes(3)));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarCredenciales(CORREO, CONTRASENA))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_05);
    }

    @Test
    void validarCredenciales_C3_sinBloqueoYContrasenaIncorrecta_registraElIntentoFallido() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarCredenciales(CORREO, "mala"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_04);
        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    void validarCredenciales_C4_bloqueoVencidoYContrasenaIncorrecta_registraElIntentoFallido() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setBloqueadoHasta(Instant.now().minusSeconds(1));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarCredenciales(CORREO, "mala"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_04);
        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    void validarCredenciales_C5_cuentaDesactivada_lanzaErrAuth03() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setActivo(false);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarCredenciales(CORREO, CONTRASENA))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_03);
    }

    @Test
    void validarCredenciales_C6_credencialesCorrectas_devuelveElUsuarioYReiniciaIntentosYBloqueos() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setIntentosFallidos(3);
        usuario.setBloqueosConsecutivos(2);
        usuario.setBloqueadoHasta(Instant.now().minusSeconds(1));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThat(service.validarCredenciales(CORREO, CONTRASENA)).isSameAs(usuario);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueosConsecutivos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
    }

    private Usuario usuarioExistente(String ci, String contrasena) {
        return new Usuario(rolPostulante, ci, CORREO, passwordEncoder.encode(contrasena), "Ana", "Perez");
    }

    private RegistroRequest request() {
        return new RegistroRequest("Ana", "Perez", "1234567", "219012345", carreraCodigo, "70000001",
                CORREO, CONTRASENA, CONTRASENA);
    }
}
