package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Escenarios de depuracion con Postgres real: que cada convocatoria tenga los datos que dice su nombre (es lo que
 * las hace utiles para depurar), que las cuentas de prueba existan y que la carga sea idempotente.
 */
@SpringBootTest
class DatosPruebaEscenariosTest {

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
        ReflectionTestUtils.setField(lista, "cantidad", 20);
        lista.run();
        cargador().run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private DatosPruebaEscenarios cargador() {
        DatosPruebaEscenarios cargador = new DatosPruebaEscenarios(jdbc, new TransactionTemplate(transacciones),
                passwordEncoder);
        ReflectionTestUtils.setField(cargador, "contrasena", "123123123");
        return cargador;
    }

    private long contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private long conv(String nombre) {
        return jdbc.queryForObject("SELECT id FROM convocatoria WHERE nombre = ?", Long.class, nombre);
    }

    private long postulaciones(String nombre, String estado) {
        return contar("SELECT count(*) FROM postulacion WHERE convocatoria_id = ? AND estado = CAST(? AS "
                + "estado_postulacion)", conv(nombre), estado);
    }

    private long becas(String nombre, String estado) {
        return contar("SELECT count(*) FROM beca WHERE convocatoria_id = ? AND estado = CAST(? AS estado_beca)",
                conv(nombre), estado);
    }

