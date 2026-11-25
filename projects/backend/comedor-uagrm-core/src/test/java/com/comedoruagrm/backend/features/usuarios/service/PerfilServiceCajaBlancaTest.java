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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ValidationException;
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

/**
 * Pruebas de caja blanca de CU-15 (capitulo 9, 9.3.2): un test por camino basico de
 * `PerfilService.actualizar` (C1 a C10).
 */
class PerfilServiceCajaBlancaTest {

    private static final String CORREO = "ana@uagrm.edu.bo";
    private static final String NUEVO = "nuevo@uagrm.edu.bo";
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
        when(carrera.getNombre()).thenReturn("Ingenieria Informatica");
        when(carrera.getFacultad()).thenReturn("Ciencias de la Computacion");
        postulanteAna = new Postulante(ana, carrera, "219012345", "70000001");
        when(usuarioRepository.findByCodigo(codigo)).thenReturn(Optional.of(ana));
        when(postulanteRepository.findByUsuarioId(any())).thenReturn(Optional.of(postulanteAna));
    }

    private void sinPostulante() {
        Rol rolAdmin = mock(Rol.class);
        when(rolAdmin.getNombre()).thenReturn("ADMINISTRADOR_DUBSS");
        ana = new Usuario(rolAdmin, "7654321", CORREO, passwordEncoder.encode(CLAVE), "Luis", "Rojas");
        when(usuarioRepository.findByCodigo(codigo)).thenReturn(Optional.of(ana));
        when(postulanteRepository.findByUsuarioId(any())).thenReturn(Optional.empty());
    }

    private ResultadoAuditado<PerfilResponse> actualizar(String correo, String telefono, String contrasena) {
        return service.actualizar(codigo, new ActualizarPerfilRequest(correo, telefono, contrasena));
    }

    private void sinGuardar(ResultadoAuditado<PerfilResponse> r) {
        assertThat(r.auditar()).isFalse();
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(postulanteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizar_C1_sinCorreoNiTelefono_devuelveElPerfilSinGuardar() {
        sinGuardar(actualizar(null, null, null));
    }

    @Test
    void actualizar_C2_correoIgualAlActual_noHayCambiosNiSeGuarda() {
        sinGuardar(actualizar("  ANA@uagrm.edu.bo ", null, null));
    }

    @Test
    void actualizar_C3_correoDeOtraCuenta_lanzaErrUs02() {
        when(usuarioRepository.existsByCorreo(NUEVO)).thenReturn(true);

        assertThatThrownBy(() -> actualizar(NUEVO, null, CLAVE)).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UsuarioErrorCodes.ERR_US_02));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizar_C4_correoNuevoDeUnPostulante_guardaUsuarioYPostulante() {
        ResultadoAuditado<PerfilResponse> r = actualizar(NUEVO, null, CLAVE);

        assertThat(ana.getCorreo()).isEqualTo(NUEVO);
        assertThat(r.detalle()).containsEntry("campos", List.of("correo"));
        verify(usuarioRepository).saveAndFlush(ana);
        verify(postulanteRepository).saveAndFlush(postulanteAna);
    }

    @Test
    void actualizar_C5_telefonoEnUnaCuentaSinPostulante_lanzaErrUs13() {
        sinPostulante();

        assertThatThrownBy(() -> actualizar(null, "71111111", null)).isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UsuarioErrorCodes.ERR_US_13));
    }

    @Test
    void actualizar_C6_telefonoDistinto_guardaUsuarioYPostulante() {
        ResultadoAuditado<PerfilResponse> r = actualizar(null, " 71111111 ", null);

        assertThat(postulanteAna.getTelefono()).isEqualTo("71111111");
        assertThat(r.detalle()).containsEntry("campos", List.of("telefono"));
        verify(usuarioRepository).saveAndFlush(ana);
        verify(postulanteRepository).saveAndFlush(postulanteAna);
    }

    @Test
    void actualizar_C7_telefonoIgualAlActual_noHayCambiosNiSeGuarda() {
        sinGuardar(actualizar(null, "70000001", null));
    }

    @Test
    void actualizar_C8_correoNuevoDeUnaCuentaSinPostulante_guardaSoloElUsuario() {
        sinPostulante();

        ResultadoAuditado<PerfilResponse> r = actualizar(NUEVO, null, CLAVE);

        assertThat(ana.getCorreo()).isEqualTo(NUEVO);
        assertThat(r.auditar()).isTrue();
        verify(usuarioRepository).saveAndFlush(ana);
        verify(postulanteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizar_C9_errorDeIntegridadAlGuardarElPostulante_seTraduceAlConflicto() {
        when(postulanteRepository.saveAndFlush(any(Postulante.class))).thenThrow(new DataIntegrityViolationException(
                "violacion", new RuntimeException("duplicate key value violates unique constraint \"uq_usuario_correo\"")));

        assertThatThrownBy(() -> actualizar(NUEVO, null, CLAVE)).isInstanceOf(ConflictException.class);
        verify(usuarioRepository).saveAndFlush(ana);
        verify(postulanteRepository).saveAndFlush(postulanteAna);
    }

    @Test
    void actualizar_C10_errorDeIntegridadAlGuardarElUsuarioSinPostulante_seTraduceAlConflicto() {
        sinPostulante();
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException(
                "violacion", new RuntimeException("duplicate key value violates unique constraint \"uq_usuario_correo\"")));

        assertThatThrownBy(() -> actualizar(NUEVO, null, CLAVE)).isInstanceOf(ConflictException.class);
        verify(postulanteRepository, never()).saveAndFlush(any());
    }
}
