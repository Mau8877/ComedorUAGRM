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

import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Cargador de datos de prueba de la adjudicacion (CU05) con Postgres real: la convocatoria `[PRUEBA]` con cupos,
 * minimo y cantidad de finalistas, los finalistas en cada situacion, las decisiones ya tomadas, las dos
 * convocatorias de casos limite, que sea idempotente y que no toque a los finalistas de otros CU.
 */
@SpringBootTest
class DatosPruebaAdjudicacionTest {

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
        ReflectionTestUtils.setField(lista, "cantidad", 40);
        lista.run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private DatosPruebaAdjudicacion cargador() {
        return new DatosPruebaAdjudicacion(jdbc, new TransactionTemplate(transacciones));
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private Map<String, Object> convocatoria(String nombre) {
        return jdbc.queryForMap("SELECT id, CAST(estado AS text) AS estado, cupos, puntaje_minimo, "
                + "cantidad_finalistas FROM convocatoria WHERE nombre = ?", nombre);
    }

    @Test
    void run_dejaLaConvocatoriaDePruebaCerradaConCuposMinimoYCantidadDeFinalistas() {
        cargador().run();

        Map<String, Object> c = convocatoria(DatosPruebaAdjudicacion.CONVOCATORIA);
        assertThat(c.get("estado")).isEqualTo("CERRADA");
        assertThat(c.get("cupos")).isEqualTo(DatosPruebaAdjudicacion.CUPOS);
        assertThat((BigDecimal) c.get("puntaje_minimo")).isEqualByComparingTo(DatosPruebaAdjudicacion.PUNTAJE_MINIMO);
        assertThat(c.get("cantidad_finalistas")).isEqualTo(DatosPruebaAdjudicacion.CANTIDAD_FINALISTAS);
    }

    @Test
    void run_dejaFinalistasEnCadaSituacion_yCalificadasSinSeleccionar() {
        cargador().run();
        long conv = ((Number) convocatoria(DatosPruebaAdjudicacion.CONVOCATORIA).get("id")).longValue();

        // 10 de la lista priorizada + 4 nuevos (validado con puntaje bajo, validado suficiente, adjudicado y no adjudicado).
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ?""", Long.class, conv)).isEqualTo(14);
        // Validado con total por debajo del minimo: existe un finalista calificado asi.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.estado = 'calificada'
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                JOIN auditoria_expediente a ON a.finalista_id = f.id AND a.resultado = 'VALIDADO'
                WHERE po.convocatoria_id = ? AND pu.puntaje_total + a.puntaje_academico < 70""", Long.class, conv))
                .isEqualTo(1);
        // Validado con total suficiente y todavia calificado: se puede adjudicar.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.estado = 'calificada'
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                JOIN auditoria_expediente a ON a.finalista_id = f.id AND a.resultado = 'VALIDADO'
                WHERE po.convocatoria_id = ? AND pu.puntaje_total + a.puntaje_academico >= 70""", Long.class, conv))
                .isGreaterThanOrEqualTo(1);
        // Quedan calificadas sin seleccionar para probar la seleccion.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM postulacion po
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                WHERE po.convocatoria_id = ? AND po.estado = 'calificada'
                  AND NOT EXISTS (SELECT 1 FROM finalista f WHERE f.postulacion_id = po.id)""", Long.class, conv))
                .isGreaterThan(5);
    }

    @Test
    void run_dejaUnaAdjudicadaYUnaNoAdjudicadaPorDecisionFinalConSuHistorial() {
        cargador().run();
        long conv = ((Number) convocatoria(DatosPruebaAdjudicacion.CONVOCATORIA).get("id")).longValue();

        List<String> decisiones = jdbc.queryForList("""
                SELECT CAST(a.resultado AS text) || ':' || po.estado::text || ':'
                       || (a.puntaje_final IS NULL) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? ORDER BY a.resultado""", String.class, conv);
        assertThat(decisiones).containsExactly("ADJUDICADA:adjudicada:false", "NO_ADJUDICADA:no_adjudicada:true");
        assertThat(jdbc.queryForObject("""
                SELECT a.puntaje_final >= 70 FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = CAST('ADJUDICADA' AS resultado_adjudicacion)""",
                Boolean.class, conv)).isTrue();
        assertThat(jdbc.queryForList("""
                SELECT h.estado_anterior::text || '>' || h.estado_nuevo::text || ':' || (h.motivo IS NULL)
                FROM postulacion_historial h JOIN adjudicacion a ON a.postulacion_id = h.postulacion_id
                JOIN postulacion po ON po.id = a.postulacion_id WHERE po.convocatoria_id = ?
                ORDER BY h.estado_nuevo""", String.class, conv))
                .containsExactly("calificada>adjudicada:true", "calificada>no_adjudicada:false");
    }

    @Test
    void run_dejaLasDosConvocatoriasDeCasosLimite() {
        cargador().run();

        Map<String, Object> casi = convocatoria(DatosPruebaAdjudicacion.CASI_AGOTADA);
        assertThat(casi.get("estado")).isEqualTo("CERRADA");
        assertThat(casi.get("cupos")).isEqualTo(2);
        long idCasi = ((Number) casi.get("id")).longValue();
        // Un cupo ya usado (un adjudicado) y tres finalistas validados compitiendo por el que queda.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = CAST('ADJUDICADA' AS resultado_adjudicacion)""",
                Long.class, idCasi)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND po.estado = 'calificada'""", Long.class, idCasi)).isEqualTo(3);

        Map<String, Object> sinCupos = convocatoria(DatosPruebaAdjudicacion.SIN_CUPOS);
        assertThat(sinCupos.get("cupos")).isNull();
        assertThat(sinCupos.get("puntaje_minimo")).isNull();
        assertThat(sinCupos.get("cantidad_finalistas")).isNull();
        long idSin = ((Number) sinCupos.get("id")).longValue();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM postulacion WHERE convocatoria_id = ? AND estado = 'calificada'""",
                Long.class, idSin)).isEqualTo(3);
    }

    @Test
    void run_esIdempotente() {
        cargador().run();
        long adjudicaciones = contar("adjudicacion");
        long finalistas = contar("finalista");
        long convocatorias = contar("convocatoria");

        cargador().run();

        assertThat(contar("adjudicacion")).isEqualTo(adjudicaciones);
        assertThat(contar("finalista")).isEqualTo(finalistas);
        assertThat(contar("convocatoria")).isEqualTo(convocatorias);
    }

    @Test
    void run_noTocaALosDiezFinalistasDeLosOtrosCasosDeUso() {
        List<String> antes = jdbc.queryForList("""
                SELECT f.posicion || ':' || po.estado::text || ':' || f.id FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id ORDER BY f.posicion""", String.class);

        cargador().run();

        List<String> despues = jdbc.queryForList("""
                SELECT f.posicion || ':' || po.estado::text || ':' || f.id FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id
                JOIN convocatoria cv ON cv.id = po.convocatoria_id
                WHERE cv.nombre = ? AND f.posicion <= 10 ORDER BY f.posicion""", String.class,
                DatosPruebaAdjudicacion.CONVOCATORIA);
        assertThat(despues).containsExactlyElementsOf(antes);
    }

    @Test
    void run_sinLaConvocatoriaDePrueba_noHaceNada() {
        borrarTodo();

        cargador().run();

        assertThat(contar("adjudicacion")).isZero();
        assertThat(contar("convocatoria")).isZero();
    }

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
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
        jdbc.update("DELETE FROM modelo_preseleccion");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
