package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Cargador de datos de prueba de «Mis postulaciones» (CU04) con Postgres real: una postulacion del postulante de
 * prueba en cada estado, con su historial, que corra dos veces sin duplicar y que solo exista en el perfil `dev`.
 */
@SpringBootTest
class DatosPruebaMisPostulacionesTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Autowired
    private ApplicationContext contexto;

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
        TransactionTemplate tx = new TransactionTemplate(transacciones);
        DatosPruebaListaPriorizada lista = new DatosPruebaListaPriorizada(jdbc, tx);
        ReflectionTestUtils.setField(lista, "cantidad", 40);
        lista.run();
        DatosPruebaFinalistas finalistas = new DatosPruebaFinalistas(jdbc, tx, passwordEncoder);
        ReflectionTestUtils.setField(finalistas, "contrasena", "123123123");
        finalistas.run();
        new DatosPruebaAdjudicacion(jdbc, tx).run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private DatosPruebaMisPostulaciones cargador() {
        return new DatosPruebaMisPostulaciones(jdbc, new TransactionTemplate(transacciones));
    }

    private long contar(String sql) {
        return jdbc.queryForObject("SELECT count(*) " + sql, Long.class);
    }

    private long usuario() {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE correo = ?", Long.class,
                DatosPruebaFinalistas.CORREO);
    }

    /** Estado real de la postulacion del postulante de prueba en esa convocatoria. */
    private String estadoEn(String convocatoria) {
        return jdbc.queryForObject("""
                SELECT CAST(po.estado AS text) FROM postulacion po JOIN convocatoria cv ON cv.id = po.convocatoria_id
                WHERE cv.nombre = ? AND po.postulante_id = ?""", String.class, convocatoria, usuario());
    }

    @Test
    void run_dejaUnaPostulacionDelPostulanteDePruebaEnCadaEstado() {
        cargador().run();

        assertThat(estadoEn(DatosPruebaMisPostulaciones.PENDIENTE)).isEqualTo("pendiente");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.DEVUELTA)).isEqualTo("devuelta");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.ARCHIVADA)).isEqualTo("archivada");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.DECIDIDA_SIN_PUBLICAR)).isEqualTo("adjudicada");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.FINALISTA)).isEqualTo("calificada");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.PUBLICADA)).isEqualTo("adjudicada");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.RECHAZADA)).isEqualTo("no_adjudicada");
        assertThat(estadoEn(DatosPruebaMisPostulaciones.BORRADOR)).isEqualTo("borrador");
    }

    @Test
    void run_laPublicadaTieneSuPublicacion_ylaDecididaSinPublicarNo() {
        cargador().run();

        assertThat(contar("FROM publicacion_resultado pr JOIN convocatoria cv ON cv.id = pr.convocatoria_id "
                + "WHERE cv.nombre = '" + DatosPruebaMisPostulaciones.PUBLICADA + "'")).isEqualTo(1);
        assertThat(contar("FROM publicacion_resultado pr JOIN convocatoria cv ON cv.id = pr.convocatoria_id "
                + "WHERE cv.nombre = '" + DatosPruebaMisPostulaciones.DECIDIDA_SIN_PUBLICAR + "'")).isZero();
    }

    @Test
    void run_elRechazoDelCu20NoTieneFilaDeAdjudicacion_yLaDecisionFinalSi() {
        cargador().run();

        String rechazada = DatosPruebaMisPostulaciones.RECHAZADA;
        String decidida = DatosPruebaMisPostulaciones.DECIDIDA_SIN_PUBLICAR;
        assertThat(contar("FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id "
                + "JOIN convocatoria cv ON cv.id = po.convocatoria_id WHERE cv.nombre = '" + rechazada + "'"))
                .isZero();
        assertThat(contar("FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id "
                + "JOIN convocatoria cv ON cv.id = po.convocatoria_id WHERE cv.nombre = '" + decidida + "'"))
                .isEqualTo(1);
    }

    @Test
    void run_cadaPostulacionEnviadaTieneHistorialCoherenteConFechasDistintas() {
        cargador().run();

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT cv.nombre, count(h.id) AS pasos, count(DISTINCT h.creado_en) AS fechas,
                       max(CAST(h.estado_nuevo AS text)) FILTER (WHERE h.creado_en = (
                           SELECT max(h2.creado_en) FROM postulacion_historial h2 WHERE h2.postulacion_id = po.id))
                           AS ultimo
                FROM postulacion po JOIN convocatoria cv ON cv.id = po.convocatoria_id
                LEFT JOIN postulacion_historial h ON h.postulacion_id = po.id
                WHERE po.postulante_id = ? AND cv.nombre LIKE ? AND po.estado <> 'borrador'
                GROUP BY cv.nombre, po.id, po.estado""", usuario(), DatosPruebaMisPostulaciones.PREFIJO + "%");

        assertThat(filas).hasSize(7);
        for (Map<String, Object> fila : filas) {
            assertThat(((Number) fila.get("pasos")).longValue()).as(fila.get("nombre").toString()).isPositive();
            assertThat(fila.get("pasos")).isEqualTo(fila.get("fechas"));
        }
        // El ultimo cambio coincide con el estado real de la postulacion.
        assertThat(contar("FROM postulacion po JOIN convocatoria cv ON cv.id = po.convocatoria_id WHERE "
                + "po.postulante_id = " + usuario() + " AND cv.nombre = '" + DatosPruebaMisPostulaciones.DEVUELTA
                + "' AND EXISTS (SELECT 1 FROM postulacion_historial h WHERE h.postulacion_id = po.id AND "
                + "h.estado_nuevo = CAST('devuelta' AS estado_postulacion) AND h.motivo IS NOT NULL)")).isEqualTo(1);
    }

    @Test
    void run_elFinalistaTienePlazoYUnAvisoConLosDocumentosLigadoALaPostulacion() {
        cargador().run();

        Map<String, Object> aviso = jdbc.queryForMap("""
                SELECT n.mensaje, f.plazo_entrega, f.documentacion_recibida_en
                FROM notificacion n JOIN postulacion po ON po.id = n.postulacion_id
                JOIN convocatoria cv ON cv.id = po.convocatoria_id JOIN finalista f ON f.postulacion_id = po.id
                WHERE cv.nombre = ? AND n.titulo = 'Entrega de documentación física - Beca Comedor'""",
                DatosPruebaMisPostulaciones.FINALISTA);

        assertThat((String) aviso.get("mensaje")).contains("Documentos a presentar: carnet de identidad");
        assertThat(aviso.get("plazo_entrega")).isNotNull();
        assertThat(aviso.get("documentacion_recibida_en")).isNull();
    }

    @Test
    void run_dejaPostulacionesDeDosGestionesParaProbarElFiltro() {
        cargador().run();

        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT g.id) FROM postulacion po JOIN convocatoria cv ON cv.id = po.convocatoria_id
                JOIN gestion_academica g ON g.id = cv.gestion_id
                WHERE po.postulante_id = ? AND cv.nombre LIKE ?""", Long.class, usuario(),
                DatosPruebaMisPostulaciones.PREFIJO + "%")).isEqualTo(2);
    }

    @Test
    void run_dosVecesNoDuplicaNada() {
        cargador().run();
        long postulaciones = contar("FROM postulacion");
        long historial = contar("FROM postulacion_historial");
        long convocatorias = contar("FROM convocatoria");
        long avisos = contar("FROM notificacion");

        cargador().run();

        assertThat(contar("FROM postulacion")).isEqualTo(postulaciones);
        assertThat(contar("FROM postulacion_historial")).isEqualTo(historial);
        assertThat(contar("FROM convocatoria")).isEqualTo(convocatorias);
        assertThat(contar("FROM notificacion")).isEqualTo(avisos);
    }

    @Test
    void run_sinElPostulanteDePrueba_noHaceNada() {
        jdbc.update("UPDATE usuario SET correo = 'otro@prueba.local' WHERE correo = ?", DatosPruebaFinalistas.CORREO);

        cargador().run();

        assertThat(contar("FROM convocatoria WHERE nombre LIKE '" + DatosPruebaMisPostulaciones.PREFIJO + "%'"))
                .isZero();
    }

    @Test
    void soloExisteEnElPerfilDev_yEnLosTestsSinEsePerfilNoSeCrea() {
        Profile perfil = DatosPruebaMisPostulaciones.class.getAnnotation(Profile.class);

        assertThat(perfil.value()).containsExactly("dev");
        assertThat(contexto.getBeanNamesForType(DatosPruebaMisPostulaciones.class)).isEmpty();
    }

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
        jdbc.update("DELETE FROM publicacion_resultado");
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
