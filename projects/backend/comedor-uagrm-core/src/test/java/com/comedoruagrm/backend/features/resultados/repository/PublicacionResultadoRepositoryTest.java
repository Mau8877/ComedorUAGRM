package com.comedoruagrm.backend.features.resultados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.resultados.model.PublicacionResultado;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Model `PublicacionResultado` (CU22) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PublicacionResultadoRepositoryTest {

    @Autowired
    private PublicacionResultadoRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void guardarYLeer_conservaLosTresValoresYGeneraElCodigoPublico() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convocatoria = fx.convocatoria("PUBR1");
        long admin = fx.usuario("9610001", "ADMINISTRADOR_DUBSS");
        Instant cuando = Instant.parse("2026-11-12T15:00:00Z");

        PublicacionResultado guardada = repository.saveAndFlush(new PublicacionResultado(convocatoria, admin, cuando));

        PublicacionResultado leida = repository.findByCodigo(guardada.getCodigo()).orElseThrow();
        assertThat(leida.getCodigo()).isNotNull();
        assertThat(leida.getConvocatoriaId()).isEqualTo(convocatoria);
        assertThat(leida.getPublicadoPor()).isEqualTo(admin);
        assertThat(leida.getPublicadoEn()).isEqualTo(cuando);
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(leida.getEliminadoEn()).isNull();
    }

    @Test
    void existsByConvocatoriaId_distingueLaConvocatoriaPublicadaDeLaQueNo() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long publicada = fx.convocatoria("PUBR2");
        long otra = fx.convocatoria("PUBR3");
        long admin = fx.usuario("9610002", "ADMINISTRADOR_DUBSS");
        repository.saveAndFlush(new PublicacionResultado(publicada, admin, Instant.now()));

        assertThat(repository.existsByConvocatoriaId(publicada)).isTrue();
        assertThat(repository.existsByConvocatoriaId(otra)).isFalse();
    }

    @Test
    void guardar_unaSegundaParaLaMismaConvocatoria_fallaPorLaUnicidad() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convocatoria = fx.convocatoria("PUBR4");
        long admin = fx.usuario("9610003", "ADMINISTRADOR_DUBSS");
        repository.saveAndFlush(new PublicacionResultado(convocatoria, admin, Instant.now()));

        assertThatThrownBy(() -> repository.saveAndFlush(new PublicacionResultado(convocatoria, admin, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
