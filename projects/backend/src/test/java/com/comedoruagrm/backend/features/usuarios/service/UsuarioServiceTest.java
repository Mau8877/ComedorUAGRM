package com.comedoruagrm.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.usuarios.RolNombres;
import com.comedoruagrm.backend.features.usuarios.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.model.Postulante;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.CarreraRepository;
import com.comedoruagrm.backend.features.usuarios.repository.PostulanteRepository;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;

class UsuarioServiceTest {

    private static final String UID = "uid-ana";
    private static final String CORREO = "ana@uagrm.edu.bo";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PostulanteRepository postulanteRepository = mock(PostulanteRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final CarreraRepository carreraRepository = mock(CarreraRepository.class);
    private final UsuarioService service = new UsuarioService(
            usuarioRepository, postulanteRepository, rolRepository, carreraRepository);

    private final Rol rolPostulante = mock(Rol.class);
    private final Carrera carrera = mock(Carrera.class);
    private final UUID carreraCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        when(rolPostulante.getNombre()).thenReturn(RolNombres.POSTULANTE);
        when(rolRepository.findByNombre(RolNombres.POSTULANTE)).thenReturn(Optional.of(rolPostulante));
        when(carreraRepository.findByCodigo(carreraCodigo)).thenReturn(Optional.of(carrera));
        when(usuarioRepository.findByFirebaseUid(UID)).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postulanteRepository.saveAndFlush(any(Postulante.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrarPostulante_conDatosNuevos_creaUsuarioConRolPostulanteYSuPostulante() {
        ResultadoRegistro resultado = service.registrarPostulante(UID, CORREO, request());

        assertThat(resultado.creado()).isTrue();
        Usuario usuario = resultado.usuario();
        assertThat(usuario.getRol()).isSameAs(rolPostulante);
        assertThat(usuario.getCi()).isEqualTo("1234567");
        assertThat(usuario.getCorreo()).isEqualTo(CORREO);
        assertThat(usuario.getFirebaseUid()).isEqualTo(UID);
        assertThat(usuario.isActivo()).isTrue();

        ArgumentCaptor<Postulante> captor = ArgumentCaptor.forClass(Postulante.class);
        verify(postulanteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getCarrera()).isSameAs(carrera);
        assertThat(captor.getValue().getRegistroUniversitario()).isEqualTo("219012345");
    }

    @Test
    void registrarPostulante_conElCiYaRegistrado_lanzaConflictExceptionErrUs01() {
        when(usuarioRepository.existsByCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(UID, CORREO, request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPostulante_conElCorreoYaRegistrado_lanzaConflictExceptionErrUs02() {
        when(usuarioRepository.existsByCorreo(CORREO)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(UID, CORREO, request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_02);
    }

    @Test
    void registrarPostulante_conElRegistroUniversitarioYaRegistrado_lanzaConflictExceptionErrUs03() {
        when(postulanteRepository.existsByRegistroUniversitario("219012345")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPostulante(UID, CORREO, request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_03);
    }

    @Test
    void registrarPostulante_conUnaCarreraInexistente_lanzaNotFoundExceptionErrUs04() {
        UUID otra = UUID.randomUUID();
        when(carreraRepository.findByCodigo(otra)).thenReturn(Optional.empty());
        RegistroRequest conCarreraInexistente = new RegistroRequest("Ana", "Perez", "1234567", "219012345", otra,
                "70000001");

        assertThatThrownBy(() -> service.registrarPostulante(UID, CORREO, conCarreraInexistente))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_04);
    }

    @Test
    void registrarPostulante_conElMismoFirebaseUid_devuelveElExistenteSinCrearNada() {
        Usuario existente = mock(Usuario.class);
        when(usuarioRepository.findByFirebaseUid(UID)).thenReturn(Optional.of(existente));

        ResultadoRegistro resultado = service.registrarPostulante(UID, CORREO, request());

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.usuario()).isSameAs(existente);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(postulanteRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPostulante_conUnRegistroSimultaneoConElMismoCi_traduceAErrUs01() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException(
                "violacion", new RuntimeException("duplicate key value violates unique constraint \"uq_usuario_ci\"")));

        assertThatThrownBy(() -> service.registrarPostulante(UID, CORREO, request()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UsuarioErrorCodes.ERR_US_01);
    }

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

    private RegistroRequest request() {
        return new RegistroRequest("Ana", "Perez", "1234567", "219012345", carreraCodigo, "70000001");
    }
}
