package com.comedoruagrm.backend.features.actas.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

import com.comedoruagrm.backend.features.actas.model.ActaAdjudicacion;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Model `ActaAdjudicacion` (CU08) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActaAdjudicacionRepositoryTest {

    @Autowired
    private ActaAdjudicacionRepository repository;

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
    void guardarYLeer_conservaLosCincoValoresYGeneraElCodigoPublico() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convocatoria = fx.convocatoria("ACTR1");
        long admin = fx.usuario("9710001", "ADMINISTRADOR_DUBSS");
        Instant cuando = Instant.parse("2026-11-13T15:00:00Z");

        ActaAdjudicacion guardada = repository.saveAndFlush(
                new ActaAdjudicacion(convocatoria, "001/2026", "actas/x/acta-001-2026.pdf", admin, cuando));

        ActaAdjudicacion leida = repository.findByCodigo(guardada.getCodigo()).orElseThrow();
        assertThat(leida.getCodigo()).isNotNull();
        assertThat(leida.getConvocatoriaId()).isEqualTo(convocatoria);
        assertThat(leida.getNumero()).isEqualTo("001/2026");
        assertThat(leida.getStoragePath()).isEqualTo("actas/x/acta-001-2026.pdf");
        assertThat(leida.getGeneradaPor()).isEqualTo(admin);
        assertThat(leida.getGeneradaEn()).isEqualTo(cuando);
        assertThat(repository.findByConvocatoriaId(convocatoria)).isPresent();
    }

    @Test
    void existsByConvocatoriaId_distingueLaConvocatoriaConActaDeLaQueNo() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conActa = fx.convocatoria("ACTR2");
        long sinActa = fx.convocatoria("ACTR3");
        long admin = fx.usuario("9710002", "ADMINISTRADOR_DUBSS");
        repository.saveAndFlush(new ActaAdjudicacion(conActa, "001/2026", "a.pdf", admin, Instant.now()));

        assertThat(repository.existsByConvocatoriaId(conActa)).isTrue();
        assertThat(repository.existsByConvocatoriaId(sinActa)).isFalse();
        assertThat(repository.findByConvocatoriaId(sinActa)).isEmpty();
    }

    @Test
    void guardar_unaSegundaParaLaMismaConvocatoria_fallaPorLaUnicidad() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convocatoria = fx.convocatoria("ACTR4");
        long admin = fx.usuario("9710003", "ADMINISTRADOR_DUBSS");
        repository.saveAndFlush(new ActaAdjudicacion(convocatoria, "001/2026", "a.pdf", admin, Instant.now()));

        assertThatThrownBy(() -> repository
                .saveAndFlush(new ActaAdjudicacion(convocatoria, "002/2026", "b.pdf", admin, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void borradoLogico_excluyeLaFilaDeLasConsultas() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convocatoria = fx.convocatoria("ACTR5");
        long admin = fx.usuario("9710004", "ADMINISTRADOR_DUBSS");
        ActaAdjudicacion guardada = repository
                .saveAndFlush(new ActaAdjudicacion(convocatoria, "001/2026", "a.pdf", admin, Instant.now()));
        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE id = ?", guardada.getId());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM acta_adjudicacion WHERE convocatoria_id = ? "
                + "AND eliminado_en IS NULL", Integer.class, convocatoria)).isZero();
    }

    @Test
    void modelo_noTieneNingunSetter() {
        for (Method metodo : ActaAdjudicacion.class.getDeclaredMethods()) {
            if (Modifier.isPublic(metodo.getModifiers())) {
                assertThat(metodo.getName()).doesNotStartWith("set");
            }
        }
    }
}
