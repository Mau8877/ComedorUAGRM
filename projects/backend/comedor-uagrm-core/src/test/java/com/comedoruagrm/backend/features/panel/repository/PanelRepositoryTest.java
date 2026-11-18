package com.comedoruagrm.backend.features.panel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Los conteos de las pantallas de inicio contra Postgres real: las consultas corren sobre el esquema migrado. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PanelRepositoryTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 18);

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void sinDatos_losConteosSonCeroYNoHayConvocatoriaAbierta() {
        PanelRepository repositorio = new PanelRepository(jdbc);

        assertThat(repositorio.convocatoriasAbiertas()).isZero();
        assertThat(repositorio.postulacionesEnEstado("pendiente")).isZero();
        assertThat(repositorio.postulacionesEnEstado("calificada")).isZero();
        assertThat(repositorio.becasEnEstado("VIGENTE")).isZero();
        assertThat(repositorio.becasEnEstado("SUSPENDIDA")).isZero();
        assertThat(repositorio.asistenciasDelDia(FECHA)).isZero();
        assertThat(repositorio.asistenciasDelDiaRegistradasPor(FECHA, 1L)).isZero();
        assertThat(repositorio.ultimoRegistroDelDia(FECHA)).isEmpty();
        assertThat(repositorio.convocatoriaAbierta()).isEmpty();
        assertThat(repositorio.estadoEnConvocatoriaAbierta(1L)).isEmpty();
        assertThat(repositorio.asistenciasPorFecha(FECHA.minusDays(6), FECHA)).isEmpty();
    }
}
