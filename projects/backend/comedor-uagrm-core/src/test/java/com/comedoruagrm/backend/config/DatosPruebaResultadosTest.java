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
 * Cargador de datos de prueba de la publicacion de resultados (CU22) con Postgres real: la convocatoria `[PRUEBA]`
 * con todo decidido y sin publicar, la que tiene un finalista pendiente, la ya publicada con sus avisos, que sea
 * idempotente y que solo exista en el perfil `dev`.
 */
@SpringBootTest
class DatosPruebaResultadosTest {

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
        DatosPruebaListaPriorizada lista = new DatosPruebaListaPriorizada(jdbc, new TransactionTemplate(transacciones));
        ReflectionTestUtils.setField(lista, "cantidad", 40);
        lista.run();
        new DatosPruebaAdjudicacion(jdbc, new TransactionTemplate(transacciones)).run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private DatosPruebaResultados cargador() {
        return new DatosPruebaResultados(jdbc, new TransactionTemplate(transacciones));
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private long id(String nombre) {
        return jdbc.queryForObject("SELECT id FROM convocatoria WHERE nombre = ?", Long.class, nombre);
    }

    private long finalistasEn(long convocatoria, String estado) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND po.estado = CAST(? AS estado_postulacion)""", Long.class,
                convocatoria, estado);
    }

    private boolean publicada(long convocatoria) {
        return contarPublicaciones(convocatoria) > 0;
    }

    private long contarPublicaciones(long convocatoria) {
        return jdbc.queryForObject("SELECT count(*) FROM publicacion_resultado WHERE convocatoria_id = ?",
                Long.class, convocatoria);
    }

    @Test
    void run_dejaLaConvocatoriaDePruebaConTodoDecididoYSinPublicar() {
        long prueba = id(DatosPruebaAdjudicacion.CONVOCATORIA);
        assertThat(finalistasEn(prueba, "calificada")).isPositive();

        cargador().run();

        assertThat(finalistasEn(prueba, "calificada")).isZero();
        assertThat(publicada(prueba)).isFalse();
        assertThat(jdbc.queryForObject("SELECT CAST(estado AS text) FROM convocatoria WHERE id = ?", String.class,
                prueba)).isEqualTo("CERRADA");
        // Hay adjudicados y no adjudicados, y los adjudicados no pasan de los cupos.
        long adjudicados = jdbc.queryForObject("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = 'ADJUDICADA'""", Long.class, prueba);
        long noAdjudicados = jdbc.queryForObject("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = 'NO_ADJUDICADA'""", Long.class, prueba);
        assertThat(adjudicados).isBetween(1L, (long) DatosPruebaAdjudicacion.CUPOS);
        assertThat(noAdjudicados).isPositive();
        // Los casos del CU20 siguen como estaban: devuelta, archivada y rechazada no se tocan.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND po.estado IN ('devuelta', 'archivada')""", Long.class, prueba))
                .isLessThanOrEqualTo(3);
    }

    @Test
    void run_creaLaConvocatoriaConPendientes_cerradaYConUnFinalistaEnCalificada() {
        cargador().run();

        long conPendientes = id(DatosPruebaResultados.CON_PENDIENTES);
        assertThat(jdbc.queryForObject("SELECT CAST(estado AS text) FROM convocatoria WHERE id = ?", String.class,
                conPendientes)).isEqualTo("CERRADA");
        assertThat(finalistasEn(conPendientes, "calificada")).isGreaterThanOrEqualTo(1);
        assertThat(finalistasEn(conPendientes, "adjudicada")).isEqualTo(1);
        assertThat(finalistasEn(conPendientes, "no_adjudicada")).isEqualTo(1);
        assertThat(publicada(conPendientes)).isFalse();
    }

    @Test
    void run_creaLaConvocatoriaPublicadaConSuFilaYSusAvisos() {
        cargador().run();

        long publicada = id(DatosPruebaResultados.PUBLICADA);
        assertThat(jdbc.queryForObject("SELECT CAST(estado AS text) FROM convocatoria WHERE id = ?", String.class,
                publicada)).isEqualTo("PUBLICADA");
        assertThat(contarPublicaciones(publicada)).isEqualTo(1);
        assertThat(finalistasEn(publicada, "adjudicada")).isEqualTo(2);
        assertThat(finalistasEn(publicada, "no_adjudicada")).isEqualTo(1);
        assertThat(finalistasEn(publicada, "archivada")).isEqualTo(1);
        // Un aviso por finalista con adjudicacion (no por el archivado), cada uno con su resultado.
        List<Map<String, Object>> avisos = jdbc.queryForList("""
                SELECT CAST(n.tipo AS text) AS tipo, n.mensaje FROM notificacion n
                JOIN postulacion po ON po.id = n.postulacion_id
                WHERE po.convocatoria_id = ? ORDER BY n.tipo, n.id""", publicada);
        assertThat(avisos).hasSize(3);
        assertThat(avisos).extracting(a -> a.get("tipo")).containsExactly("SUCCESS", "SUCCESS", "INFO");
        assertThat(avisos.get(0).get("mensaje").toString()).contains("obtuviste la Beca Comedor");
        assertThat(avisos.get(2).get("mensaje").toString()).contains("no fue adjudicada");
    }

    @Test
    void run_losAdjudicadosPublicadosTienenCarreraYFacultad() {
        cargador().run();

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id
                JOIN adjudicacion a ON a.postulacion_id = po.id AND a.resultado = 'ADJUDICADA'
                JOIN postulante pt ON pt.usuario_id = po.postulante_id
                JOIN carrera c ON c.id = pt.carrera_id AND c.nombre <> '' AND c.facultad <> ''
                WHERE po.convocatoria_id = ?""", Long.class, id(DatosPruebaResultados.PUBLICADA))).isEqualTo(2);
    }

    @Test
    void run_dosVeces_noDuplicaNada() {
        cargador().run();
        long convocatorias = contar("convocatoria");
        long adjudicaciones = contar("adjudicacion");
        long publicaciones = contar("publicacion_resultado");
        long notificaciones = contar("notificacion");

        cargador().run();

        assertThat(contar("convocatoria")).isEqualTo(convocatorias);
        assertThat(contar("adjudicacion")).isEqualTo(adjudicaciones);
        assertThat(contar("publicacion_resultado")).isEqualTo(publicaciones);
        assertThat(contar("notificacion")).isEqualTo(notificaciones);
    }

    @Test
    void run_sinLaConvocatoriaDePrueba_noHaceNada() {
        borrarTodo();

        cargador().run();

        assertThat(contar("publicacion_resultado")).isZero();
        assertThat(contar("convocatoria")).isZero();
    }

    @Test
    void soloExisteEnElPerfilDev_yEnLosTestsSinEsePerfilNoSeCrea() {
        Profile perfil = DatosPruebaResultados.class.getAnnotation(Profile.class);

        assertThat(perfil.value()).containsExactly("dev");
        assertThat(contexto.getBeanNamesForType(DatosPruebaResultados.class)).isEmpty();
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
