package com.comedoruagrm.backend.features.usuarios.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

import jakarta.persistence.EntityManager;

/** Columnas del enlace de restablecimiento de `usuario` (CU15). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRestablecimientoRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

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
    void iniciarRestablecimiento_guardaElHashYSePuedeBuscar() {
        Usuario ana = usuarioRepository.saveAndFlush(nuevoUsuario("5000001", "ana.reset@uagrm.edu.bo"));
        ana.iniciarRestablecimiento("a".repeat(64), Instant.now().plus(60, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(ana);
        entityManager.clear();

        Usuario encontrada = usuarioRepository.findByRestablecimientoTokenHash("a".repeat(64)).orElseThrow();

        assertThat(encontrada.getCodigo()).isEqualTo(ana.getCodigo());
        assertThat(encontrada.tieneRestablecimientoVigente(Instant.now())).isTrue();
    }

    @Test
    void iniciarRestablecimiento_dosVeces_dejaSoloElUltimoHash() {
        Usuario ana = usuarioRepository.saveAndFlush(nuevoUsuario("5000002", "ana.dos@uagrm.edu.bo"));
        ana.iniciarRestablecimiento("1".repeat(64), Instant.now().plus(60, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(ana);
        ana.iniciarRestablecimiento("2".repeat(64), Instant.now().plus(60, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(ana);
        entityManager.clear();

        assertThat(usuarioRepository.findByRestablecimientoTokenHash("1".repeat(64))).isEmpty();
        assertThat(usuarioRepository.findByRestablecimientoTokenHash("2".repeat(64))).isPresent();
    }

    @Test
    void limpiarRestablecimiento_dejaLasDosColumnasEnNull() {
        Usuario ana = usuarioRepository.saveAndFlush(nuevoUsuario("5000003", "ana.limpia@uagrm.edu.bo"));
        ana.iniciarRestablecimiento("b".repeat(64), Instant.now().plus(60, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(ana);
        ana.limpiarRestablecimiento();
        usuarioRepository.saveAndFlush(ana);
        entityManager.clear();

        Usuario recargada = usuarioRepository.findByCodigo(ana.getCodigo()).orElseThrow();
        assertThat(recargada.getRestablecimientoTokenHash()).isNull();
        assertThat(recargada.getRestablecimientoExpiraEn()).isNull();
        assertThat(usuarioRepository.findByRestablecimientoTokenHash("b".repeat(64))).isEmpty();
    }

    @Test
    void tieneRestablecimientoVigente_distingueVigenteVencidoYSinEnlace() {
        Usuario ana = nuevoUsuario("5000004", "ana.vigente@uagrm.edu.bo");
        Instant ahora = Instant.now();
        assertThat(ana.tieneRestablecimientoVigente(ahora)).isFalse();

        ana.iniciarRestablecimiento("c".repeat(64), ahora.plusSeconds(60));
        assertThat(ana.tieneRestablecimientoVigente(ahora)).isTrue();

        ana.iniciarRestablecimiento("c".repeat(64), ahora.minusSeconds(1));
        assertThat(ana.tieneRestablecimientoVigente(ahora)).isFalse();
    }

    @Test
    void guardar_dosCuentasConElMismoHash_lanzaDataIntegrityViolationException() {
        Usuario uno = usuarioRepository.saveAndFlush(nuevoUsuario("5000005", "uno.hash@uagrm.edu.bo"));
        uno.iniciarRestablecimiento("d".repeat(64), Instant.now().plus(60, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(uno);

        Usuario dos = usuarioRepository.saveAndFlush(nuevoUsuario("5000006", "dos.hash@uagrm.edu.bo"));
        dos.iniciarRestablecimiento("d".repeat(64), Instant.now().plus(60, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(dos))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Usuario nuevoUsuario(String ci, String correo) {
        var rol = rolRepository.findByNombre(RolNombres.POSTULANTE).orElseThrow();
        return new Usuario(rol, ci, correo, "$2a$04$hash-de-prueba", "Ana", "Perez");
    }
}
