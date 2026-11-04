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
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
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

class UsuarioServiceTest {

    private static final String CORREO = "ana@uagrm.edu.bo";
    private static final String CONTRASENA = "secreta1";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PostulanteRepository postulanteRepository = mock(PostulanteRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final CarreraRepository carreraRepository = mock(CarreraRepository.class);
    // Costo 4 (el minimo): los tests no necesitan un hash lento.
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

    // ---- registrarPostulante ----

    @Test
    void registrarPostulante_conDatosNuevos_creaUsuarioConRolPostulanteYSuPostulante() {
        ResultadoRegistro resultado = service.registrarPostulante(request());

        assertThat(resultado.creado()).isTrue();
        Usuario usuario = resultado.usuario();
        assertThat(usuario.getRol()).isSameAs(rolPostulante);
        assertThat(usuario.getCi()).isEqualTo("1234567");
        assertThat(usuario.getCorreo()).isEqualTo(CORREO);
        assertThat(usuario.isActivo()).isTrue();

        ArgumentCaptor<Postulante> captor = ArgumentCaptor.forClass(Postulante.class);
        verify(postulanteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getCarrera()).isSameAs(carrera);
        assertThat(captor.getValue().getRegistroUniversitario()).isEqualTo("219012345");
    }

    @Test
    void registrarPostulante_guardaLaContrasenaSoloComoHashBcrypt() {
        Usuario usuario = service.registrarPostulante(request()).usuario();

        assertThat(usuario.getPasswordHash()).isNotEqualTo(CONTRASENA).startsWith("$2");
        assertThat(passwordEncoder.matches(CONTRASENA, usuario.getPasswordHash())).isTrue();
    }

    @Test
    void registrarPostulante_normalizaElCorreoAMinusculasYSinEspacios() {
        RegistroRequest conMayusculas = request("  Ana@UAGRM.edu.bo ");

        Usuario usuario = service.registrarPostulante(conMayusculas).usuario();

        assertThat(usuario.getCorreo()).isEqualTo(CORREO);
    }

    @Test
    void registrarPostulante_conElCiYaRegistrado_lanzaConflictExceptionErrUs01() {
        when(usuarioRepository.existsByCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPostulante_conElCorreoYaRegistradoDeOtraPersona_lanzaConflictExceptionErrUs02() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioExistente("999", "otra-clave")));

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_02);
    }

    @Test
    void registrarPostulante_conElRegistroUniversitarioYaRegistrado_lanzaConflictExceptionErrUs03() {
        when(postulanteRepository.existsByRegistroUniversitario("219012345")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_03);
    }

    @Test
    void registrarPostulante_conUnaCarreraInexistente_lanzaNotFoundExceptionErrUs04() {
        UUID otra = UUID.randomUUID();
        when(carreraRepository.findByCodigo(otra)).thenReturn(Optional.empty());
        RegistroRequest conCarreraInexistente = new RegistroRequest("Ana", "Perez", "1234567", "219012345", otra,
                "70000001", CORREO, CONTRASENA, CONTRASENA);

        assertThatThrownBy(() -> service.registrarPostulante(conCarreraInexistente))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_04);
    }

    @Test
    void registrarPostulante_repetidoConLosMismosDatos_devuelveElExistenteSinCrearNada() {
        Usuario existente = usuarioExistente("1234567", CONTRASENA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(existente));

        ResultadoRegistro resultado = service.registrarPostulante(request());

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.usuario()).isSameAs(existente);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(postulanteRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPostulante_conElMismoCorreoYCiPeroOtraContrasena_noLoTomaPorRepetido() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioExistente("1234567", "otra-clave")));
        when(usuarioRepository.existsByCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registrarPostulante_conUnRegistroSimultaneoConElMismoCi_traduceAErrUs01() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException(
                "violacion", new RuntimeException("duplicate key value violates unique constraint \"uq_usuario_ci\"")));

        assertThatThrownBy(() -> service.registrarPostulante(request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
    }

    // ---- validarCredenciales ----

    @Test
    void validarCredenciales_conCredencialesCorrectas_devuelveElUsuario() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThat(service.validarCredenciales(CORREO, CONTRASENA)).isSameAs(usuario);
    }

    @Test
    void validarCredenciales_ignoraMayusculasYEspaciosDelCorreo() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThat(service.validarCredenciales(" ANA@uagrm.edu.bo ", CONTRASENA)).isSameAs(usuario);
    }

    @Test
    void validarCredenciales_conUnCorreoInexistente_lanzaErrAuth04() {
        assertThatThrownBy(() -> service.validarCredenciales(CORREO, CONTRASENA))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_04);
    }

    @Test
    void validarCredenciales_conUnaContrasenaIncorrecta_lanzaElMismoErrorYMensajeQueConUnCorreoInexistente() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioExistente("1234567", CONTRASENA)));

        Throwable conContrasenaMala = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.validarCredenciales(CORREO, "incorrecta"));
        Throwable sinCorreo = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.validarCredenciales("nadie@uagrm.edu.bo", "incorrecta"));

        assertThat(conContrasenaMala).isInstanceOf(UnauthorizedException.class);
        assertThat(conContrasenaMala.getMessage()).isEqualTo(sinCorreo.getMessage());
        assertThat(((BusinessException) conContrasenaMala).getErrorCode())
                .isEqualTo(((BusinessException) sinCorreo).getErrorCode());
    }

    @Test
    void validarCredenciales_conUnaCuentaDesactivada_lanzaForbiddenErrAuth03() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setActivo(false);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarCredenciales(CORREO, CONTRASENA))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_03);
    }

    @Test
    void validarCredenciales_alQuintoIntentoFallido_bloqueaLaCuentaPorCincoMinutos() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        for (int i = 0; i < UsuarioService.MAX_INTENTOS_FALLIDOS; i++) {
            assertThatThrownBy(() -> service.validarCredenciales(CORREO, "mala"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        assertThat(usuario.getBloqueadoHasta()).isAfter(Instant.now().plus(Duration.ofMinutes(4)))
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
        assertThat(usuario.getBloqueosConsecutivos()).isEqualTo(1);
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void validarCredenciales_conLaCuentaBloqueada_laNiegaAunqueLaContrasenaSeaCorrecta() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setBloqueadoHasta(Instant.now().plus(Duration.ofMinutes(3)));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarCredenciales(CORREO, CONTRASENA))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_05);
    }

    @Test
    void validarCredenciales_alVolverAFallarTrasUnBloqueo_elSiguienteBloqueoDuraMas() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setBloqueosConsecutivos(1);
        usuario.setBloqueadoHasta(Instant.now().minusSeconds(1));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        for (int i = 0; i < UsuarioService.MAX_INTENTOS_FALLIDOS; i++) {
            assertThatThrownBy(() -> service.validarCredenciales(CORREO, "mala"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        assertThat(usuario.getBloqueadoHasta()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
        assertThat(usuario.getBloqueosConsecutivos()).isEqualTo(2);
    }

    @Test
    void validarCredenciales_conUnAciertoDespuesDeFallos_reiniciaElContador() {
        Usuario usuario = usuarioExistente("1234567", CONTRASENA);
        usuario.setIntentosFallidos(3);
        usuario.setBloqueosConsecutivos(2);
        usuario.setBloqueadoHasta(Instant.now().minusSeconds(1));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        service.validarCredenciales(CORREO, CONTRASENA);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueosConsecutivos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
    }

    // ---- consultas ----

    @Test
    void obtenerPorCodigo_conUnCodigoInexistente_lanzaUnauthorizedException() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.findByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorCodigo(codigo)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void obtenerResumen_conUnUsuarioExistente_devuelveSusDatosYElNombreDelRol() {
        Usuario usuario = mock(Usuario.class);
        UUID codigo = UUID.randomUUID();
        when(usuario.getCodigo()).thenReturn(codigo);
        when(usuario.getNombres()).thenReturn("Ana");
        when(usuario.getApellidos()).thenReturn("Perez");
        when(usuario.getCorreo()).thenReturn(CORREO);
        when(usuario.getRol()).thenReturn(rolPostulante);
        when(usuarioRepository.findByCodigo(codigo)).thenReturn(Optional.of(usuario));

        var respuesta = service.obtenerResumen(codigo);

        assertThat(respuesta.codigo()).isEqualTo(codigo);
        assertThat(respuesta.rol()).isEqualTo("POSTULANTE");
        assertThat(respuesta.correo()).isEqualTo(CORREO);
    }

    private Usuario usuarioExistente(String ci, String contrasena) {
        return new Usuario(rolPostulante, ci, CORREO, passwordEncoder.encode(contrasena), "Ana", "Perez");
    }

    private RegistroRequest request() {
        return request(CORREO);
    }

    private RegistroRequest request(String correo) {
        return new RegistroRequest("Ana", "Perez", "1234567", "219012345", carreraCodigo, "70000001",
                correo, CONTRASENA, CONTRASENA);
    }
}
