package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.DevolucionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Tabla `postulacion_historial`, su repository y las consultas de la devolucion (CU19) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DevolucionConsultaRepository.class)
class DevolucionRepositoryTest {

    @Autowired
    private DevolucionConsultaRepository consulta;

    @Autowired
    private PostulacionHistorialRepository historial;

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
    void historial_guardaLaFilaYLaDevuelveDeLaMasRecienteALaMasAntigua() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV1");
        long admin = fx.usuario("9300001", "ADMINISTRADOR_DUBSS");
        long postulacion = fx.postulacion(conv, fx.postulante("9300002"), "calificada");

        historial.saveAndFlush(new PostulacionHistorial(postulacion, admin, EstadoPostulacion.calificada,
                EstadoPostulacion.devuelta, "Falta el carnet"));
        historial.saveAndFlush(new PostulacionHistorial(postulacion, null, EstadoPostulacion.devuelta,
                EstadoPostulacion.pendiente, null));

        List<PostulacionHistorial> filas = historial.findByPostulacionIdOrderByCreadoEnDescIdDesc(postulacion);
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0).getEstadoNuevo()).isEqualTo(EstadoPostulacion.pendiente);
        assertThat(filas.get(1).getMotivo()).isEqualTo("Falta el carnet");
        assertThat(filas.get(1).getUsuarioId()).isEqualTo(admin);
        assertThat(filas.get(1).getCodigo()).isNotNull();
    }

    @Test
    void tabla_noAceptaUnaPostulacionInexistenteNiEstadosNulos() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, estado_anterior, estado_nuevo)
                VALUES (-1, CAST('calificada' AS estado_postulacion), CAST('devuelta' AS estado_postulacion))"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cabecera_devuelveFinalistaPostulacionYPostulante() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV2");
        long postulacion = fx.postulacion(conv, fx.postulante("9300003"), "calificada");
        long finalista = fx.finalista(postulacion, 2);

        Cabecera c = consulta.cabecera(codigoFinalista(finalista)).orElseThrow();

        assertThat(c.finalistaId()).isEqualTo(finalista);
        assertThat(c.postulacionId()).isEqualTo(postulacion);
        assertThat(c.estado()).isEqualTo("calificada");
        assertThat(c.convocatoriaNombre()).isEqualTo("Conv DEV2");
        assertThat(c.postulanteCodigo()).isNotNull();
        assertThat(consulta.estadoActual(postulacion)).isEqualTo("calificada");
    }

    @Test
    void cabecera_conFinalistaInexistenteOEliminado_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV3");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9300004"), "calificada"), 1);
        UUID codigo = codigoFinalista(finalista);
        jdbc.update("UPDATE finalista SET eliminado_en = now() WHERE id = ?", finalista);

        assertThat(consulta.cabecera(codigo)).isEmpty();
        assertThat(consulta.cabecera(UUID.randomUUID())).isEmpty();
    }

    @Test
    void auditoriaPosterior_sinAuditoriasEsFalso() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV4");
        long postulacion = fx.postulacion(conv, fx.postulante("9300005"), "calificada");
        long finalista = fx.finalista(postulacion, 1);

        assertThat(consulta.hayAuditoriaPosteriorALaUltimaDevolucion(finalista, postulacion)).isFalse();
    }

    @Test
    void auditoriaPosterior_conUnaAuditoriaYSinDevolucionesEsVerdadero() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV5");
        long admin = fx.usuario("9300006", "ADMINISTRADOR_DUBSS");
        long postulacion = fx.postulacion(conv, fx.postulante("9300007"), "calificada");
        long finalista = fx.finalista(postulacion, 1);
        fx.auditoria(finalista, admin, "OBSERVADO", "Falta algo", null, "2026-11-01T10:00:00Z");

        assertThat(consulta.hayAuditoriaPosteriorALaUltimaDevolucion(finalista, postulacion)).isTrue();
    }

    @Test
    void auditoriaPosterior_unaDevolucionMasRecienteQueLasAuditoriasLaVuelveFalsa_yUnaNuevaLaVuelveVerdadera() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV6");
        long admin = fx.usuario("9300008", "ADMINISTRADOR_DUBSS");
        long postulacion = fx.postulacion(conv, fx.postulante("9300009"), "calificada");
        long finalista = fx.finalista(postulacion, 1);
        fx.auditoria(finalista, admin, "OBSERVADO", "Falta algo", null, "2026-11-01T10:00:00Z");
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                   motivo, creado_en)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), CAST('devuelta' AS estado_postulacion),
                        'Falta el carnet', CAST('2026-11-02T10:00:00Z' AS timestamptz))""", postulacion, admin);

        assertThat(consulta.hayAuditoriaPosteriorALaUltimaDevolucion(finalista, postulacion)).isFalse();

        fx.auditoria(finalista, admin, "VALIDADO", null, "20", "2026-11-03T10:00:00Z");

        assertThat(consulta.hayAuditoriaPosteriorALaUltimaDevolucion(finalista, postulacion)).isTrue();
    }

    @Test
    void bloquearPostulacion_noFallaConUnaFilaExistente() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DEV7");
        long postulacion = fx.postulacion(conv, fx.postulante("9300010"), "calificada");

        consulta.bloquearPostulacion(postulacion);

        assertThat(consulta.estadoActual(postulacion)).isEqualTo("calificada");
    }
}
