package com.comedoruagrm.backend.features.resultados.repository;

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

/** Migracion de la publicacion de resultados (CU22): tabla, unicidad por convocatoria, FK y estado PUBLICADA. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PublicacionResultadoMigracionTest {

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
        convocatoria = datos.convocatoria("PUBMIG");
        admin = datos.usuario("9600001", "ADMINISTRADOR_DUBSS");
    }

    private int insertar(Long convocatoriaId, Long publicadoPor) {
        return jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)",
                convocatoriaId, publicadoPor);
    }

    @Test
    void migracion_creaLaTablaConSuCodigoPublicoYLaFecha() {
        insertar(convocatoria, admin);

        var fila = jdbc.queryForMap("SELECT codigo_publicacion_resultado, publicado_en, creado_en, eliminado_en "
                + "FROM publicacion_resultado WHERE convocatoria_id = ?", convocatoria);

        assertThat(fila.get("codigo_publicacion_resultado")).isNotNull();
        assertThat(fila.get("publicado_en")).isNotNull();
        assertThat(fila.get("creado_en")).isNotNull();
        assertThat(fila.get("eliminado_en")).isNull();
    }

    @Test
    void unicidad_dosPublicacionesParaLaMismaConvocatoria_laSegundaFalla() {
        insertar(convocatoria, admin);

        assertThatThrownBy(() -> insertar(convocatoria, admin)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void unicidad_siLaPrimeraEstaEliminada_seAceptaOtra() {
        insertar(convocatoria, admin);
        jdbc.update("UPDATE publicacion_resultado SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);

        assertThat(insertar(convocatoria, admin)).isEqualTo(1);
    }

    @Test
    void fk_conUnaConvocatoriaInexistente_laBaseLaRechaza() {
        assertThatThrownBy(() -> insertar(-1L, admin)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void fk_conUnUsuarioInexistente_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar(convocatoria, -1L)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void campos_obligatorios_noAceptanNulo() {
        assertThatThrownBy(() -> insertar(null, admin)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertar(convocatoria, null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void estadoConvocatoria_aceptaPublicada() {
        int filas = jdbc.update("UPDATE convocatoria SET estado = CAST('PUBLICADA' AS estado_convocatoria) "
                + "WHERE id = ?", convocatoria);

        assertThat(filas).isEqualTo(1);
    }
}
