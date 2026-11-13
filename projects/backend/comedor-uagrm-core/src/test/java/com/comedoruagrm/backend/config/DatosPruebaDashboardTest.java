package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.comedoruagrm.backend.features.dashboard.service.RefrescoIndicadoresService;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Cargador de datos de prueba del dashboard (CU12) con Postgres real: el Directivo de prueba, postulaciones en
 * varios meses, borradores que no cuentan, una convocatoria publicada y otra sin publicar, la vista refrescada,
 * que corra dos veces sin duplicar y que solo exista en el perfil `dev`.
 */
@SpringBootTest
class DatosPruebaDashboardTest {

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
        new DatosPruebaAdjudicacion(jdbc, tx).run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
        // La vista guarda lo que vio la ultima vez: se refresca vacia para que otros tests no la vean sucia.
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
    }

    private DatosPruebaDashboard cargador() {
        DatosPruebaDashboard cargador = new DatosPruebaDashboard(jdbc, new TransactionTemplate(transacciones),
                passwordEncoder, new RefrescoIndicadoresService(jdbc));
        ReflectionTestUtils.setField(cargador, "contrasena", "123123123");
        return cargador;
    }

    private long contar(String sql) {
        return jdbc.queryForObject("SELECT count(*) " + sql, Long.class);
    }

    private long sumaVista(String columna, String convocatoria) {
        return jdbc.queryForObject("SELECT coalesce(sum(v." + columna + "), 0) FROM mv_indicadores_convocatoria v "
                + "JOIN convocatoria cv ON cv.id = v.convocatoria_id WHERE cv.nombre = ?", Long.class, convocatoria);
    }

    @Test
    void run_creaElDirectivoDePruebaConContrasenaQueSirve() {
        cargador().run();

        String hash = jdbc.queryForObject("""
                SELECT u.password_hash FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE u.correo = ? AND r.nombre = 'DIRECTIVO'""", String.class,
                DatosPruebaDashboard.CORREO_DIRECTIVO);
        assertThat(passwordEncoder.matches("123123123", hash)).isTrue();
    }

    @Test
    void run_laVistaQuedaConFilasEnAlMenosDosMeses() {
        cargador().run();

        assertThat(jdbc.queryForObject("SELECT count(DISTINCT mes) FROM mv_indicadores_convocatoria v JOIN "
                + "convocatoria cv ON cv.id = v.convocatoria_id WHERE cv.nombre = ?", Long.class,
                DatosPruebaDashboard.PUBLICADA)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void run_losBorradoresNoCuentan() {
        cargador().run();

        long enviadas = contar("FROM postulacion po JOIN convocatoria cv ON cv.id = po.convocatoria_id "
                + "WHERE cv.nombre = '" + DatosPruebaDashboard.PUBLICADA + "' AND po.fecha_envio IS NOT NULL");
        long borradores = contar("FROM postulacion po JOIN convocatoria cv ON cv.id = po.convocatoria_id "
                + "WHERE cv.nombre = '" + DatosPruebaDashboard.PUBLICADA + "' AND po.estado = 'borrador'");

        assertThat(borradores).isEqualTo(3);
        assertThat(sumaVista("postulaciones_recibidas", DatosPruebaDashboard.PUBLICADA)).isEqualTo(enviadas);
    }

    @Test
    void run_laConvocatoriaSinPublicarNoSumaAdjudicadas_yLaPublicadaSi() {
        cargador().run();

        assertThat(contar("FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id JOIN convocatoria cv "
                + "ON cv.id = po.convocatoria_id WHERE cv.nombre = '" + DatosPruebaDashboard.SIN_PUBLICAR
                + "' AND a.resultado = 'ADJUDICADA'")).isPositive();
        assertThat(sumaVista("adjudicadas", DatosPruebaDashboard.SIN_PUBLICAR)).isZero();
        assertThat(sumaVista("adjudicadas", DatosPruebaDashboard.PUBLICADA)).isEqualTo(4);
        assertThat(sumaVista("finalistas", DatosPruebaDashboard.SIN_PUBLICAR)).isPositive();
    }

    @Test
    void run_dosVecesNoDuplicaNada() {
        cargador().run();
        long usuarios = contar("FROM usuario");
        long postulaciones = contar("FROM postulacion");
        long convocatorias = contar("FROM convocatoria");
        long adjudicaciones = contar("FROM adjudicacion");
        long filasVista = contar("FROM mv_indicadores_convocatoria");

        cargador().run();

        assertThat(contar("FROM usuario")).isEqualTo(usuarios);
        assertThat(contar("FROM postulacion")).isEqualTo(postulaciones);
        assertThat(contar("FROM convocatoria")).isEqualTo(convocatorias);
        assertThat(contar("FROM adjudicacion")).isEqualTo(adjudicaciones);
        assertThat(contar("FROM mv_indicadores_convocatoria")).isEqualTo(filasVista);
    }

    @Test
    void run_sinLaConvocatoriaDePrueba_noCargaConvocatoriasPeroElDirectivoSiSeCrea() {
        jdbc.update("UPDATE convocatoria SET nombre = 'otra' WHERE nombre = ?", DatosPruebaAdjudicacion.CONVOCATORIA);

        cargador().run();

        assertThat(contar("FROM convocatoria WHERE nombre LIKE '" + DatosPruebaListaPriorizada.MARCA
                + " Dashboard%'")).isZero();
    }

    @Test
    void soloExisteEnElPerfilDev_yEnLosTestsSinEsePerfilNoSeCrea() {
        Profile perfil = DatosPruebaDashboard.class.getAnnotation(Profile.class);

        assertThat(perfil.value()).containsExactly("dev");
        assertThat(contexto.getBeanNamesForType(DatosPruebaDashboard.class)).isEmpty();
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
