package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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

import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;
import com.comedoruagrm.backend.features.postulaciones.enums.ResultadoAdjudicacion;
import com.comedoruagrm.backend.features.postulaciones.model.Adjudicacion;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Model `Adjudicacion` (CU05) y la lectura de `cantidad_finalistas` de la convocatoria, con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdjudicacionRepositoryTest {

    @Autowired
    private AdjudicacionRepository repository;

    @Autowired
    private ConvocatoriaRepository convocatorias;

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
    void guardarYLeer_conservaElEnumYElPuntaje() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long postulacion = fx.postulacion(fx.convocatoria("ADJM1"), fx.postulante("9510001"), "calificada");
        long admin = fx.usuario("9510002", "ADMINISTRADOR_DUBSS");

        Adjudicacion guardada = repository.saveAndFlush(new Adjudicacion(postulacion,
                ResultadoAdjudicacion.ADJUDICADA, new BigDecimal("77.25"), admin, Instant.now()));

        Adjudicacion leida = repository.findByPostulacionId(postulacion).orElseThrow();
        assertThat(leida.getCodigo()).isEqualTo(guardada.getCodigo()).isNotNull();
        assertThat(leida.getResultado()).isEqualTo(ResultadoAdjudicacion.ADJUDICADA);
        assertThat(leida.getPuntajeFinal()).isEqualByComparingTo("77.25");
        assertThat(leida.getDecididoPor()).isEqualTo(admin);
        assertThat(leida.getFechaAdjudicacion()).isNotNull();
    }

    @Test
    void guardarYLeer_conPuntajeNulo_loPermite() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long postulacion = fx.postulacion(fx.convocatoria("ADJM2"), fx.postulante("9510003"), "calificada");
        long admin = fx.usuario("9510004", "ADMINISTRADOR_DUBSS");

        repository.saveAndFlush(new Adjudicacion(postulacion, ResultadoAdjudicacion.NO_ADJUDICADA, null, admin,
                Instant.now()));

        Adjudicacion leida = repository.findByPostulacionId(postulacion).orElseThrow();
        assertThat(leida.getResultado()).isEqualTo(ResultadoAdjudicacion.NO_ADJUDICADA);
        assertThat(leida.getPuntajeFinal()).isNull();
    }

    @Test
    void unaSegundaAdjudicacionParaLaMismaPostulacion_violaElIndiceUnico() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long postulacion = fx.postulacion(fx.convocatoria("ADJM3"), fx.postulante("9510005"), "calificada");
        long admin = fx.usuario("9510006", "ADMINISTRADOR_DUBSS");
        repository.saveAndFlush(new Adjudicacion(postulacion, ResultadoAdjudicacion.ADJUDICADA,
                new BigDecimal("80"), admin, Instant.now()));

        assertThatThrownBy(() -> repository.saveAndFlush(new Adjudicacion(postulacion,
                ResultadoAdjudicacion.NO_ADJUDICADA, new BigDecimal("80"), admin, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laAdjudicacionEsInmutable_noTieneSetters() {
        for (var metodo : Adjudicacion.class.getDeclaredMethods()) {
            assertThat(metodo.getName()).doesNotStartWith("set");
        }
    }

    @Test
    void convocatoria_leeLaCantidadDeFinalistas() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conConfig = fx.convocatoriaConCupos("ADJM4", 10, "50", 15);
        long sinConfig = fx.convocatoriaConCupos("ADJM5", 10, "50", null);

        Convocatoria a = convocatorias.findById(conConfig).orElseThrow();
        Convocatoria b = convocatorias.findById(sinConfig).orElseThrow();

        assertThat(a.getCantidadFinalistas()).isEqualTo(15);
        assertThat(b.getCantidadFinalistas()).isNull();
    }
}
