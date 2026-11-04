package com.comedoruagrm.backend.features.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.auth.model.Sesion;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

import jakarta.persistence.EntityManager;

/** Revocar todas las sesiones de una cuenta (CU14). Cada test se revierte (@DataJpaTest). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SesionRepositoryTest {

    @Autowired
    private SesionRepository sesionRepository;

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
    void revocarTodasDe_revocaLasSesionesDeEsaCuentaYNoLasDeOtra() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        Usuario beto = usuario("2", "beto@uagrm.edu.bo");
        sesion(ana, "hash-ana-1");
        sesion(ana, "hash-ana-2");
        sesion(beto, "hash-beto-1");

        int cerradas = sesionRepository.revocarTodasDe(ana.getId(), Instant.now());
        entityManager.clear();

        assertThat(cerradas).isEqualTo(2);
        assertThat(sesionRepository.findByTokenHash("hash-ana-1").orElseThrow().getRevocadaEn()).isNotNull();
        assertThat(sesionRepository.findByTokenHash("hash-ana-2").orElseThrow().getRevocadaEn()).isNotNull();
        assertThat(sesionRepository.findByTokenHash("hash-beto-1").orElseThrow().getRevocadaEn()).isNull();
    }

    @Test
    void revocarTodasDe_noVuelveARevocarLasYaRevocadasYEsIdempotente() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        Sesion yaRevocada = sesion(ana, "hash-vieja");
        Instant antes = Instant.parse("2026-01-01T00:00:00Z");
        yaRevocada.setRevocadaEn(antes);
        sesion(ana, "hash-vigente");
        sesionRepository.flush();

        assertThat(sesionRepository.revocarTodasDe(ana.getId(), Instant.now())).isEqualTo(1);
        assertThat(sesionRepository.revocarTodasDe(ana.getId(), Instant.now())).isZero();
        entityManager.clear();

        assertThat(sesionRepository.findByTokenHash("hash-vieja").orElseThrow().getRevocadaEn()).isEqualTo(antes);
    }

    private Usuario usuario(String ci, String correo) {
        return usuarioRepository.saveAndFlush(new Usuario(
                rolRepository.findByNombre(RolNombres.DIRECTIVO).orElseThrow(), ci, correo,
                "$2a$04$hash-de-prueba", "Nombre", "Apellido"));
    }

    private Sesion sesion(Usuario usuario, String hash) {
        return sesionRepository.saveAndFlush(new Sesion(usuario, hash, Instant.now().plusSeconds(3600)));
    }
}
