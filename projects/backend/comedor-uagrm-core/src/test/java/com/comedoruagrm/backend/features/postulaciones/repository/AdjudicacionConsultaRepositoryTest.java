package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CabeceraFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CandidataFinalista;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas de la adjudicacion (CU05) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    AdjudicacionConsultaRepository.class,
    CierrePostulacionConsultaRepository.class,
    ListaPriorizadaRepository.class
})
class AdjudicacionConsultaRepositoryTest {

    @Autowired
    private AdjudicacionConsultaRepository consulta;

    @Autowired
    private ListaPriorizadaRepository listaPriorizada;

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

    private UUID codigoConvocatoria(long id) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class, id);
    }

    private UUID codigoFinalista(long id) {
        return jdbc.queryForObject("SELECT codigo_finalista FROM finalista WHERE id = ?", UUID.class, id);
    }

    private UUID codigoPostulacion(long id) {
        return jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class, id);
    }

    /** Una calificada con puntaje de la IA vigente. */
    private long calificada(ExpedienteFixtures fx, long convocatoria, String ci, String puntaje) {
        long id = fx.postulacion(convocatoria, fx.postulante(ci), "calificada");
        fx.puntajeIa(id, puntaje, "{}");
        return id;
    }

    @Test
    void convocatoria_devuelveSusDatosYLaCantidadDeFinalistas() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoriaConCupos("ACR1", 10, "50.50", 12);

        var c = consulta.convocatoria(codigoConvocatoria(conv)).orElseThrow();

        assertThat(c.id()).isEqualTo(conv);
        assertThat(c.nombre()).isEqualTo("Conv ACR1");
        assertThat(c.estado()).isEqualTo("CERRADA");
        assertThat(c.cupos()).isEqualTo(10);
        assertThat(c.puntajeMinimo()).isEqualByComparingTo("50.50");
        assertThat(c.cantidadFinalistas()).isEqualTo(12);
    }

    @Test
    void convocatoria_sinCuposNiMinimo_losDevuelveNulos() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR2");

        var c = consulta.convocatoria(codigoConvocatoria(conv)).orElseThrow();

        assertThat(c.cupos()).isNull();
        assertThat(c.puntajeMinimo()).isNull();
        assertThat(c.cantidadFinalistas()).isNull();
    }

    @Test
    void convocatoria_inexistenteOEliminada_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR3");
        UUID codigo = codigoConvocatoria(conv);
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", conv);

        assertThat(consulta.convocatoria(codigo)).isEmpty();
        assertThat(consulta.convocatoria(UUID.randomUUID())).isEmpty();
    }

    @Test
    void cabeceraFinalista_devuelveLaCabeceraConElPuntajeVigenteDeLaIa() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoriaConCupos("ACR4", 5, "40", null);
        long postulacion = calificada(fx, conv, "9520001", "55.25");
        long finalista = fx.finalista(postulacion, 3);

        CabeceraFinalista c = consulta.cabeceraFinalista(codigoFinalista(finalista)).orElseThrow();

        assertThat(c.finalistaId()).isEqualTo(finalista);
        assertThat(c.finalistaCodigo()).isEqualTo(codigoFinalista(finalista));
        assertThat(c.postulacionId()).isEqualTo(postulacion);
        assertThat(c.postulacionCodigo()).isEqualTo(codigoPostulacion(postulacion));
        assertThat(c.convocatoriaId()).isEqualTo(conv);
        assertThat(c.convocatoriaCodigo()).isEqualTo(codigoConvocatoria(conv));
        assertThat(c.estado()).isEqualTo("calificada");
        assertThat(c.puntajeIa()).isEqualByComparingTo("55.25");
    }

    @Test
    void cabeceraFinalista_sinPuntajeDeLaIa_traePuntajeNulo() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR5");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9520002"), "calificada"), 1);

        assertThat(consulta.cabeceraFinalista(codigoFinalista(finalista)).orElseThrow().puntajeIa()).isNull();
    }

    @Test
    void cabeceraFinalista_inexistenteOEliminado_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR6");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9520003"), "calificada"), 1);
        UUID codigo = codigoFinalista(finalista);
        jdbc.update("UPDATE finalista SET eliminado_en = now() WHERE id = ?", finalista);

        assertThat(consulta.cabeceraFinalista(codigo)).isEmpty();
        assertThat(consulta.cabeceraFinalista(UUID.randomUUID())).isEmpty();
    }

    @Test
    void ultimaAuditoria_conVariasAuditorias_devuelveLaMasReciente() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR7");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9520004"), "calificada"), 1);
        long auditor = fx.usuario("9520005", "ADMINISTRADOR_DUBSS");
        fx.auditoria(finalista, auditor, "OBSERVADO", "Falta un documento", null, "2026-11-01T10:00:00Z");
        fx.auditoria(finalista, auditor, "VALIDADO", null, "21.5", "2026-11-03T10:00:00Z");
        fx.auditoria(finalista, auditor, "NO_VALIDO", "Antigua", null, "2026-10-01T10:00:00Z");

        var ultima = consulta.ultimaAuditoria(finalista).orElseThrow();

        assertThat(ultima.resultado()).isEqualTo("VALIDADO");
        assertThat(ultima.puntajeAcademico()).isEqualByComparingTo("21.5");
    }

    @Test
    void ultimaAuditoria_sinAuditorias_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR8");
        long finalista = fx.finalista(fx.postulacion(conv, fx.postulante("9520006"), "calificada"), 1);

        assertThat(consulta.ultimaAuditoria(finalista)).isEmpty();
    }

    @Test
    void cuentas_adjudicadasNoAdjudicadasFinalistasYCalificadasSinFinalista() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoriaConCupos("ACR9", 3, "50", null);
        long admin = fx.usuario("9520007", "ADMINISTRADOR_DUBSS");
        long adjudicada = fx.postulacion(conv, fx.postulante("9520008"), "adjudicada");
        long noAdjudicada = fx.postulacion(conv, fx.postulante("9520009"), "no_adjudicada");
        fx.adjudicacion(adjudicada, "ADJUDICADA", "80", admin);
        fx.adjudicacion(noAdjudicada, "NO_ADJUDICADA", null, admin);
        // Rechazada por el CU20: no_adjudicada pero SIN fila de adjudicacion; no cuenta.
        fx.postulacion(conv, fx.postulante("9520010"), "no_adjudicada");
        long finalista = calificada(fx, conv, "9520011", "70");
        fx.finalista(finalista, 1);
        calificada(fx, conv, "9520012", "60");
        calificada(fx, conv, "9520013", "65");

        assertThat(consulta.contarAdjudicadas(conv)).isEqualTo(1);
        assertThat(consulta.contarNoAdjudicadas(conv)).isEqualTo(1);
        assertThat(consulta.contarFinalistas(conv)).isEqualTo(1);
        assertThat(consulta.contarCalificadasSinFinalista(conv)).isEqualTo(2);
    }

    @Test
    void cuentas_noContabilizanDecisionesEliminadasNiDeOtraConvocatoria() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoriaConCupos("ACR10", 3, "50", null);
        long otra = fx.convocatoriaConCupos("ACR11", 3, "50", null);
        long admin = fx.usuario("9520014", "ADMINISTRADOR_DUBSS");
        long eliminada = fx.postulacion(conv, fx.postulante("9520015"), "adjudicada");
        long deOtra = fx.postulacion(otra, fx.postulante("9520016"), "adjudicada");
        fx.adjudicacion(eliminada, "ADJUDICADA", "80", admin);
        fx.adjudicacion(deOtra, "ADJUDICADA", "80", admin);
        jdbc.update("UPDATE adjudicacion SET eliminado_en = now() WHERE postulacion_id = ?", eliminada);

        assertThat(consulta.contarAdjudicadas(conv)).isZero();
        assertThat(consulta.contarAdjudicadas(otra)).isEqualTo(1);
    }

    @Test
    void siguientesSinFinalista_entregaLaPosicionDeLaListaCompletaYDejaFueraALosFinalistas() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR12");
        long p1 = calificada(fx, conv, "9520020", "60");
        long p2 = calificada(fx, conv, "9520021", "50");
        long p3 = calificada(fx, conv, "9520022", "40");
        long p4 = calificada(fx, conv, "9520023", "30");
        // La primera ya es finalista (posicion 1 de la lista completa): no se repite, pero ocupa su lugar.
        fx.finalista(p1, 1);

        List<CandidataFinalista> siguientes = consulta.siguientesSinFinalista(conv, 2);

        assertThat(siguientes).extracting(CandidataFinalista::postulacionId).containsExactly(p2, p3);
        assertThat(siguientes).extracting(CandidataFinalista::posicion).containsExactly(2L, 3L);
        assertThat(siguientes.get(0).postulacionCodigo()).isEqualTo(codigoPostulacion(p2));
        assertThat(consulta.siguientesSinFinalista(conv, 10)).hasSize(3).last()
                .extracting(CandidataFinalista::postulacionId).isEqualTo(p4);
    }

    @Test
    void siguientesSinFinalista_elOrdenCoincideConLaListaPriorizadaDelCu06() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR13");
        long a = calificada(fx, conv, "9520030", "55");
        long b = calificada(fx, conv, "9520031", "55");
        long c = calificada(fx, conv, "9520032", "55");
        long d = calificada(fx, conv, "9520033", "70");
        fx.fechaEnvio(a, "2026-08-05T10:00:00Z");
        fx.fechaEnvio(b, "2026-08-01T10:00:00Z");
        // `c` queda sin fecha de envio: va al final de su empate.

        List<PostulacionPriorizadaResponse> cu06 = listaPriorizada.buscar(codigoConvocatoria(conv), null, null,
                100, 0);
        List<CandidataFinalista> propia = consulta.siguientesSinFinalista(conv, 100);

        assertThat(propia).extracting(CandidataFinalista::postulacionCodigo)
                .containsExactlyElementsOf(cu06.stream().map(PostulacionPriorizadaResponse::codigo).toList());
        assertThat(propia).extracting(CandidataFinalista::posicion)
                .containsExactlyElementsOf(cu06.stream().map(PostulacionPriorizadaResponse::posicion).toList());
        assertThat(propia).extracting(CandidataFinalista::postulacionId).containsExactly(d, b, a, c);
    }

    @Test
    void siguientesSinFinalista_ignoraPostulacionesQueNoEstanCalificadasOSinPuntaje() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR14");
        long valida = calificada(fx, conv, "9520040", "50");
        long devuelta = fx.postulacion(conv, fx.postulante("9520041"), "devuelta");
        fx.puntajeIa(devuelta, "65", "{}");
        fx.postulacion(conv, fx.postulante("9520042"), "calificada");

        assertThat(consulta.siguientesSinFinalista(conv, 10)).extracting(CandidataFinalista::postulacionId)
                .containsExactly(valida);
        assertThat(consulta.contarCalificadasSinFinalista(conv)).isEqualTo(1);
    }

    @Test
    void losDosBloqueos_noFallanYElEstadoSeLeeTrasBloquear() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("ACR15");
        long postulacion = fx.postulacion(conv, fx.postulante("9520050"), "calificada");

        consulta.bloquearConvocatoria(conv);
        consulta.bloquearPostulacion(postulacion);

        assertThat(consulta.estadoActual(postulacion)).isEqualTo("calificada");
    }

    @Test
    void resultadosPublicados_enUnaConvocatoriaSinPublicar_esFalso() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);

        assertThat(consulta.resultadosPublicados(fx.convocatoria("ACR16"))).isFalse();
    }
}
