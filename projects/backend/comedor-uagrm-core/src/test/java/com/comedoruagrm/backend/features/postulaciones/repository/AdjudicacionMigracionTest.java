package com.comedoruagrm.backend.features.postulaciones.repository;

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

/** Migracion de la adjudicacion (CU05): tabla, enum, indice unico parcial y CHECK de las dos columnas. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdjudicacionMigracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    private ExpedienteFixtures datos;
    private long convocatoria;
    private long postulacion;
    private long admin;

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
        convocatoria = datos.convocatoria("ADJMIG");
        postulacion = datos.postulacion(convocatoria, datos.postulante("9500001"), "calificada");
        admin = datos.usuario("9500002", "ADMINISTRADOR_DUBSS");
    }

    private int insertar(long postulacionId, String resultado, String puntaje) {
        return jdbc.update("""
                INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por)
                VALUES (?, CAST(? AS resultado_adjudicacion), CAST(? AS numeric), ?)""",
                postulacionId, resultado, puntaje, admin);
    }

    @Test
    void migracion_creaLaTablaAdjudicacionConSuCodigoPublicoYLasFechas() {
        insertar(postulacion, "ADJUDICADA", "75.50");

        var fila = jdbc.queryForMap("SELECT codigo_adjudicacion, fecha_adjudicacion, creado_en, eliminado_en "
                + "FROM adjudicacion WHERE postulacion_id = ?", postulacion);

        assertThat(fila.get("codigo_adjudicacion")).isNotNull();
        assertThat(fila.get("fecha_adjudicacion")).isNotNull();
        assertThat(fila.get("creado_en")).isNotNull();
        assertThat(fila.get("eliminado_en")).isNull();
    }

    @Test
    void enum_aceptaLosDosValores() {
        long otra = datos.postulacion(convocatoria, datos.postulante("9500003"), "calificada");

        assertThat(insertar(postulacion, "ADJUDICADA", "80")).isEqualTo(1);
        assertThat(insertar(otra, "NO_ADJUDICADA", "40")).isEqualTo(1);
    }

    @Test
    void enum_conUnValorDesconocido_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar(postulacion, "PENDIENTE", "10")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void puntajeFinal_aceptaLosLimitesYNulo() {
        long a = datos.postulacion(convocatoria, datos.postulante("9500004"), "calificada");
        long b = datos.postulacion(convocatoria, datos.postulante("9500005"), "calificada");

        assertThat(insertar(postulacion, "ADJUDICADA", "0")).isEqualTo(1);
        assertThat(insertar(a, "ADJUDICADA", "100")).isEqualTo(1);
        assertThat(insertar(b, "NO_ADJUDICADA", null)).isEqualTo(1);
    }

    // Un INSERT que falla aborta la transaccion del test: cada rechazo va en su propio test.
    @Test
    void puntajeFinal_porEncimaDe100_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar(postulacion, "ADJUDICADA", "100.01")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void puntajeFinal_negativo_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar(postulacion, "ADJUDICADA", "-1")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void unaSegundaDecisionParaLaMismaPostulacion_laBaseLaRechaza() {
        insertar(postulacion, "NO_ADJUDICADA", "30");

        assertThatThrownBy(() -> insertar(postulacion, "ADJUDICADA", "80")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void unaDecisionEliminadaLogicamente_permiteOtraParaLaMismaPostulacion() {
        insertar(postulacion, "NO_ADJUDICADA", "30");
        jdbc.update("UPDATE adjudicacion SET eliminado_en = now() WHERE postulacion_id = ?", postulacion);

        assertThat(insertar(postulacion, "ADJUDICADA", "80")).isEqualTo(1);
    }

    @Test
    void cantidadFinalistas_aceptaNuloYPositivos() {
        assertThat(jdbc.update("UPDATE convocatoria SET cantidad_finalistas = NULL WHERE id = ?", convocatoria))
                .isEqualTo(1);
        assertThat(jdbc.update("UPDATE convocatoria SET cantidad_finalistas = 1 WHERE id = ?", convocatoria))
                .isEqualTo(1);
        assertThat(jdbc.update("UPDATE convocatoria SET cantidad_finalistas = 250 WHERE id = ?", convocatoria))
                .isEqualTo(1);
    }

    @Test
    void cantidadFinalistas_enCero_laBaseLaRechaza() {
        assertThatThrownBy(() -> jdbc.update("UPDATE convocatoria SET cantidad_finalistas = 0 WHERE id = ?",
                convocatoria)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void cantidadFinalistas_negativa_laBaseLaRechaza() {
        assertThatThrownBy(() -> jdbc.update("UPDATE convocatoria SET cantidad_finalistas = -3 WHERE id = ?",
                convocatoria)).isInstanceOf(DataAccessException.class);
    }
}
