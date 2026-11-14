package com.comedoruagrm.backend.features.dashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.TotalesFila;
import com.comedoruagrm.backend.features.dashboard.service.FiltrosDashboardService;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresService;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Las consultas de indicadores (CU12) contra Postgres real: salen de la vista y son rapidas. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IndicadoresRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    private IndicadoresRepository repositorio;
    private IndicadoresService servicio;
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
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbc);
        repositorio = new IndicadoresRepository(named);
        FiltrosDashboardService filtros = new FiltrosDashboardService(new FiltrosDashboardRepository(named),
                Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC));
        servicio = new IndicadoresService(repositorio, filtros);
        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 9940000;
    }

    private void postulacionEnviada(long convocatoria, String envio) {
        long p = fx.postulacion(convocatoria, fx.postulante(String.valueOf(siguienteCi++)), "pendiente");
        fx.fechaEnvio(p, envio);
    }

    private UUID codigoGestionDe(long convocatoria) {
        return jdbc.queryForObject("SELECT g.codigo_gestion_academica FROM convocatoria c JOIN gestion_academica g "
                + "ON g.id = c.gestion_id WHERE c.id = ?", UUID.class, convocatoria);
    }

    private UUID codigoConvocatoria(long convocatoria) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoria);
    }

    @Test
    void resuelveGestionYConvocatoriaPorCodigo_yIgnoraLasEliminadas() {
        long c = fx.convocatoria("IR1");

        assertThat(repositorio.gestion(codigoGestionDe(c))).isPresent();
        ConvocatoriaDatos datos = repositorio.convocatoria(codigoConvocatoria(c)).orElseThrow();
        assertThat(datos.gestion().codigo()).isEqualTo(codigoGestionDe(c));
        assertThat(repositorio.gestion(UUID.randomUUID())).isEmpty();
        assertThat(repositorio.convocatoria(UUID.randomUUID())).isEmpty();

        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", c);
        assertThat(repositorio.convocatoria(codigoConvocatoria(c))).isEmpty();
    }

    @Test
    void losNumerosSalenDeLaVista_noDeLasTablas() {
        long c = fx.convocatoria("IR2");
        postulacionEnviada(c, "2026-09-10T15:00:00Z");
        postulacionEnviada(c, "2026-10-10T15:00:00Z");
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
        long gestionId = jdbc.queryForObject("SELECT gestion_id FROM convocatoria WHERE id = ?", Long.class, c);

        // Una postulacion nueva sin refrescar la vista: los indicadores no la ven.
        postulacionEnviada(c, "2026-10-12T15:00:00Z");

        TotalesFila antes = repositorio.totales(gestionId, null);
        assertThat(antes.postulaciones()).isEqualTo(2);
        assertThat(repositorio.serieMensual(gestionId, null)).hasSize(2);
        assertThat(repositorio.ultimaActualizacion()).isPresent();

        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
        assertThat(repositorio.totales(gestionId, null).postulaciones()).isEqualTo(3);
    }

    @Test
    void elFiltroPorConvocatoria_limitaLosTotales() {
        long c = fx.convocatoria("IR3");
        postulacionEnviada(c, "2026-09-10T15:00:00Z");
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
        long gestionId = jdbc.queryForObject("SELECT gestion_id FROM convocatoria WHERE id = ?", Long.class, c);

        assertThat(repositorio.totales(gestionId, c).postulaciones()).isEqualTo(1);
        assertThat(repositorio.totales(gestionId, c + 100_000).postulaciones()).isZero();
        assertThat(repositorio.serieMensual(gestionId, c + 100_000)).isEmpty();
    }

    @Test
    void consultarConVeinteMilPostulaciones_respondeEnMenosDeTresSegundos() {
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES ('G-PERF', DATE '2036-01-01', DATE '2036-12-31') RETURNING id""", Long.class);
        jdbc.update("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                SELECT ?, 'Perf ' || n, TIMESTAMPTZ '2036-01-01 00:00:00+00' + (n || ' days')::interval,
                       TIMESTAMPTZ '2036-12-01 00:00:00+00', CAST('CERRADA' AS estado_convocatoria)
                FROM generate_series(1, 100) n""", gestion);
        for (int i = 0; i < 200; i++) {
            fx.postulante(String.valueOf(siguienteCi++));
        }
        jdbc.update("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, numero_envios, fecha_envio)
                SELECT c.id, p.usuario_id, CAST('pendiente' AS estado_postulacion), 1,
                       TIMESTAMPTZ '2036-01-15 12:00:00+00' + ((p.usuario_id % 10) || ' months')::interval
                FROM convocatoria c CROSS JOIN postulante p
                WHERE c.gestion_id = ? AND p.registro_universitario LIKE 'RU-994%'""", gestion);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM postulacion p JOIN convocatoria c ON c.id = "
                + "p.convocatoria_id WHERE c.gestion_id = ?", Long.class, gestion)).isGreaterThanOrEqualTo(20_000L);
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
        UUID codigoGestion = jdbc.queryForObject("SELECT codigo_gestion_academica FROM gestion_academica WHERE id = ?",
                UUID.class, gestion);

        long inicio = System.nanoTime();
        IndicadoresResponse r = servicio.consultar(codigoGestion, null);
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(r.totales().postulacionesRecibidas()).isGreaterThanOrEqualTo(20_000L);
        assertThat(r.porMes()).isNotEmpty();
        assertThat(tardo).isLessThan(Duration.ofSeconds(3));
    }
}
