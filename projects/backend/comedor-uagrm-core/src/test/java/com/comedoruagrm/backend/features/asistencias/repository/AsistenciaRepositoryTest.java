package com.comedoruagrm.backend.features.asistencias.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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

import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository.Becado;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas y registro idempotente de la asistencia (CU10) y su migracion, con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AsistenciaRepositoryTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final Instant AHORA = Instant.parse("2026-11-16T15:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AsistenciaRepository repository;

    private AsistenciasConsultaRepository consulta;
    private ExpedienteFixtures fx;
    private long convocatoria;
    private long personal;

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
        fx = new ExpedienteFixtures(jdbc);
        consulta = new AsistenciasConsultaRepository(new NamedParameterJdbcTemplate(jdbc));
        convocatoria = fx.convocatoria("ASIREP");
        personal = fx.usuario("9760001", "PERSONAL_COMEDOR");
    }

    private long beca(String ci, String estado) {
        long usuario = fx.postulante(ci);
        return jdbc.queryForObject("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), DATE '2026-09-01') RETURNING id""", Long.class, convocatoria,
                usuario, estado);
    }

    @Test
    void insertarSiNoExiste_laPrimeraVezCreaYLaSegundaNoDuplica() {
        long beca = beca("9760010", "VIGENTE");

        assertThat(repository.insertarSiNoExiste(beca, personal, HOY, AHORA)).isEqualTo(1);
        assertThat(repository.insertarSiNoExiste(beca, personal, HOY, AHORA.plusSeconds(60))).isZero();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM asistencia", Long.class)).isEqualTo(1);
        assertThat(repository.findByBecaIdAndFecha(beca, HOY)).get()
                .extracting("horaRegistro").isEqualTo(AHORA);
    }

    @Test
    void insertarSiNoExiste_otroDiaOOtraBeca_creaOtraFila() {
        long uno = beca("9760010", "VIGENTE");
        long dos = beca("9760011", "VIGENTE");

        assertThat(repository.insertarSiNoExiste(uno, personal, HOY, AHORA)).isEqualTo(1);
        assertThat(repository.insertarSiNoExiste(uno, personal, HOY.plusDays(1), AHORA)).isEqualTo(1);
        assertThat(repository.insertarSiNoExiste(dos, personal, HOY, AHORA)).isEqualTo(1);
    }

    @Test
    void insertarSiNoExiste_conLaAsistenciaEliminadaLogicamente_permiteRegistrarDeNuevo() {
        long beca = beca("9760010", "VIGENTE");
        repository.insertarSiNoExiste(beca, personal, HOY, AHORA);
        jdbc.update("UPDATE asistencia SET eliminado_en = now()");

        assertThat(repository.insertarSiNoExiste(beca, personal, HOY, AHORA)).isEqualTo(1);
    }

    @Test
    void buscarPorCi_devuelveElBecadoConSuAsistenciaDeLaFecha_sinDistinguirMayusculas() {
        long beca = beca("9760020", "VIGENTE");
        jdbc.update("UPDATE usuario SET ci = '9760020a' WHERE ci = '9760020'");

        Becado sinRegistro = consulta.buscarPorCi("9760020A", HOY).orElseThrow();
        assertThat(sinRegistro.becaId()).isEqualTo(beca);
        assertThat(sinRegistro.estado()).isEqualTo("VIGENTE");
        assertThat(sinRegistro.horaRegistro()).isNull();

        repository.insertarSiNoExiste(beca, personal, HOY, AHORA);
        assertThat(consulta.buscarPorCi("9760020a", HOY).orElseThrow().horaRegistro()).isEqualTo(AHORA);
        assertThat(consulta.buscarPorCi("9760020a", HOY.plusDays(1)).orElseThrow().horaRegistro()).isNull();
    }

    @Test
    void buscarPorCi_conVariasBecas_prefiereLaVigente() {
        long usuario = fx.postulante("9760030");
        long otra = fx.convocatoria("ASIREP2");
        jdbc.update("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST('VIGENTE' AS estado_beca), DATE '2026-09-01')""", convocatoria, usuario);
        jdbc.update("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST('BAJA' AS estado_beca), DATE '2026-09-01')""", otra, usuario);

        assertThat(consulta.buscarPorCi("9760030", HOY).orElseThrow().estado()).isEqualTo("VIGENTE");
    }

    @Test
    void buscarPorCi_sinBecaOInexistente_devuelveVacio() {
        fx.postulante("9760040");

        assertThat(consulta.buscarPorCi("9760040", HOY)).isEmpty();
        assertThat(consulta.buscarPorCi("0", HOY)).isEmpty();
    }

    @Test
    void cabeceraBeca_devuelveElEstadoYUnaInexistenteVacio() {
        long beca = beca("9760050", "SUSPENDIDA");
        UUID codigo = jdbc.queryForObject("SELECT codigo_beca FROM beca WHERE id = ?", UUID.class, beca);

        assertThat(consulta.cabeceraBeca(codigo).orElseThrow().estado()).isEqualTo("SUSPENDIDA");
        assertThat(consulta.cabeceraBeca(UUID.randomUUID())).isEmpty();
    }
}
