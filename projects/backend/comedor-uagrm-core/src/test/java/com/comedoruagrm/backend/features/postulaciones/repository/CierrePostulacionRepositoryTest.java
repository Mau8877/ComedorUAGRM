package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.postulaciones.repository.CierrePostulacionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas del cierre de una postulacion (CU20) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CierrePostulacionConsultaRepository.class)
class CierrePostulacionRepositoryTest {

    @Autowired
    private CierrePostulacionConsultaRepository consulta;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    private UUID codigoFinalista(long finalista) {
        return jdbc.queryForObject("SELECT codigo_finalista FROM finalista WHERE id = ?", UUID.class, finalista);
    }

    @Test
    void cabecera_devuelveFinalistaPostulacionConvocatoriaYPostulante() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ARC1");
        long postulacion = fx.postulacion(conv, fx.postulante("9400001"), "calificada");
        long finalista = fx.finalista(postulacion, 2);

        Cabecera c = consulta.cabecera(codigoFinalista(finalista)).orElseThrow();

        assertThat(c.finalistaId()).isEqualTo(finalista);
        assertThat(c.finalistaCodigo()).isEqualTo(codigoFinalista(finalista));
        assertThat(c.postulacionId()).isEqualTo(postulacion);
        assertThat(c.postulacionCodigo()).isNotNull();
        assertThat(c.convocatoriaId()).isEqualTo(conv);
        assertThat(c.estado()).isEqualTo("calificada");
        assertThat(c.convocatoriaNombre()).isEqualTo("Conv ARC1");
        assertThat(c.postulanteUsuarioCodigo()).isNotNull();
        assertThat(c.postulanteNombre()).isNotBlank();
        assertThat(consulta.estadoActual(postulacion)).isEqualTo("calificada");
    }

    @Test
    void cabecera_conUnaPostulacionYaCerrada_traeElEstadoActual() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ARC2");
        long postulacion = fx.postulacion(conv, fx.postulante("9400002"), "archivada");
        long finalista = fx.finalista(postulacion, 1);

        assertThat(consulta.cabecera(codigoFinalista(finalista)).orElseThrow().estado()).isEqualTo("archivada");
    }

    @Test
    void cabecera_conFinalistaInexistenteOEliminado_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ARC3");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9400003"), "calificada"), 1);
        UUID codigo = codigoFinalista(finalista);
        jdbc.update("UPDATE finalista SET eliminado_en = now() WHERE id = ?", finalista);

        assertThat(consulta.cabecera(codigo)).isEmpty();
        assertThat(consulta.cabecera(UUID.randomUUID())).isEmpty();
    }

    @Test
    void bloquearPostulacion_noFallaConUnaFilaExistente() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ARC4");
        long postulacion = fx.postulacion(conv, fx.postulante("9400004"), "calificada");

        consulta.bloquearPostulacion(postulacion);

        assertThat(consulta.estadoActual(postulacion)).isEqualTo("calificada");
    }

    @Test
    void resultadosPublicados_enUnaConvocatoriaSinPublicar_esFalso() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ARC5");

        assertThat(consulta.resultadosPublicados(conv)).isFalse();
    }
}
