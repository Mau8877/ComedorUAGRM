package com.comedoruagrm.backend.features.usuarios.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.model.Postulante;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

import jakarta.persistence.EntityManager;

/** Cada test corre dentro de una transaccion que se revierte (comportamiento de @DataJpaTest). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioPostulanteRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PostulanteRepository postulanteRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CarreraRepository carreraRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void guardar_unUsuarioYSuPostulante_losPuedeBuscarPorLosDatosUnicos() {
        Usuario usuario = usuarioRepository.saveAndFlush(nuevoUsuario("1234567", "ana@uagrm.edu.bo"));
        postulanteRepository.saveAndFlush(new Postulante(usuario, unaCarrera(), "219012345", "70000001"));
        entityManager.clear();

        Usuario encontrado = usuarioRepository.findByCorreo("ana@uagrm.edu.bo").orElseThrow();

        assertThat(encontrado.getCodigo()).isNotNull();
        assertThat(encontrado.isActivo()).isTrue();
        assertThat(encontrado.getPasswordHash()).isEqualTo("$2a$04$hash-de-prueba");
        assertThat(encontrado.getIntentosFallidos()).isZero();
        assertThat(encontrado.getBloqueadoHasta()).isNull();
        assertThat(encontrado.getRol().getNombre()).isEqualTo(RolNombres.POSTULANTE);
        assertThat(usuarioRepository.existsByCi("1234567")).isTrue();
        assertThat(usuarioRepository.existsByCorreo("ana@uagrm.edu.bo")).isTrue();
        assertThat(postulanteRepository.existsByRegistroUniversitario("219012345")).isTrue();
        assertThat(postulanteRepository.findById(usuario.getId())).isPresent();
    }

    @Test
    void guardar_conCiDuplicado_lanzaDataIntegrityViolationException() {
        usuarioRepository.saveAndFlush(nuevoUsuario("7654321", "uno@uagrm.edu.bo"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(nuevoUsuario("7654321", "dos@uagrm.edu.bo")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByCorreo_conUsuarioEliminadoLogicamente_noLoEncuentra() {
        Usuario usuario = usuarioRepository.saveAndFlush(nuevoUsuario("1111111", "baja@uagrm.edu.bo"));
        usuario.setEliminadoEn(Instant.now());
        usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();

        assertThat(usuarioRepository.findByCorreo("baja@uagrm.edu.bo")).isEmpty();
        assertThat(usuarioRepository.existsByCi("1111111")).isFalse();
    }

    @Test
    void guardar_conElMismoCiQueUnUsuarioEliminado_loPermite() {
        Usuario viejo = usuarioRepository.saveAndFlush(nuevoUsuario("2222222", "viejo@uagrm.edu.bo"));
        viejo.setEliminadoEn(Instant.now());
        usuarioRepository.saveAndFlush(viejo);

        Usuario nuevo = usuarioRepository.saveAndFlush(nuevoUsuario("2222222", "nuevo@uagrm.edu.bo"));

        assertThat(nuevo.getId()).isNotNull();
    }

    private Usuario nuevoUsuario(String ci, String correo) {
        Rol rol = rolRepository.findByNombre(RolNombres.POSTULANTE).orElseThrow();
        return new Usuario(rol, ci, correo, "$2a$04$hash-de-prueba", "Ana", "Perez");
    }

    private Carrera unaCarrera() {
        return carreraRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);
    }
}
