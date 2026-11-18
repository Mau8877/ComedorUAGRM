package com.comedoruagrm.backend.features.dashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

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

import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaAsistenciaBi;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaCarrera;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaConvocatoria;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaTramo;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Las vistas `mv_bi_*` del dashboard BI (CU12) contra Postgres real: que cuenta cada columna, que las adjudicadas
 * solo valen con resultados publicados, la asistencia, las facultades y que las consultas leen de la vista.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VistasBiMigracionTest {

    private static final List<String> VISTAS = List.of("mv_indicadores_convocatoria", "mv_bi_asistencia_dia",
            "mv_bi_convocatoria", "mv_bi_carrera_convocatoria", "mv_bi_puntaje_convocatoria");

    @Autowired
    private JdbcTemplate jdbc;

    private BiRepository repositorio;
    private ExpedienteFixtures fx;
    private long admin;
    private int siguienteCi;
    private long carreraA;
    private long carreraB;

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
        repositorio = new BiRepository(new NamedParameterJdbcTemplate(jdbc));
        fx = new ExpedienteFixtures(jdbc);
        admin = fx.usuario("9930001", "ADMINISTRADOR_DUBSS");
        siguienteCi = 9931000;
        carreraA = jdbc.queryForObject("SELECT min(id) FROM carrera", Long.class);
        carreraB = jdbc.queryForObject("SELECT min(id) FROM carrera WHERE facultad <> "
                + "(SELECT facultad FROM carrera WHERE id = ?)", Long.class, carreraA);
    }

    private void refrescar() {
        // Mismo orden que el lote nocturno: mv_bi_convocatoria lee mv_bi_asistencia_dia.
        VISTAS.forEach(v -> jdbc.execute("REFRESH MATERIALIZED VIEW " + v));
    }

    /** Un postulante de la carrera dada con su postulacion enviada; devuelve [usuarioId, postulacionId]. */
    private long[] postulacion(long convocatoria, long carrera, String estado) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE postulante SET carrera_id = ? WHERE usuario_id = ?", carrera, usuario);
        long id = fx.postulacion(convocatoria, usuario, estado);
        fx.fechaEnvio(id, "2026-09-10T15:00:00Z");
        return new long[] {usuario, id};
    }

    private long beca(long convocatoria, long usuario, String estado, String inicio) {
        return jdbc.queryForObject("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), CAST(? AS date)) RETURNING id""", Long.class, convocatoria,
                usuario, estado, inicio);
    }

    private void asistencia(long beca, String fecha) {
        jdbc.update("INSERT INTO asistencia (beca_id, registrado_por, fecha) VALUES (?, ?, CAST(? AS date))", beca,
                admin, fecha);
    }

    private void publicar(long convocatoria) {
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en) "
                + "VALUES (?, ?, now())", convocatoria, admin);
    }

    /** Una convocatoria con 3 postulaciones (2 de la carrera A, 1 de la B), becas y asistencia; devuelve su id. */
    private long escenario(String etiqueta, boolean publicada) {
        long c = fx.convocatoriaConCupos(etiqueta, 3, null, null);
        long[] a1 = postulacion(c, carreraA, "adjudicada");
        long[] a2 = postulacion(c, carreraA, "calificada");
        long[] b1 = postulacion(c, carreraB, "calificada");
        fx.puntajeIa(a1[1], "45.00", "{}");
        fx.puntajeIa(a2[1], "70.00", "{}");
        long f = fx.finalista(a1[1], 1);
        assertThat(f).isPositive();
        fx.adjudicacion(a1[1], "ADJUDICADA", "80.00", admin);
        if (publicada) {
            publicar(c);
        }
        long becaUno = beca(c, a1[0], "VIGENTE", "2026-09-01");
        long becaDos = beca(c, a2[0], "VIGENTE", "2026-09-01");
        long becaTres = beca(c, b1[0], "SUSPENDIDA", "2027-01-01");
        jdbc.update("""
                INSERT INTO movimiento_beca (beca_id, beca_reemplazante_id, registrado_por, tipo, fecha, motivo)
                VALUES (?, ?, ?, CAST('REEMPLAZO' AS tipo_movimiento_beca), DATE '2026-10-01', 'prueba')""",
                becaTres, becaUno, admin);
        // Cinco dias de servicio: la beca uno asiste a todos; la dos solo a los dos primeros.
        for (String dia : List.of("2026-09-07", "2026-09-08", "2026-09-09", "2026-09-10", "2026-09-11")) {
            asistencia(becaUno, dia);
        }
        asistencia(becaDos, "2026-09-07");
        asistencia(becaDos, "2026-09-08");
        return c;
    }

    private long gestionDe(long convocatoria) {
        return jdbc.queryForObject("SELECT gestion_id FROM convocatoria WHERE id = ?", Long.class, convocatoria);
    }

    @Test
    void laVistaPorConvocatoria_cuentaElEmbudoElPadronYLosReemplazos() {
        long c = escenario("BI1", true);
        refrescar();

        FilaConvocatoria fila = repositorio.convocatorias(gestionDe(c)).get(0);

        assertThat(fila.id()).isEqualTo(c);
        assertThat(fila.cupos()).isEqualTo(3);
        assertThat(fila.recibidas()).isEqualTo(3);
        assertThat(fila.precalificadas()).isEqualTo(2);
        assertThat(fila.puntajePromedio()).isEqualTo(57.5);
        assertThat(fila.finalistas()).isEqualTo(1);
        assertThat(fila.adjudicadas()).isEqualTo(1);
        assertThat(fila.becasVigentes()).isEqualTo(2);
        assertThat(fila.becasSuspendidas()).isEqualTo(1);
        assertThat(fila.becasBaja()).isZero();
        assertThat(fila.reemplazos()).isEqualTo(1);
    }

    @Test
    void lasAdjudicadas_soloCuentanConResultadosPublicados() {
        long sinPublicar = escenario("BI2", false);
        refrescar();

        assertThat(repositorio.convocatorias(gestionDe(sinPublicar)).get(0).adjudicadas()).isZero();
        assertThat(repositorio.carreras(gestionDe(sinPublicar), null)).allMatch(f -> f.adjudicadas() == 0);

        publicar(sinPublicar);
        refrescar();

        assertThat(repositorio.convocatorias(gestionDe(sinPublicar)).get(0).adjudicadas()).isEqualTo(1);
        assertThat(repositorio.carreras(gestionDe(sinPublicar), null)).extracting(FilaCarrera::adjudicadas)
                .contains(1L);
    }

    @Test
    void losBorradores_noCuentanComoRecibidas() {
        long c = fx.convocatoria("BI3");
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        fx.postulacion(c, usuario, "borrador");
        refrescar();

        FilaConvocatoria fila = repositorio.convocatorias(gestionDe(c)).get(0);
        assertThat(fila.recibidas()).isZero();
        assertThat(repositorio.carreras(gestionDe(c), null)).isEmpty();
    }

    @Test
    void laVistaPorCarrera_agrupaPorFacultadYCarrera_ordenadaPorPostulaciones() {
        long c = escenario("BI4", true);
        refrescar();

        List<FilaCarrera> carreras = repositorio.carreras(gestionDe(c), null);

        assertThat(carreras).hasSize(2);
        assertThat(carreras.get(0).postulaciones()).isEqualTo(2);
        assertThat(carreras.get(0).adjudicadas()).isEqualTo(1);
        assertThat(carreras.get(1).postulaciones()).isEqualTo(1);
        assertThat(carreras.get(0).facultad()).isNotEqualTo(carreras.get(1).facultad());
        // Filtrar por una convocatoria que no existe da vacio.
        assertThat(repositorio.carreras(gestionDe(c), c + 100_000)).isEmpty();
    }

    @Test
    void elPuntaje_seAgrupaEnTramosDeDiez_yElMaximoCaeEnElUltimo() {
        long c = escenario("BI5", true);
        refrescar();

        List<FilaTramo> tramos = repositorio.tramosPuntaje(gestionDe(c), null);

        assertThat(tramos).extracting(FilaTramo::tramo).containsExactly(4, 6);
        assertThat(tramos).extracting(FilaTramo::cantidad).containsExactly(1L, 1L);
    }

    @Test
    void laAsistencia_esperaTodasLasBecasDelDia_yMarcaLasBecasEnRiesgo() {
        long c = escenario("BI6", true);
        refrescar();
        long gestion = gestionDe(c);

        List<Map<String, Object>> dias = jdbc.queryForList(
                "SELECT fecha, asistieron, esperados FROM mv_bi_asistencia_dia WHERE convocatoria_id = ? ORDER BY fecha",
                c);
        assertThat(dias).hasSize(5);
        // La beca suspendida empieza en 2027: no se espera ese dia. Los dos primeros dias asisten las dos.
        assertThat(dias.get(0)).containsEntry("asistieron", 2).containsEntry("esperados", 2);
        assertThat(dias.get(4)).containsEntry("asistieron", 1).containsEntry("esperados", 2);

        FilaAsistenciaBi total = repositorio.asistenciaTotal(gestion, null);
        assertThat(total.asistieron()).isEqualTo(7);
        assertThat(total.esperados()).isEqualTo(10);
        assertThat(total.dias()).isEqualTo(5);
        // 7/10 = 70 %; el 11 de septiembre fue viernes (ISO 5) y el 7 lunes (ISO 1).
        assertThat(repositorio.asistenciaPorDiaSemana(gestion, null)).hasSize(5);
        // La beca dos asistio a 2 de 5 dias (40 % < 60 %): en riesgo. La uno, con 100 %, no.
        assertThat(repositorio.convocatorias(gestion).get(0).becasEnRiesgo()).isEqualTo(1);
    }

    @Test
    void sinAsistenciaRegistrada_todoQuedaEnCero() {
        long c = fx.convocatoria("BI7");
        refrescar();

        FilaAsistenciaBi total = repositorio.asistenciaTotal(gestionDe(c), null);

        assertThat(total.asistieron()).isZero();
        assertThat(total.esperados()).isZero();
        assertThat(total.dias()).isZero();
        assertThat(repositorio.asistenciaPorDiaSemana(gestionDe(c), null)).isEmpty();
    }

    @Test
    void lasConsultas_leenDeLaVista_noDeLasTablas() {
        long c = escenario("BI8", true);
        refrescar();
        long gestion = gestionDe(c);

        // Una postulacion nueva sin refrescar: la vista no la ve.
        postulacion(c, carreraB, "calificada");

        assertThat(repositorio.convocatorias(gestion).get(0).recibidas()).isEqualTo(3);
        assertThat(repositorio.carreras(gestion, null)).extracting(FilaCarrera::postulaciones).containsExactly(2L, 1L);

        refrescar();
        assertThat(repositorio.convocatorias(gestion).get(0).recibidas()).isEqualTo(4);
    }

    @Test
    void laSerieDeGestiones_traeTodasConSusTotales() {
        long c = escenario("BI9", true);
        refrescar();

        var gestiones = repositorio.gestiones();

        assertThat(gestiones).anySatisfy(g -> {
            assertThat(g.etiqueta()).isEqualTo("G-BI9");
            assertThat(g.postulaciones()).isEqualTo(3);
            assertThat(g.adjudicadas()).isEqualTo(1);
        });
        assertThat(gestionDe(c)).isPositive();
    }

    @Test
    void todasLasVistasSePuedenRefrescarEnConcurrente_porSuIndiceUnico() {
        for (String vista : VISTAS) {
            Integer indices = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_indexes
                    WHERE tablename = ? AND indexdef LIKE 'CREATE UNIQUE INDEX%'""", Integer.class, vista);
            assertThat(indices).as(vista).isGreaterThanOrEqualTo(1);
        }
    }
}
