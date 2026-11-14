package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** El refresco de la vista de indicadores (CU12) contra Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefrescoIndicadoresBaseRealTest {

    @Autowired
    private JdbcTemplate jdbc;

    private RefrescoIndicadoresService service;
    private ExpedienteFixtures fx;
    private int siguienteCi;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @BeforeEach
    void preparar() {
        service = new RefrescoIndicadoresService(jdbc);
        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 9930000;
    }

    private long convocatoriaConPostulacion(String etiqueta) {
        long c = fx.convocatoria(etiqueta);
        long p = fx.postulacion(c, fx.postulante(String.valueOf(siguienteCi++)), "pendiente");
        fx.fechaEnvio(p, "2026-09-10T15:00:00Z");
        return c;
    }

    private int recibidas(long convocatoria) {
        return jdbc.queryForObject("SELECT coalesce(sum(postulaciones_recibidas), 0) FROM "
                + "mv_indicadores_convocatoria WHERE convocatoria_id = ?", Integer.class, convocatoria);
    }

    @Test
    void refrescar_reflejaLasFilasNuevas_yAvanzaCalculadoEn() {
        long c = convocatoriaConPostulacion("RFB1");
        assertThat(service.refrescar()).isTrue();
        Timestamp antes = jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria",
                Timestamp.class);
        long otra = fx.postulacion(c, fx.postulante(String.valueOf(siguienteCi++)), "pendiente");
        fx.fechaEnvio(otra, "2026-09-12T15:00:00Z");
        assertThat(recibidas(c)).isEqualTo(1);

        jdbc.execute("SELECT pg_sleep(0.05)");
        assertThat(service.refrescar()).isTrue();

        assertThat(recibidas(c)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria", Timestamp.class))
                .isAfterOrEqualTo(antes);
    }

    @Test
    void refrescarSiEstaVacia_conLaVistaVaciaLaPuebla_yConDatosNoHaceNada() {
        long c = convocatoriaConPostulacion("RFB2");
        // La vista se creo al migrar, antes de estos datos: esta vacia aunque las tablas tengan filas.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mv_indicadores_convocatoria", Long.class)).isZero();

        assertThat(service.refrescarSiEstaVacia()).isTrue();

        assertThat(recibidas(c)).isEqualTo(1);
        long nueva = fx.postulacion(c, fx.postulante(String.valueOf(siguienteCi++)), "pendiente");
        fx.fechaEnvio(nueva, "2026-09-13T15:00:00Z");
        assertThat(service.refrescarSiEstaVacia()).isFalse();
        assertThat(recibidas(c)).isEqualTo(1);
    }
}
