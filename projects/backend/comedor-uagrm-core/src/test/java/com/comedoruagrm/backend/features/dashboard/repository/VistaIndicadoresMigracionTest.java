package com.comedoruagrm.backend.features.dashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

/** Migracion de la vista `mv_indicadores_convocatoria` (CU12): que cuenta cada columna, los meses y el refresco. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VistaIndicadoresMigracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    private ExpedienteFixtures fx;
    private long admin;
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
        fx = new ExpedienteFixtures(jdbc);
        admin = fx.usuario("9910001", "ADMINISTRADOR_DUBSS");
        siguienteCi = 9920000;
    }

    private void refrescar() {
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
    }

    /** Una postulacion de un postulante nuevo, enviada en `envio` (ISO) o sin enviar si es nulo (borrador). */
    private long postulacion(long convocatoria, String estado, String envio) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        long id = fx.postulacion(convocatoria, usuario, estado);
        if (envio != null) {
            fx.fechaEnvio(id, envio);
        }
        return id;
    }

    private void publicar(long convocatoria) {
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)", convocatoria,
                admin);
    }

    private void fechaAdjudicacion(long postulacion, String instante) {
        jdbc.update("UPDATE adjudicacion SET fecha_adjudicacion = CAST(? AS timestamptz) WHERE postulacion_id = ?",
                instante, postulacion);
    }

    private List<Map<String, Object>> filasDe(long convocatoria) {
        return jdbc.queryForList("SELECT mes, postulaciones_recibidas, finalistas, adjudicadas "
                + "FROM mv_indicadores_convocatoria WHERE convocatoria_id = ? ORDER BY mes", convocatoria);
    }

    private static LocalDate mes(Map<String, Object> fila) {
        return ((Date) fila.get("mes")).toLocalDate();
    }

    @Test
    void laVistaExiste_conSusColumnasEIndices() {
        assertThat(jdbc.queryForList("SELECT attname FROM pg_attribute WHERE attrelid = "
                + "'mv_indicadores_convocatoria'::regclass AND attnum > 0 AND NOT attisdropped ORDER BY attnum",
                String.class))
                .containsExactly("convocatoria_id", "gestion_id", "mes", "postulaciones_recibidas", "finalistas",
                        "adjudicadas", "calculado_en");
        assertThat(jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename = "
                + "'mv_indicadores_convocatoria'", String.class))
                .contains("uq_mv_indicadores_convocatoria_mes", "ix_mv_indicadores_convocatoria_gestion");
    }

    @Test
    void postulaciones_cuentanLasEnviadasPorMes_yNoLosBorradores() {
        long c = fx.convocatoria("IND1");
        postulacion(c, "pendiente", "2026-09-10T15:00:00Z");
        postulacion(c, "calificada", "2026-09-20T15:00:00Z");
        postulacion(c, "pendiente", "2026-10-05T15:00:00Z");
        postulacion(c, "borrador", null);

        refrescar();

        List<Map<String, Object>> filas = filasDe(c);
        assertThat(filas).hasSize(2);
        assertThat(mes(filas.get(0))).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(filas.get(0).get("postulaciones_recibidas")).isEqualTo(2);
        assertThat(mes(filas.get(1))).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(filas.get(1).get("postulaciones_recibidas")).isEqualTo(1);
    }

    @Test
    void elMesSeCalculaEnLaPaz_noEnUtc() {
        long c = fx.convocatoria("IND2");
        // 01:00 UTC del 1 de octubre son las 21:00 del 30 de septiembre en La Paz (UTC-4).
        postulacion(c, "pendiente", "2026-10-01T01:00:00Z");

        refrescar();

        assertThat(mes(filasDe(c).get(0))).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void postulacionesEliminadas_noCuentan() {
        long c = fx.convocatoria("IND3");
        long eliminada = postulacion(c, "pendiente", "2026-09-10T15:00:00Z");
        postulacion(c, "pendiente", "2026-09-11T15:00:00Z");
        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", eliminada);

        refrescar();

        assertThat(filasDe(c).get(0).get("postulaciones_recibidas")).isEqualTo(1);
    }

    @Test
    void finalistas_cuentanPorElMesDeSuCreacion() {
        long c = fx.convocatoria("IND4");
        long p1 = postulacion(c, "calificada", "2026-09-10T15:00:00Z");
        long p2 = postulacion(c, "calificada", "2026-09-11T15:00:00Z");
        long f1 = fx.finalista(p1, 1);
        long f2 = fx.finalista(p2, 2);
        jdbc.update("UPDATE finalista SET creado_en = CAST('2026-10-03T15:00:00Z' AS timestamptz) WHERE id IN (?, ?)",
                f1, f2);

        refrescar();

        List<Map<String, Object>> filas = filasDe(c);
        assertThat(filas).hasSize(2);
        assertThat(filas.get(1).get("finalistas")).isEqualTo(2);
        assertThat(filas.get(0).get("finalistas")).isEqualTo(0);
    }

    @Test
    void adjudicadas_soloSumanEnConvocatoriasConResultadosPublicados() {
        long publicada = fx.convocatoria("IND5A");
        long sinPublicar = fx.convocatoria("IND5B");
        long a = postulacion(publicada, "adjudicada", "2026-09-10T15:00:00Z");
        long b = postulacion(sinPublicar, "adjudicada", "2026-09-10T15:00:00Z");
        fx.adjudicacion(a, "ADJUDICADA", "80.00", admin);
        fx.adjudicacion(b, "ADJUDICADA", "80.00", admin);
        fechaAdjudicacion(a, "2026-11-02T15:00:00Z");
        fechaAdjudicacion(b, "2026-11-02T15:00:00Z");
        publicar(publicada);

        refrescar();

        List<Map<String, Object>> conPublicacion = filasDe(publicada);
        assertThat(conPublicacion.stream().mapToInt(f -> (Integer) f.get("adjudicadas")).sum()).isEqualTo(1);
        assertThat(conPublicacion).anySatisfy(f -> assertThat(mes(f)).isEqualTo(LocalDate.of(2026, 11, 1)));
        assertThat(filasDe(sinPublicar).stream().mapToInt(f -> (Integer) f.get("adjudicadas")).sum()).isZero();
    }

    @Test
    void adjudicadas_noCuentanLasNoAdjudicadasNiLasEliminadas_niConPublicacionEliminada() {
        long c = fx.convocatoria("IND6");
        long no = postulacion(c, "no_adjudicada", "2026-09-10T15:00:00Z");
        long si = postulacion(c, "adjudicada", "2026-09-10T15:00:00Z");
        fx.adjudicacion(no, "NO_ADJUDICADA", "40.00", admin);
        fx.adjudicacion(si, "ADJUDICADA", "80.00", admin);
        publicar(c);
        jdbc.update("UPDATE adjudicacion SET eliminado_en = now() WHERE postulacion_id = ?", si);

        refrescar();

        assertThat(filasDe(c).stream().mapToInt(f -> (Integer) f.get("adjudicadas")).sum()).isZero();
    }

    @Test
    void unaPublicacionEliminada_noHabilitaLasAdjudicadas() {
        long c = fx.convocatoria("IND7");
        long p = postulacion(c, "adjudicada", "2026-09-10T15:00:00Z");
        fx.adjudicacion(p, "ADJUDICADA", "80.00", admin);
        publicar(c);
        jdbc.update("UPDATE publicacion_resultado SET eliminado_en = now() WHERE convocatoria_id = ?", c);

        refrescar();

        assertThat(filasDe(c).stream().mapToInt(f -> (Integer) f.get("adjudicadas")).sum()).isZero();
    }

    @Test
    void unaConvocatoriaSinNada_noTieneFilas() {
        long c = fx.convocatoria("IND8");

        refrescar();

        assertThat(filasDe(c)).isEmpty();
    }

    @Test
    void cadaFilaTraeSuGestion_yLaVistaAceptaRefreshConcurrently() {
        long c = fx.convocatoria("IND9");
        postulacion(c, "pendiente", "2026-09-10T15:00:00Z");
        refrescar();
        long gestion = jdbc.queryForObject("SELECT gestion_id FROM convocatoria WHERE id = ?", Long.class, c);

        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_indicadores_convocatoria");

        assertThat(jdbc.queryForObject("SELECT gestion_id FROM mv_indicadores_convocatoria WHERE convocatoria_id = ?",
                Long.class, c)).isEqualTo(gestion);
    }

    @Test
    void elRefrescoCambiaCalculadoEnYReflejaLoNuevo() {
        long c = fx.convocatoria("IND10");
        postulacion(c, "pendiente", "2026-09-10T15:00:00Z");
        refrescar();
        Object antes = jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria",
                java.sql.Timestamp.class);
        postulacion(c, "pendiente", "2026-09-12T15:00:00Z");
        assertThat(filasDe(c).get(0).get("postulaciones_recibidas")).isEqualTo(1); // la vista no se mueve sola

        // Dentro de una transaccion `now()` es siempre el mismo instante: se avanza el reloj de la prueba con una pausa.
        jdbc.execute("SELECT pg_sleep(0.05)");
        refrescar();

        assertThat(filasDe(c).get(0).get("postulaciones_recibidas")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria",
                java.sql.Timestamp.class)).isAfterOrEqualTo((java.sql.Timestamp) antes);
    }
}
