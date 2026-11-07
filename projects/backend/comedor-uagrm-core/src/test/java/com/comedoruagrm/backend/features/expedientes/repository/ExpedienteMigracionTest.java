package com.comedoruagrm.backend.features.expedientes.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Migracion de la auditoria del expediente (CU07): tablas, enum y CHECK del puntaje academico. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpedienteMigracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    private ExpedienteFixtures datos;
    private long postulacion;
    private long finalista;
    private long auditor;

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
        datos = new ExpedienteFixtures(jdbc);
        long postulante = datos.postulante("9100001");
        postulacion = datos.postulacion(datos.convocatoria("MIG-1"), postulante, "calificada");
        finalista = datos.finalista(postulacion, 1);
        auditor = datos.usuario("9100002", "ADMINISTRADOR_DUBSS");
    }

    private int insertar(String resultado, String observaciones, String puntaje) {
        return jdbc.update("""
                INSERT INTO auditoria_expediente (finalista_id, auditor_id, resultado, observaciones, puntaje_academico)
                VALUES (?, ?, CAST(? AS resultado_auditoria), ?, CAST(? AS numeric))""",
                finalista, auditor, resultado, observaciones, puntaje);
    }

    @Test
    void migracion_creaLasCincoTablas() {
        Long tablas = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'
                AND table_name IN ('auditoria_expediente', 'tipo_documento', 'convocatoria_documento',
                                   'documento_adjunto', 'respuesta_socioeconomica')""", Long.class);
        assertThat(tablas).isEqualTo(5);
    }

    @Test
    void auditoria_aceptaLosLimitesDelPuntajeAcademicoYNulo() {
        assertThat(insertar("VALIDADO", null, "0")).isEqualTo(1);
        assertThat(insertar("VALIDADO", null, "30")).isEqualTo(1);
        assertThat(insertar("VALIDADO", null, "25.75")).isEqualTo(1);
        assertThat(insertar("OBSERVADO", "Falta un documento", null)).isEqualTo(1);
    }

    // Un INSERT que falla aborta la transaccion del test: cada rechazo va en su propio test.
    @Test
    void auditoria_conPuntajeAcademicoPorEncimaDe30_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar("VALIDADO", null, "30.01")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_conPuntajeAcademicoNegativo_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar("VALIDADO", null, "-0.01")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_conUnResultadoQueNoEstaEnElEnum_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar("APROBADO", null, null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_conObservacionesDeExactamente500Caracteres_seGuarda() {
        assertThat(insertar("OBSERVADO", "x".repeat(500), null)).isEqualTo(1);
    }

    @Test
    void auditoria_conObservacionesDeMasDe500Caracteres_laBaseLaRechaza() {
        assertThatThrownBy(() -> insertar("OBSERVADO", "x".repeat(501), null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_unFinalistaPuedeTenerVarias() {
        insertar("OBSERVADO", "Falta un documento", null);
        insertar("VALIDADO", null, "20");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM auditoria_expediente WHERE finalista_id = ?",
                Long.class, finalista)).isEqualTo(2);
    }

    @Test
    void auditoria_lleva_codigoPublicoYBorradoLogico() {
        insertar("VALIDADO", null, "20");

        assertThat(jdbc.queryForObject("""
                SELECT codigo_auditoria_expediente IS NOT NULL AND eliminado_en IS NULL
                FROM auditoria_expediente WHERE finalista_id = ?""", Boolean.class, finalista)).isTrue();
    }

    // ---- UT-CU07-BE-02: la IA califica de 0 a 70 ----

    @Test
    void puntajeDeLaIa_de70Exactos_seGuarda() {
        datos.puntajeIa(postulacion, "70.00", "{}");

        assertThat(jdbc.queryForObject("SELECT max(puntaje_total) FROM puntaje_postulacion WHERE postulacion_id = ?",
                java.math.BigDecimal.class, postulacion)).isEqualByComparingTo("70.00");
    }

    @Test
    void puntajeDeLaIa_porEncimaDe70_laBaseLoRechaza() {
        assertThatThrownBy(() -> datos.puntajeIa(postulacion, "70.01", "{}")).isInstanceOf(DataAccessException.class);
    }
}
