package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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

import com.comedoruagrm.backend.features.postulaciones.model.DocumentoAdjunto;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Documentos de la postulacion (CU16): el model, su repository y las consultas con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DocumentosPostulacionConsultaRepository.class)
class DocumentosPostulacionRepositoryTest {

    @Autowired
    private DocumentosPostulacionConsultaRepository consulta;

    @Autowired
    private DocumentoAdjuntoRepository repositorio;

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

    private UUID codigoUsuario(long usuario) {
        return jdbc.queryForObject("SELECT codigo_usuario FROM usuario WHERE id = ?", UUID.class, usuario);
    }

    private UUID codigoPostulacion(long postulacion) {
        return jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class, postulacion);
    }

    private UUID codigoTipo(long tipo) {
        return jdbc.queryForObject("SELECT codigo_tipo_documento FROM tipo_documento WHERE id = ?", UUID.class, tipo);
    }

    @Test
    void cabecera_devuelveLaPostulacionDelDuenoConElEstadoDeSuConvocatoria() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DOC1");
        long usuario = fx.postulante("9500001");
        long postulacion = fx.postulacion(conv, usuario, "borrador");

        var c = consulta.cabecera(codigoPostulacion(postulacion), codigoUsuario(usuario)).orElseThrow();

        assertThat(c.postulacionId()).isEqualTo(postulacion);
        assertThat(c.convocatoriaId()).isEqualTo(conv);
        assertThat(c.estado()).isEqualTo("borrador");
        assertThat(c.convocatoriaEstado()).isNotBlank();
    }

    @Test
    void cabecera_conPostulacionDeOtroPostulanteOEliminada_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DOC2");
        long dueno = fx.postulante("9500002");
        long otro = fx.postulante("9500003");
        long postulacion = fx.postulacion(conv, dueno, "borrador");
        UUID codigo = codigoPostulacion(postulacion);

        assertThat(consulta.cabecera(codigo, codigoUsuario(otro))).isEmpty();

        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", postulacion);
        assertThat(consulta.cabecera(codigo, codigoUsuario(dueno))).isEmpty();
    }

    @Test
    void requeridos_traeLosDocumentosDeLaConvocatoriaYNoLosEliminados() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DOC3");
        long carnet = fx.tipoDocumento("Carnet DOC3");
        long notas = fx.tipoDocumento("Notas DOC3");
        fx.exigeDocumento(conv, carnet, true);
        fx.exigeDocumento(conv, notas, false);
        jdbc.update("UPDATE convocatoria_documento SET eliminado_en = now() WHERE convocatoria_id = ? "
                + "AND tipo_documento_id = ?", conv, notas);

        var requeridos = consulta.requeridos(conv);

        assertThat(requeridos).hasSize(1);
        assertThat(requeridos.get(0).nombre()).isEqualTo("Carnet DOC3");
        assertThat(requeridos.get(0).obligatorio()).isTrue();
    }

    @Test
    void tipoRequerido_soloDevuelveLosTiposDeLaListaDeLaConvocatoria() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DOC4");
        long exigido = fx.tipoDocumento("Exigido DOC4");
        long ajeno = fx.tipoDocumento("Ajeno DOC4");
        fx.exigeDocumento(conv, exigido, true);

        assertThat(consulta.tipoRequerido(conv, codigoTipo(exigido))).contains(exigido);
        assertThat(consulta.tipoRequerido(conv, codigoTipo(ajeno))).isEmpty();
        assertThat(consulta.tipoRequerido(conv, UUID.randomUUID())).isEmpty();
    }

    @Test
    void repositorio_guardaBuscaPorPostulacionCuentaYOcultaLosEliminados() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DOC5");
        long usuario = fx.postulante("9500005");
        long postulacion = fx.postulacion(conv, usuario, "borrador");
        long otraPostulacion = fx.postulacion(fx.convocatoria("DOC5-OTRA"), usuario, "borrador");
        long tipo = fx.tipoDocumento("Carnet DOC5");

        DocumentoAdjunto d = repositorio.saveAndFlush(new DocumentoAdjunto(postulacion, tipo, "ci.pdf",
                "application/pdf", "postulaciones/x/documentos/a.pdf", 100L, Instant.parse("2026-07-16T15:00:00Z")));

        assertThat(d.getCodigo()).isNotNull();
        assertThat(repositorio.findByCodigoAndPostulacionId(d.getCodigo(), postulacion)).isPresent();
        assertThat(repositorio.findByCodigoAndPostulacionId(d.getCodigo(), otraPostulacion)).isEmpty();
        assertThat(repositorio.countByPostulacionIdAndTipoDocumentoId(postulacion, tipo)).isEqualTo(1);
        assertThat(consulta.adjuntos(postulacion)).hasSize(1);
        assertThat(consulta.adjuntos(postulacion).get(0).nombreArchivo()).isEqualTo("ci.pdf");

        d.setEliminadoEn(Instant.now());
        repositorio.saveAndFlush(d);
        assertThat(repositorio.countByPostulacionIdAndTipoDocumentoId(postulacion, tipo)).isZero();
        assertThat(consulta.adjuntos(postulacion)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM documento_adjunto WHERE postulacion_id = ?",
                Long.class, postulacion)).isEqualTo(1L);
    }

    @Test
    void repositorio_reemplazarArchivoCambiaLosDatosEnLaMismaFila() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("DOC6");
        long usuario = fx.postulante("9500006");
        long postulacion = fx.postulacion(conv, usuario, "devuelta");
        long tipo = fx.tipoDocumento("Carnet DOC6");
        DocumentoAdjunto d = repositorio.saveAndFlush(new DocumentoAdjunto(postulacion, tipo, "viejo.pdf",
                "application/pdf", "ruta/vieja.pdf", 10L, Instant.parse("2026-07-16T15:00:00Z")));

        d.reemplazarArchivo("nuevo.png", "image/png", "ruta/nueva.png", 20L, Instant.parse("2026-07-17T15:00:00Z"));
        repositorio.saveAndFlush(d);

        var fila = consulta.adjuntos(postulacion);
        assertThat(fila).hasSize(1);
        assertThat(fila.get(0).codigo()).isEqualTo(d.getCodigo());
        assertThat(fila.get(0).nombreArchivo()).isEqualTo("nuevo.png");
        assertThat(fila.get(0).tamanoBytes()).isEqualTo(20L);
    }
}
