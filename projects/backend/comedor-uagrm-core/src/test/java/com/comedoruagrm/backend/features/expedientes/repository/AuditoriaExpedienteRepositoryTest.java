package com.comedoruagrm.backend.features.expedientes.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.expedientes.model.AuditoriaExpediente;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Auditorias del expediente (CU07): guardar, la mas reciente y borrado logico. Cada test se revierte. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditoriaExpedienteRepositoryTest {

    @Autowired
    private AuditoriaExpedienteRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private long finalista;
    private long otroFinalista;
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
        ExpedienteFixtures datos = new ExpedienteFixtures(jdbc);
        long convocatoria = datos.convocatoria("REPO-1");
        finalista = datos.finalista(datos.postulacion(convocatoria, datos.postulante("9200001"), "calificada"), 1);
        otroFinalista = datos.finalista(datos.postulacion(convocatoria, datos.postulante("9200002"), "calificada"), 2);
        auditor = datos.usuario("9200003", "ADMINISTRADOR_DUBSS");
    }

    private AuditoriaExpediente guardar(long deFinalista, ResultadoAuditoria resultado, String observaciones,
            String puntaje, String fecha) {
        return repository.saveAndFlush(new AuditoriaExpediente(deFinalista, auditor, Instant.parse(fecha), resultado,
                observaciones, puntaje == null ? null : new BigDecimal(puntaje)));
    }

    @Test
    void guarda_conElEnumNativoElPuntajeYElCodigoPublico() {
        AuditoriaExpediente guardada = guardar(finalista, ResultadoAuditoria.VALIDADO, null, "25.50",
                "2026-11-06T10:00:00Z");

        assertThat(guardada.getCodigo()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT resultado::text FROM auditoria_expediente WHERE id = ?", String.class,
                guardada.getId())).isEqualTo("VALIDADO");
        assertThat(jdbc.queryForObject("SELECT puntaje_academico FROM auditoria_expediente WHERE id = ?",
                BigDecimal.class, guardada.getId())).isEqualByComparingTo("25.50");
    }

    @Test
    void laMasReciente_esLaDeFechaMayor_yNoLaUltimaInsertada() {
        guardar(finalista, ResultadoAuditoria.VALIDADO, null, "20", "2026-11-06T12:00:00Z");
        guardar(finalista, ResultadoAuditoria.OBSERVADO, "Falta un documento", null, "2026-11-06T09:00:00Z");

        AuditoriaExpediente vigente = repository.findFirstByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista)
                .orElseThrow();

        assertThat(vigente.getResultado()).isEqualTo(ResultadoAuditoria.VALIDADO);
    }

    @Test
    void conLaMismaFecha_desempataElIdMasAlto() {
        guardar(finalista, ResultadoAuditoria.OBSERVADO, "Falta un documento", null, "2026-11-06T10:00:00Z");
        guardar(finalista, ResultadoAuditoria.NO_VALIDO, "No procede", null, "2026-11-06T10:00:00Z");

        assertThat(repository.findFirstByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista).orElseThrow()
                .getResultado()).isEqualTo(ResultadoAuditoria.NO_VALIDO);
    }

    @Test
    void lista_devuelveTodasDeLaMasRecienteALaMasAntiguaYSoloLasDelFinalista() {
        guardar(finalista, ResultadoAuditoria.OBSERVADO, "Falta un documento", null, "2026-11-05T10:00:00Z");
        guardar(finalista, ResultadoAuditoria.VALIDADO, null, "28", "2026-11-06T10:00:00Z");
        guardar(otroFinalista, ResultadoAuditoria.NO_VALIDO, "No procede", null, "2026-11-07T10:00:00Z");

        assertThat(repository.findByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista))
                .extracting(AuditoriaExpediente::getResultado)
                .containsExactly(ResultadoAuditoria.VALIDADO, ResultadoAuditoria.OBSERVADO);
    }

    @Test
    void sinAuditorias_laMasRecienteEstaVacia() {
        assertThat(repository.findFirstByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista)).isEmpty();
    }

    @Test
    void unaAuditoriaEliminadaLogicamenteNoSeLee() {
        AuditoriaExpediente guardada = guardar(finalista, ResultadoAuditoria.VALIDADO, null, "20",
                "2026-11-06T10:00:00Z");
        jdbc.update("UPDATE auditoria_expediente SET eliminado_en = now() WHERE id = ?", guardada.getId());

        assertThat(repository.findByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista)).isEmpty();
        assertThat(repository.findFirstByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista)).isEmpty();
    }

    private AuditoriaExpediente guardarConClave(long deFinalista, UUID clave) {
        return repository.saveAndFlush(new AuditoriaExpediente(deFinalista, auditor,
                Instant.parse("2026-11-06T10:00:00Z"), ResultadoAuditoria.VALIDADO, null, new BigDecimal("20"),
                clave));
    }

    @Test
    void buscaPorLaClaveDeIdempotenciaDentroDelFinalista() {
        UUID clave = UUID.randomUUID();
        AuditoriaExpediente guardada = guardarConClave(finalista, clave);

        assertThat(repository.findByFinalistaIdAndClaveIdempotencia(finalista, clave)).contains(guardada);
        assertThat(repository.findByFinalistaIdAndClaveIdempotencia(otroFinalista, clave)).isEmpty();
        assertThat(repository.findByFinalistaIdAndClaveIdempotencia(finalista, UUID.randomUUID())).isEmpty();
    }

    @Test
    void laMismaClaveSeRepiteEnOtroFinalista_peroNoEnElMismo() {
        UUID clave = UUID.randomUUID();
        guardarConClave(finalista, clave);
        guardarConClave(otroFinalista, clave);

        assertThatThrownBy(() -> guardarConClave(finalista, clave)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void variasAuditoriasSinClave_noChocan() {
        guardarConClave(finalista, null);
        guardarConClave(finalista, null);

        assertThat(repository.findByFinalistaIdOrderByFechaAuditoriaDescIdDesc(finalista)).hasSize(2);
    }
}
