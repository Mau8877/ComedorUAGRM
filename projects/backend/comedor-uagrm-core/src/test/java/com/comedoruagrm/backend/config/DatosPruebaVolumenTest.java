package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.features.postulaciones.service.OpcionesSocioeconomicas;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Cargador de datos de volumen (B1) con Postgres real: postulaciones con respuestas del formulario y puntaje,
 * adjudicados, becas y asistencias; que respete los catalogos del CU02, los topes de la IA y los indices unicos,
 * y que sea idempotente.
 */
@SpringBootTest
class DatosPruebaVolumenTest {

    private static final int POSTULANTES = 100;
    /** `min(1000, max(1, 100 / 5))`. */
    private static final int CUPOS = 20;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transacciones;

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
        borrarTodo();
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '0000001', 'admin@dubss.com', ?, 'Administrador', 'DUBSS' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS'""", passwordEncoder.encode("secreta1"));
        DatosPruebaListaPriorizada lista = new DatosPruebaListaPriorizada(jdbc, new TransactionTemplate(transacciones));
        ReflectionTestUtils.setField(lista, "cantidad", POSTULANTES);
        lista.run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private DatosPruebaVolumen cargador() {
        DatosPruebaVolumen cargador = new DatosPruebaVolumen(jdbc, new TransactionTemplate(transacciones));
        ReflectionTestUtils.setField(cargador, "dias", 35);
        return cargador;
    }

    private long conv() {
        return jdbc.queryForObject("SELECT id FROM convocatoria WHERE nombre = ?", Long.class,
                DatosPruebaVolumen.CONVOCATORIA);
    }

    private long contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    void run_creaSuPropiaConvocatoriaCerradaConLosCuposDelVolumen() {
        cargador().run();

        Map<String, Object> c = jdbc.queryForMap(
                "SELECT CAST(estado AS text) AS estado, cupos FROM convocatoria WHERE nombre = ?",
                DatosPruebaVolumen.CONVOCATORIA);
        assertThat(c.get("estado")).isEqualTo("CERRADA");
        assertThat(c.get("cupos")).isEqualTo(CUPOS);
    }

    @Test
    void run_unaPostulacionPorPostulanteConLaDistribucionDeEstadosDeLasDecisiones() {
        cargador().run();

        assertThat(contar("SELECT count(*) FROM postulacion WHERE convocatoria_id = ?", conv())).isEqualTo(POSTULANTES);
        assertThat(contar("SELECT count(*) FROM postulacion WHERE convocatoria_id = ? AND estado = 'adjudicada'",
                conv())).isEqualTo(CUPOS);
        assertThat(contar("SELECT count(*) FROM postulacion WHERE convocatoria_id = ? AND estado = 'no_adjudicada'",
                conv())).isEqualTo(CUPOS / 2);
        assertThat(contar("SELECT count(*) FROM postulacion WHERE convocatoria_id = ? AND estado = 'calificada'",
                conv())).isEqualTo(POSTULANTES - CUPOS - CUPOS / 2);
    }

