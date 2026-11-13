package com.comedoruagrm.backend.features.actas.repository;

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

/** Migracion del acta de adjudicacion (CU08): tabla, una por convocatoria, numero unico y FK. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActaAdjudicacionMigracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long convocatoria;
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
        ExpedienteFixtures datos = new ExpedienteFixtures(jdbc);
        convocatoria = datos.convocatoria("ACTMIG");
        admin = datos.usuario("9700001", "ADMINISTRADOR_DUBSS");
    }

    private int insertar(Long convocatoriaId, String numero, Long generadaPor) {
        return jdbc.update("INSERT INTO acta_adjudicacion (convocatoria_id, numero, storage_path, generada_por) "
                + "VALUES (?, ?, 'actas/x/acta.pdf', ?)", convocatoriaId, numero, generadaPor);
    }

    @Test
    void migracion_creaLaTablaConSuCodigoPublicoYLaFecha() {
        insertar(convocatoria, "001/2026", admin);

        var fila = jdbc.queryForMap("SELECT codigo_acta_adjudicacion, generada_en, creado_en, eliminado_en "
                + "FROM acta_adjudicacion WHERE convocatoria_id = ?", convocatoria);

        assertThat(fila.get("codigo_acta_adjudicacion")).isNotNull();
        assertThat(fila.get("generada_en")).isNotNull();
        assertThat(fila.get("creado_en")).isNotNull();
        assertThat(fila.get("eliminado_en")).isNull();
    }

    @Test
    void unicidad_dosActasParaLaMismaConvocatoria_laSegundaFalla() {
        insertar(convocatoria, "001/2026", admin);

        assertThatThrownBy(() -> insertar(convocatoria, "002/2026", admin)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void unicidad_siLaPrimeraEstaEliminada_seAceptaOtraConvocatoriaYaNoCuenta() {
        insertar(convocatoria, "001/2026", admin);
        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);

        assertThat(insertar(convocatoria, "002/2026", admin)).isEqualTo(1);
    }

    @Test
    void unicidad_numeroRepetido_falla() {
        long otra = new ExpedienteFixtures(jdbc).convocatoria("ACTMIG2");
        insertar(convocatoria, "001/2026", admin);

        assertThatThrownBy(() -> insertar(otra, "001/2026", admin)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void unicidad_numeroDeUnaFilaEliminada_noSeReutiliza() {
        long otra = new ExpedienteFixtures(jdbc).convocatoria("ACTMIG3");
        insertar(convocatoria, "001/2026", admin);
        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);

        assertThatThrownBy(() -> insertar(otra, "001/2026", admin)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void fk_conUnaConvocatoriaInexistente_laBaseLaRechaza() {
        assertThatThrownBy(() -> insertar(-1L, "001/2026", admin)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void fk_conUnUsuarioInexistente_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar(convocatoria, "001/2026", -1L)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void campos_obligatorios_noAceptanNulo() {
        assertThatThrownBy(() -> insertar(null, "001/2026", admin)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertar(convocatoria, null, admin)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertar(convocatoria, "001/2026", null)).isInstanceOf(DataAccessException.class);
    }
}