    @Test
    void run_creaLasOchoConvocatoriasConElEstadoQueDiceSuNombre() {
        assertThat(contar("SELECT count(*) FROM convocatoria WHERE nombre LIKE '[PRUEBA] E0%'")).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT CAST(estado AS text) FROM convocatoria WHERE nombre = ?",
                String.class, DatosPruebaEscenarios.E01)).isEqualTo("ABIERTA");
        assertThat(jdbc.queryForObject("SELECT CAST(estado AS text) FROM convocatoria WHERE nombre = ?",
                String.class, DatosPruebaEscenarios.E02)).isEqualTo("CERRADA");
        assertThat(jdbc.queryForObject("SELECT CAST(estado AS text) FROM convocatoria WHERE nombre = ?",
                String.class, DatosPruebaEscenarios.E08)).isEqualTo("PUBLICADA");
    }

    @Test
    void e01_tieneUnaPostulacionEnCadaEstadoPrevio() {
        assertThat(postulaciones(DatosPruebaEscenarios.E01, "borrador")).isEqualTo(1);
        assertThat(postulaciones(DatosPruebaEscenarios.E01, "pendiente")).isEqualTo(1);
        assertThat(postulaciones(DatosPruebaEscenarios.E01, "devuelta")).isEqualTo(1);
        assertThat(postulaciones(DatosPruebaEscenarios.E01, "calificada")).isEqualTo(2);
    }

    @Test
    void e02yE03_tienenSeisCalificadasYSoloE03TieneCuatroFinalistasSinAuditar() {
        assertThat(postulaciones(DatosPruebaEscenarios.E02, "calificada")).isEqualTo(6);
        assertThat(postulaciones(DatosPruebaEscenarios.E03, "calificada")).isEqualTo(6);
        assertThat(contar("""
                SELECT count(*) FROM finalista f JOIN postulacion p ON p.id = f.postulacion_id
                WHERE p.convocatoria_id = ?""", conv(DatosPruebaEscenarios.E02))).isZero();
        assertThat(contar("""
                SELECT count(*) FROM finalista f JOIN postulacion p ON p.id = f.postulacion_id
                WHERE p.convocatoria_id = ? AND f.notificado_en IS NULL
                  AND NOT EXISTS (SELECT 1 FROM auditoria_expediente a WHERE a.finalista_id = f.id)""",
                conv(DatosPruebaEscenarios.E03))).isEqualTo(4);
    }

    @Test
    void e04_tieneUnaAuditoriaDeCadaResultadoYUnFinalistaSinEntrega() {
        long c = conv(DatosPruebaEscenarios.E04);
        for (String resultado : new String[] {"VALIDADO", "OBSERVADO", "NO_VALIDO"}) {
            assertThat(contar("""
                    SELECT count(*) FROM auditoria_expediente a JOIN finalista f ON f.id = a.finalista_id
                    JOIN postulacion p ON p.id = f.postulacion_id
                    WHERE p.convocatoria_id = ? AND CAST(a.resultado AS text) = ?""", c, resultado)).isEqualTo(1);
        }
        assertThat(contar("""
                SELECT count(*) FROM finalista f JOIN postulacion p ON p.id = f.postulacion_id
                WHERE p.convocatoria_id = ? AND f.documentacion_recibida_en IS NULL""", c)).isEqualTo(1);
    }

    @Test
    void e05_tieneLasDecisionesTomadasPeroSinPublicar() {
        assertThat(postulaciones(DatosPruebaEscenarios.E05, "adjudicada")).isEqualTo(2);
        assertThat(postulaciones(DatosPruebaEscenarios.E05, "no_adjudicada")).isEqualTo(1);
        assertThat(postulaciones(DatosPruebaEscenarios.E05, "archivada")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM publicacion_resultado WHERE convocatoria_id = ?",
                conv(DatosPruebaEscenarios.E05))).isZero();
    }

    @Test
    void e06_estaPublicadaConAdjudicadosSinBecaParaDarDeAlta() {
        long c = conv(DatosPruebaEscenarios.E06);
        assertThat(contar("SELECT count(*) FROM publicacion_resultado WHERE convocatoria_id = ?", c)).isEqualTo(1);
        assertThat(postulaciones(DatosPruebaEscenarios.E06, "adjudicada")).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM beca WHERE convocatoria_id = ?", c)).isZero();
    }

    @Test
    void e07_tieneElPadronMixtoYUnCandidatoDeReemplazo() {
        assertThat(becas(DatosPruebaEscenarios.E07, "VIGENTE")).isEqualTo(3);
        assertThat(becas(DatosPruebaEscenarios.E07, "SUSPENDIDA")).isEqualTo(1);
        assertThat(becas(DatosPruebaEscenarios.E07, "BAJA")).isEqualTo(1);
        assertThat(contar("""
                SELECT count(*) FROM movimiento_beca m JOIN beca b ON b.id = m.beca_id
                WHERE b.convocatoria_id = ?""", conv(DatosPruebaEscenarios.E07))).isEqualTo(2);
        // El candidato: finalista calificada, validado y sin beca (lo que pide el reemplazo del CU23).
        assertThat(contar("""
                SELECT count(*) FROM finalista f JOIN postulacion p ON p.id = f.postulacion_id
                JOIN auditoria_expediente a ON a.finalista_id = f.id AND CAST(a.resultado AS text) = 'VALIDADO'
                WHERE p.convocatoria_id = ? AND CAST(p.estado AS text) = 'calificada'
                  AND NOT EXISTS (SELECT 1 FROM beca b WHERE b.postulante_id = p.postulante_id
                                  AND b.convocatoria_id = p.convocatoria_id)""",
                conv(DatosPruebaEscenarios.E07))).isEqualTo(1);
    }

    @Test
    void e08_tieneCuatroBecasVigentesConAsistenciasYSinPlanillas() {
        long c = conv(DatosPruebaEscenarios.E08);
        assertThat(becas(DatosPruebaEscenarios.E08, "VIGENTE")).isEqualTo(4);
        assertThat(contar("""
                SELECT count(DISTINCT s.beca_id) FROM asistencia s JOIN beca b ON b.id = s.beca_id
                WHERE b.convocatoria_id = ?""", c)).isEqualTo(4);
        assertThat(contar("SELECT count(*) FROM planilla_asistencia WHERE convocatoria_id = ?", c)).isZero();
    }

    @Test
    void run_dejaLasCuentasConClaveConSuRol() {
        assertThat(contar("""
                SELECT count(*) FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE u.correo = ? AND r.nombre = 'PERSONAL_COMEDOR'""", DatosPruebaEscenarios.CORREO_PERSONAL))
                .isEqualTo(1);
        for (String correo : new String[] {DatosPruebaEscenarios.CORREO_VIGENTE,
            DatosPruebaEscenarios.CORREO_SUSPENDIDO, DatosPruebaEscenarios.CORREO_BAJA}) {
            assertThat(contar("""
                    SELECT count(*) FROM usuario u JOIN rol r ON r.id = u.rol_id
                    JOIN postulante p ON p.usuario_id = u.id WHERE u.correo = ? AND r.nombre = 'POSTULANTE'""",
                    correo)).isEqualTo(1);
        }
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE correo = ?", String.class,
                DatosPruebaEscenarios.CORREO_PERSONAL);
        assertThat(passwordEncoder.matches("123123123", hash)).isTrue();
    }

    @Test
    void run_esIdempotente() {
        long antes = contar("SELECT count(*) FROM convocatoria");

        cargador().run();

        assertThat(contar("SELECT count(*) FROM convocatoria")).isEqualTo(antes);
        assertThat(contar("SELECT count(*) FROM usuario WHERE correo = ?", DatosPruebaEscenarios.CORREO_PERSONAL))
                .isEqualTo(1);
    }

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
        jdbc.update("DELETE FROM publicacion_resultado");
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
