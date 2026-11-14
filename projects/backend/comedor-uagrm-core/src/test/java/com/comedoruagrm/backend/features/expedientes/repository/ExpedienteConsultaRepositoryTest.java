package com.comedoruagrm.backend.features.expedientes.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
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

import com.comedoruagrm.backend.features.expedientes.dto.AuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.DocumentoExpedienteResponse;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository.Cabecera;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consulta del expediente (CU07) con Postgres real. Cada test se revierte. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExpedienteConsultaRepository.class)
class ExpedienteConsultaRepositoryTest {

    @Autowired
    private ExpedienteConsultaRepository consulta;

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
    void cabecera_conPuntajeDeIa_devuelveDatosYDesglose() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("EXP1");
        long postulacion = fx.postulacion(conv, fx.postulante("9200001"), "calificada");
        long finalista = fx.finalista(postulacion, 3);
        fx.puntajeIa(postulacion, "55.50", "{\"ingresos\": 20}");

        Cabecera c = consulta.cabecera(codigoFinalista(finalista)).orElseThrow();

        assertThat(c.posicion()).isEqualTo(3);
        assertThat(c.ci()).isEqualTo("9200001");
        assertThat(c.estadoPostulacion()).isEqualTo("calificada");
        assertThat(c.puntajeIa()).isEqualByComparingTo("55.50");
        assertThat(c.desgloseJson()).contains("ingresos");
        assertThat(c.entregaRecibidaEn()).isNull();
    }

    @Test
    void cabecera_conFinalistaEliminadoOInexistente_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("EXP2");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9200002"), "calificada"), 1);
        UUID codigo = codigoFinalista(finalista);
        jdbc.update("UPDATE finalista SET eliminado_en = now() WHERE id = ?", finalista);

        assertThat(consulta.cabecera(codigo)).isEmpty();
        assertThat(consulta.cabecera(UUID.randomUUID())).isEmpty();
    }

    @Test
    void documentos_marcaRecibidoYFaltaYIncluyeLosNoExigidos() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("EXP3");
        long postulacion = fx.postulacion(conv, fx.postulante("9200003"), "calificada");
        long carnet = fx.tipoDocumento("Carnet-EXP3");
        long luz = fx.tipoDocumento("Factura de luz-EXP3");
        long extra = fx.tipoDocumento("Otro-EXP3");
        fx.exigeDocumento(conv, carnet, true);
        fx.exigeDocumento(conv, luz, false);
        fx.documentoAdjunto(postulacion, carnet, "carnet.pdf", "ruta/secreta/carnet.pdf");
        fx.documentoAdjunto(postulacion, extra, "otro.pdf", "ruta/secreta/otro.pdf");

        List<DocumentoExpedienteResponse> docs = consulta.documentos(postulacion, convocatoriaDe(postulacion));

        assertThat(docs).extracting(DocumentoExpedienteResponse::tipo)
                .containsExactly("Carnet-EXP3", "Factura de luz-EXP3", "Otro-EXP3");
        assertThat(docs).extracting(DocumentoExpedienteResponse::estado)
                .containsExactly("RECIBIDO", "FALTA", "RECIBIDO");
        assertThat(docs.get(0).obligatorio()).isTrue();
        assertThat(docs.get(1).codigo()).isNull();
        assertThat(docs.get(2).obligatorio()).isFalse();
    }

    @Test
    void auditorias_vienenDeLaMasRecienteALaMasAntigua() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("EXP4");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9200004"), "calificada"), 1);
        long auditor = fx.usuario("9200005", "ADMINISTRADOR_DUBSS");
        fx.auditoria(finalista, auditor, "OBSERVADO", "Falta carnet", null, "2026-11-05T10:00:00Z");
        fx.auditoria(finalista, auditor, "VALIDADO", null, "25.00", "2026-11-06T10:00:00Z");

        List<AuditoriaResponse> lista = consulta.auditorias(finalista);

        assertThat(lista).extracting(AuditoriaResponse::resultado)
                .containsExactly(ResultadoAuditoria.VALIDADO, ResultadoAuditoria.OBSERVADO);
        assertThat(lista.get(0).puntajeAcademico()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(lista.get(0).auditor()).isEqualTo("Ana Rojas");
    }

    @Test
    void respuestaSocioeconomica_sinFila_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("EXP5");
        long postulacion = fx.postulacion(conv, fx.postulante("9200006"), "calificada");
        assertThat(consulta.respuestaSocioeconomica(postulacion)).isEmpty();

        fx.respuestaSocioeconomica(postulacion, "68.50");

        assertThat(consulta.respuestaSocioeconomica(postulacion).orElseThrow().promedioAcademico())
                .isEqualByComparingTo("68.50");
    }

    @Test
    void rutaDocumento_soloDevuelveLaRutaSiElDocumentoEsDeEsaPostulacion() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("EXP6");
        long propia = fx.postulacion(conv, fx.postulante("9200007"), "calificada");
        long ajena = fx.postulacion(conv, fx.postulante("9200008"), "calificada");
        long tipo = fx.tipoDocumento("Carnet-EXP6");
        long doc = fx.documentoAdjunto(propia, tipo, "carnet.pdf", "postulaciones/propia/carnet.pdf");
        UUID codigo = jdbc.queryForObject("SELECT codigo_documento_adjunto FROM documento_adjunto WHERE id = ?",
                UUID.class, doc);

        assertThat(consulta.rutaDocumento(propia, codigo)).contains("postulaciones/propia/carnet.pdf");
        assertThat(consulta.rutaDocumento(ajena, codigo)).isEmpty();
        assertThat(consulta.rutaDocumento(propia, UUID.randomUUID())).isEmpty();
    }

    private long convocatoriaDe(long postulacion) {
        return jdbc.queryForObject("SELECT convocatoria_id FROM postulacion WHERE id = ?", Long.class, postulacion);
    }
}
