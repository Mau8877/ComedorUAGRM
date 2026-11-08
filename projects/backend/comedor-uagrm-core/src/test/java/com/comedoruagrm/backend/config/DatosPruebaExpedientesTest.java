package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Cargador de expedientes de prueba (CU07) con Postgres real: las situaciones esperadas, que sea
 * idempotente, que MinIO caido no impida guardar las filas y que no cambie ninguna postulacion.
 */
@SpringBootTest
class DatosPruebaExpedientesTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transacciones;

    @MockitoBean
    private AlmacenamientoService almacenamiento;

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
        reset(almacenamiento);
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '0000001', 'admin@dubss.com', ?, 'Administrador', 'DUBSS' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS'""", passwordEncoder.encode("secreta1"));
        TransactionTemplate tx = new TransactionTemplate(transacciones);
        DatosPruebaListaPriorizada cargador = new DatosPruebaListaPriorizada(jdbc, tx);
        ReflectionTestUtils.setField(cargador, "cantidad", 40);
        cargador.run();
        DatosPruebaFinalistas finalistas = new DatosPruebaFinalistas(jdbc, tx, passwordEncoder);
        ReflectionTestUtils.setField(finalistas, "contrasena", "clave-de-prueba");
        finalistas.run();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private DatosPruebaExpedientes cargador() {
        return new DatosPruebaExpedientes(jdbc, new TransactionTemplate(transacciones), almacenamiento);
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private List<String> ultimoResultadoPorPosicion() {
        return jdbc.queryForList("""
                SELECT f.posicion || ':' || COALESCE((SELECT CAST(a.resultado AS text) FROM auditoria_expediente a
                       WHERE a.finalista_id = f.id ORDER BY a.fecha_auditoria DESC, a.id DESC LIMIT 1), 'SIN')
                FROM finalista f ORDER BY f.posicion""", String.class);
    }

    @Test
    void run_dejaLosDiezExpedientesConSusSituaciones_ySubeLosArchivos() {
        String estadosAntes = estados();

        cargador().run();

        assertThat(contar("respuesta_socioeconomica")).isEqualTo(10);
        assertThat(contar("documento_adjunto")).isEqualTo(34);
        assertThat(contar("convocatoria_documento")).isEqualTo(4);
        assertThat(contar("auditoria_expediente")).isEqualTo(6);
        assertThat(ultimoResultadoPorPosicion()).containsExactly("1:VALIDADO", "2:OBSERVADO", "3:SIN",
                "4:VALIDADO", "5:NO_VALIDO", "6:OBSERVADO", "7:SIN", "8:SIN", "9:SIN", "10:SIN");
        assertThat(jdbc.queryForList("""
                SELECT posicion FROM finalista WHERE documentacion_recibida_en IS NOT NULL ORDER BY posicion""",
                Integer.class)).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM documento_adjunto d JOIN tipo_documento t ON t.id = d.tipo_documento_id
                JOIN postulacion po ON po.id = d.postulacion_id
                JOIN finalista f ON f.postulacion_id = po.id
                WHERE f.posicion = 2 AND t.nombre = 'Certificado de notas'""", Long.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT puntaje_academico FROM auditoria_expediente a JOIN finalista f ON f.id = a.finalista_id
                WHERE f.posicion = 1""", java.math.BigDecimal.class)).isEqualByComparingTo("25.00");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacion n JOIN postulacion po ON po.id = n.postulacion_id
                JOIN finalista f ON f.postulacion_id = po.id
                WHERE f.posicion = 2 AND n.tipo = CAST('WARNING' AS tipo_notificacion)""", Long.class)).isEqualTo(1);
        verify(almacenamiento, times(34)).subir(any(), any(), eq("application/pdf"));
        assertThat(estados()).isNotEqualTo(estadosAntes);
        assertThat(jdbc.queryForList("""
                SELECT f.posicion FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.estado = CAST('devuelta' AS estado_postulacion)""", Integer.class)).containsExactly(6);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM postulacion_historial h JOIN finalista f ON f.postulacion_id = h.postulacion_id
                WHERE f.posicion = 6 AND h.estado_nuevo = CAST('devuelta' AS estado_postulacion)
                  AND h.motivo IS NOT NULL""", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacion WHERE titulo LIKE 'Tu postulación fue devuelta%'""",
                Long.class)).isEqualTo(1);
    }

    @Test
    void run_esIdempotente() {
        cargador().run();
        cargador().run();

        assertThat(contar("respuesta_socioeconomica")).isEqualTo(10);
        assertThat(contar("documento_adjunto")).isEqualTo(34);
        assertThat(contar("auditoria_expediente")).isEqualTo(6);
        verify(almacenamiento, times(34)).subir(any(), any(), any());
    }

    @Test
    void run_conMinioCaido_guardaIgualLasFilasYDejaDeIntentarSubir() {
        doThrow(new AlmacenamientoException("sin minio", null)).when(almacenamiento).subir(any(), any(), any());

        cargador().run();

        assertThat(contar("documento_adjunto")).isEqualTo(34);
        assertThat(contar("auditoria_expediente")).isEqualTo(6);
        verify(almacenamiento, times(1)).subir(any(), any(), any());
    }

    @Test
    void run_sinLaConvocatoriaDePrueba_noHaceNada() {
        borrarTodo();
        cargador().run();

        assertThat(contar("tipo_documento")).isZero();
        assertThat(contar("documento_adjunto")).isZero();
    }

    @Test
    void pdf_esUnDocumentoBienFormado() {
        String texto = new String(DatosPruebaExpedientes.pdf("Carnet de identidad (prueba)"),
                StandardCharsets.ISO_8859_1);

        assertThat(texto).startsWith("%PDF-1.4").endsWith("%%EOF\n").contains("Carnet de identidad prueba");
        int inicioXref = Integer.parseInt(texto.substring(texto.indexOf("startxref\n") + 10).split("\n")[0]);
        assertThat(texto.substring(inicioXref)).startsWith("xref");
    }

    private String estados() {
        return jdbc.queryForList("SELECT estado::text || ':' || id FROM postulacion ORDER BY id", String.class)
                .toString();
    }

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
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