    @Test
    void run_cadaPostulacionTieneSusRespuestasCompletasYDentroDeLosCatalogosDelFormulario() {
        cargador().run();

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT r.*, CAST(r.datos_extra AS text) AS extra,
                       r.datos_extra ->> 'dependenciaEconomica' AS dependencia,
                       r.datos_extra ->> 'ocupacionSosten' AS ocupacion,
                       r.datos_extra ->> 'tenenciaVivienda' AS tenencia
                FROM respuesta_socioeconomica r JOIN postulacion po ON po.id = r.postulacion_id
                WHERE po.convocatoria_id = ?""", conv());
        assertThat(filas).hasSize(POSTULANTES);
        for (Map<String, Object> f : filas) {
            assertThat(OpcionesSocioeconomicas.PROCEDENCIAS).contains((String) f.get("procedencia"));
            assertThat(OpcionesSocioeconomicas.PATRIMONIOS).contains((String) f.get("patrimonio"));
            assertThat(OpcionesSocioeconomicas.ESTADOS_VULNERABILIDAD).contains((String) f.get("estado_vulnerabilidad"));
            assertThat(OpcionesSocioeconomicas.DEPENDENCIAS).contains((String) f.get("dependencia"));
            assertThat(OpcionesSocioeconomicas.OCUPACIONES_SOSTEN).contains((String) f.get("ocupacion"));
            assertThat(OpcionesSocioeconomicas.TENENCIAS_VIVIENDA).contains((String) f.get("tenencia"));
            assertThat(((Number) f.get("dependientes")).intValue()).isBetween(0, OpcionesSocioeconomicas.DEPENDIENTES_MAXIMO);
            assertThat((BigDecimal) f.get("promedio_academico")).isBetween(BigDecimal.ZERO, new BigDecimal("100"));
            assertThat((BigDecimal) f.get("ingresos_familiares")).isPositive();
        }
    }

    @Test
    void run_losDatosNoSonTodosIgualesYElFormularioOficialTraeHijosYDiscapacidad() {
        cargador().run();

        // Cada fila saca su propio azar: si el LATERAL se evaluara una sola vez, todas serian iguales.
        assertThat(contar("""
                SELECT count(DISTINCT r.ingresos_familiares) FROM respuesta_socioeconomica r
                JOIN postulacion po ON po.id = r.postulacion_id WHERE po.convocatoria_id = ?""", conv()))
                .isGreaterThan(50);
        assertThat(contar("""
                SELECT count(DISTINCT r.procedencia) FROM respuesta_socioeconomica r
                JOIN postulacion po ON po.id = r.postulacion_id WHERE po.convocatoria_id = ?""", conv()))
                .isGreaterThan(1);
        // Con `cantidadHijos` solo cuando tiene hijos, como lo guarda el CU02.
        assertThat(contar("""
                SELECT count(*) FROM respuesta_socioeconomica r
                JOIN postulacion po ON po.id = r.postulacion_id
                WHERE po.convocatoria_id = ? AND (r.datos_extra ->> 'tieneHijos')::boolean IS FALSE
                  AND r.datos_extra ->> 'cantidadHijos' IS NOT NULL""", conv())).isZero();
    }

    @Test
    void run_losPuntajesSonVigentesYNoPasanDe70() {
        cargador().run();

        assertThat(contar("""
                SELECT count(*) FROM puntaje_postulacion pu JOIN postulacion po ON po.id = pu.postulacion_id
                WHERE po.convocatoria_id = ? AND pu.vigente AND pu.puntaje_total BETWEEN 0 AND 70""", conv()))
                .isEqualTo(POSTULANTES);
        assertThat(contar("""
                SELECT count(DISTINCT pu.puntaje_total) FROM puntaje_postulacion pu
                JOIN postulacion po ON po.id = pu.postulacion_id WHERE po.convocatoria_id = ?""", conv()))
                .isGreaterThan(20);
    }

    @Test
    void run_losAdjudicadosSonLosDeMasPuntajeYSuPuntajeFinalSumaElDelAuditor() {
        cargador().run();

        assertThat(contar("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = 'ADJUDICADA'""", conv())).isEqualTo(CUPOS);
        // El peor adjudicado tiene al menos el puntaje del mejor no adjudicado (la lista esta ordenada).
        BigDecimal peorAdjudicado = jdbc.queryForObject("""
                SELECT min(pu.puntaje_total) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                WHERE po.convocatoria_id = ? AND a.resultado = 'ADJUDICADA'""", BigDecimal.class, conv());
        BigDecimal mejorSinCupo = jdbc.queryForObject("""
                SELECT max(pu.puntaje_total) FROM postulacion po
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                WHERE po.convocatoria_id = ? AND po.estado <> 'adjudicada'""", BigDecimal.class, conv());
        assertThat(peorAdjudicado).isGreaterThanOrEqualTo(mejorSinCupo);
        assertThat(contar("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                WHERE po.convocatoria_id = ? AND (a.puntaje_final > 100 OR a.puntaje_final < pu.puntaje_total)""",
                conv())).isZero();
    }

    @Test
    void run_unaBecaPorAdjudicadaConSuMovimientoSiNoEstaVigente() {
        cargador().run();

        assertThat(contar("SELECT count(*) FROM beca WHERE convocatoria_id = ?", conv())).isEqualTo(CUPOS);
        assertThat(contar("SELECT count(*) FROM beca WHERE convocatoria_id = ? AND adjudicacion_id IS NOT NULL",
                conv())).isEqualTo(CUPOS);
        // Una de cada 12 suspendida (la 12) y una de cada 17 de baja (la 17).
        assertThat(contar("SELECT count(*) FROM beca WHERE convocatoria_id = ? AND estado = 'SUSPENDIDA'", conv()))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM beca WHERE convocatoria_id = ? AND estado = 'BAJA'", conv()))
                .isEqualTo(1);
        assertThat(contar("""
                SELECT count(*) FROM beca b WHERE b.convocatoria_id = ? AND b.estado <> 'VIGENTE'
                  AND EXISTS (SELECT 1 FROM movimiento_beca m WHERE m.beca_id = b.id)""", conv())).isEqualTo(2);
        assertThat(contar("""
                SELECT count(*) FROM beca WHERE convocatoria_id = ? AND estado = 'BAJA' AND fecha_fin IS NOT NULL""",
                conv())).isEqualTo(1);
    }

    @Test
    void run_lasAsistenciasSoloSonDeBecasVigentesEnDiasHabilesYSinRepetir() {
        cargador().run();

        // 18 becas vigentes x 25 dias habiles de 35 corridos x 80 % ≈ 360.
        long total = contar("SELECT count(*) FROM asistencia a JOIN beca b ON b.id = a.beca_id "
                + "WHERE b.convocatoria_id = ?", conv());
        assertThat(total).isBetween(250L, 450L);
        assertThat(contar("""
                SELECT count(*) FROM asistencia a JOIN beca b ON b.id = a.beca_id
                WHERE b.convocatoria_id = ? AND b.estado <> 'VIGENTE'""", conv())).isZero();
        assertThat(contar("""
                SELECT count(*) FROM asistencia a JOIN beca b ON b.id = a.beca_id
                WHERE b.convocatoria_id = ? AND EXTRACT(ISODOW FROM a.fecha) >= 6""", conv())).isZero();
        assertThat(contar("""
                SELECT count(*) FROM (SELECT a.beca_id, a.fecha FROM asistencia a JOIN beca b ON b.id = a.beca_id
                WHERE b.convocatoria_id = ? GROUP BY 1, 2 HAVING count(*) > 1) x""", conv())).isZero();
        assertThat(contar("""
                SELECT count(*) FROM asistencia a JOIN beca b ON b.id = a.beca_id
                WHERE b.convocatoria_id = ? AND a.fecha < b.fecha_inicio""", conv())).isZero();
    }

    @Test
    void run_esIdempotente() {
        cargador().run();
        long postulaciones = contar("SELECT count(*) FROM postulacion");
        long asistencias = contar("SELECT count(*) FROM asistencia");

        cargador().run();

        assertThat(contar("SELECT count(*) FROM convocatoria WHERE nombre = ?", DatosPruebaVolumen.CONVOCATORIA))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM postulacion")).isEqualTo(postulaciones);
        assertThat(contar("SELECT count(*) FROM asistencia")).isEqualTo(asistencias);
    }

    @Test
    void run_sinPostulantesDePruebaNoCargaNada() {
        borrarTodo();
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '0000001', 'admin@dubss.com', ?, 'Administrador', 'DUBSS' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS'""", passwordEncoder.encode("secreta1"));

        cargador().run();

        assertThat(contar("SELECT count(*) FROM convocatoria")).isZero();
    }

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
        jdbc.update("DELETE FROM asistencia");
        jdbc.update("DELETE FROM movimiento_beca");
        jdbc.update("DELETE FROM beca");
        jdbc.update("DELETE FROM adjudicacion");
        jdbc.update("DELETE FROM postulacion_historial");
        jdbc.update("DELETE FROM auditoria_expediente");
        jdbc.update("DELETE FROM documento_adjunto");
        jdbc.update("DELETE FROM respuesta_socioeconomica");
        jdbc.update("DELETE FROM convocatoria_documento");
        jdbc.update("DELETE FROM tipo_documento");
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM notificacion");
        jdbc.update("DELETE FROM dispositivo_fcm");
        jdbc.update("DELETE FROM finalista");
        jdbc.update("DELETE FROM comunicado");
        jdbc.update("DELETE FROM puntaje_postulacion");
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM parametro_modelo");
        jdbc.update("DELETE FROM modelo_preseleccion");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
