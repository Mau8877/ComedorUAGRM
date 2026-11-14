package com.comedoruagrm.backend.features.dashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository.ConvocatoriaFila;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository.GestionFila;
import com.comedoruagrm.backend.support.JdbcContadorDeConsultas;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Los catalogos del dashboard (CU12) contra Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FiltrosDashboardRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private JdbcContadorDeConsultas contador;
    private FiltrosDashboardRepository repositorio;

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
        contador = new JdbcContadorDeConsultas(dataSource);
        repositorio = new FiltrosDashboardRepository(new NamedParameterJdbcTemplate(contador));
    }

    private long gestion(String etiqueta, String inicio, String fin) {
        return jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, CAST(? AS date), CAST(? AS date)) RETURNING id""", Long.class, etiqueta, inicio, fin);
    }

    private long convocatoria(long gestion, String nombre, String estado, String apertura) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz) + interval '30 days',
                        CAST(? AS estado_convocatoria)) RETURNING id""", Long.class, gestion, nombre, apertura,
                apertura, estado);
    }

    @Test
    void gestiones_vienenPorInicioDescendente_ySinLasEliminadas() {
        gestion("FD-I", "2031-01-01", "2031-06-30");
        gestion("FD-II", "2031-07-01", "2031-12-31");
        long borrada = gestion("FD-X", "2032-01-01", "2032-06-30");
        jdbc.update("UPDATE gestion_academica SET eliminado_en = now() WHERE id = ?", borrada);

        var gestiones = repositorio.gestiones().stream().map(GestionFila::etiqueta)
                .filter(e -> e.startsWith("FD-")).toList();

        assertThat(gestiones).containsExactly("FD-II", "FD-I");
    }

    @Test
    void convocatorias_omitenBorradoresYEliminadas_yVanPorAperturaDescendente() {
        long g = gestion("FD-C", "2033-01-01", "2033-12-31");
        convocatoria(g, "Vieja", "CERRADA", "2033-02-01T00:00:00Z");
        convocatoria(g, "Nueva", "ABIERTA", "2033-05-01T00:00:00Z");
        convocatoria(g, "Borrador", "BORRADOR", "2033-06-01T00:00:00Z");
        long borrada = convocatoria(g, "Borrada", "CERRADA", "2033-07-01T00:00:00Z");
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", borrada);

        var nombres = repositorio.convocatorias().stream().filter(c -> c.gestionId() == g)
                .map(ConvocatoriaFila::nombre).toList();

        assertThat(nombres).containsExactly("Nueva", "Vieja");
    }

    @Test
    void elNumeroDeConsultasNoCreceConLaCantidadDeGestiones() {
        gestion("FD-N1", "2034-01-01", "2034-06-30");
        contador.reiniciar();
        repositorio.gestiones();
        repositorio.convocatorias();
        int conUna = contador.consultas();

        for (int i = 0; i < 5; i++) {
            long g = gestion("FD-M" + i, "20" + (40 + i) + "-01-01", "20" + (40 + i) + "-06-30");
            convocatoria(g, "C" + i, "ABIERTA", "20" + (40 + i) + "-02-01T00:00:00Z");
        }
        contador.reiniciar();
        repositorio.gestiones();
        repositorio.convocatorias();

        assertThat(contador.consultas()).isEqualTo(conUna).isEqualTo(2);
    }
}
