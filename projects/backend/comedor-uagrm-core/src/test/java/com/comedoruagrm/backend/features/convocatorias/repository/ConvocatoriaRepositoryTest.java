package com.comedoruagrm.backend.features.convocatorias.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumento;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

import jakarta.persistence.EntityManager;

/**
 * Consultas de los repositorios del CU13 contra una base real. Cada test corre dentro de una transaccion que se
 * revierte, asi que no deja filas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConvocatoriaRepositoryTest {

    @Autowired
    private ConvocatoriaRepository convocatoriaRepository;

    @Autowired
    private GestionAcademicaRepository gestionRepository;

    @Autowired
    private TipoDocumentoRepository tipoRepository;

    @Autowired
    private ConvocatoriaDocumentoRepository documentoRepository;

    @Autowired
    private PostulacionRepository postulacionRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private ExpedienteFixtures fixtures;

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
        fixtures = new ExpedienteFixtures(jdbc);
    }

    private Convocatoria convocatoria(long id) {
        em.flush();
        em.clear();
        return convocatoriaRepository.findById(id).orElseThrow();
    }

    // ---- buscar ----

    @Test
    void buscar_filtraPorTextoEstadoYGestionYPagina() {
        long abierta = fixtures.convocatoria("CV1");
        fixtures.estadoConvocatoria(abierta, "ABIERTA");
        fixtures.convocatoria("CV2");
        jdbc.update("UPDATE convocatoria SET nombre = 'Beca Comedor' WHERE id = ?", abierta);
        em.clear();
        UUID gestionAbierta = convocatoria(abierta).getGestion().getCodigo();

        Page<Convocatoria> porTexto = convocatoriaRepository.buscar("%beca%", null, null, PageRequest.of(0, 10));
        Page<Convocatoria> porEstado = convocatoriaRepository.buscar(null, EstadoConvocatoria.ABIERTA, null,
                PageRequest.of(0, 10));
        Page<Convocatoria> porGestion = convocatoriaRepository.buscar(null, null, gestionAbierta,
                PageRequest.of(0, 10));
        Page<Convocatoria> todas = convocatoriaRepository.buscar(null, null, null, PageRequest.of(0, 1));

        assertThat(porTexto.getContent()).extracting(Convocatoria::getId).containsExactly(abierta);
        assertThat(porEstado.getContent()).extracting(Convocatoria::getId).contains(abierta);
        assertThat(porEstado.getContent()).allMatch(c -> c.getEstado() == EstadoConvocatoria.ABIERTA);
        assertThat(porGestion.getContent()).extracting(Convocatoria::getId).containsExactly(abierta);
        assertThat(todas.getContent()).hasSize(1);
        assertThat(todas.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void buscar_ordenaPorElCampoPedido() {
        fixtures.convocatoria("CV3");
        fixtures.convocatoria("CV4");

        Page<Convocatoria> pagina = convocatoriaRepository.buscar(null, null, null,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "nombre")));

        assertThat(pagina.getContent()).extracting(Convocatoria::getNombre).isSortedAccordingTo(
                java.util.Comparator.reverseOrder());
    }

    @Test
    void buscar_noTraeLasEliminadas() {
        long id = fixtures.convocatoria("CV5");
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", id);

        Page<Convocatoria> pagina = convocatoriaRepository.buscar("%cv5%", null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).isEmpty();
    }

    // ---- otra abierta en la gestion ----

    @Test
    void existsByGestionIdAndEstadoAndIdNot_distingueLaPropiaFila() {
        long abierta = fixtures.convocatoria("CV6");
        fixtures.estadoConvocatoria(abierta, "ABIERTA");
        Convocatoria c = convocatoria(abierta);
        Long gestionId = c.getGestion().getId();

        assertThat(convocatoriaRepository.existsByGestionIdAndEstadoAndIdNot(gestionId, EstadoConvocatoria.ABIERTA,
                abierta)).isFalse();
        assertThat(convocatoriaRepository.existsByGestionIdAndEstadoAndIdNot(gestionId, EstadoConvocatoria.ABIERTA,
                -1L)).isTrue();
    }

    // ---- vencidas ----

    @Test
    void findByEstadoAndFechaCierreLessThanEqual_soloTraeLasAbiertasVencidas() {
        long vencida = fixtures.convocatoria("CV7");
        long vigente = fixtures.convocatoria("CV8");
        long cerrada = fixtures.convocatoria("CV9");
        fixtures.estadoConvocatoria(vencida, "ABIERTA");
        fixtures.estadoConvocatoria(vigente, "ABIERTA");
        jdbc.update("UPDATE convocatoria SET fecha_cierre = now() - interval '1 hour' WHERE id IN (?, ?)",
                vencida, cerrada);
        jdbc.update("UPDATE convocatoria SET fecha_apertura = now() - interval '2 hours', "
                + "fecha_cierre = now() + interval '1 hour' WHERE id = ?", vigente);
        em.clear();

        List<Convocatoria> resultado = convocatoriaRepository.findByEstadoAndFechaCierreLessThanEqual(
                EstadoConvocatoria.ABIERTA, Instant.now());

        assertThat(resultado).extracting(Convocatoria::getId).contains(vencida)
                .doesNotContain(vigente, cerrada);
    }

    // ---- gestiones ----

    @Test
    void gestion_buscaPorEtiquetaYDetectaDuplicados() {
        GestionAcademica gestion = gestionRepository.saveAndFlush(new GestionAcademica("TEST-2030-II",
                java.time.LocalDate.of(2030, 7, 1), java.time.LocalDate.of(2030, 12, 20)));

        assertThat(gestionRepository.buscar("%2030-ii%", PageRequest.of(0, 10)).getContent())
                .extracting(GestionAcademica::getId).containsExactly(gestion.getId());
        assertThat(gestionRepository.existsByEtiqueta("TEST-2030-II")).isTrue();
        assertThat(gestionRepository.existsByEtiquetaAndIdNot("TEST-2030-II", gestion.getId())).isFalse();
        assertThat(gestionRepository.existsByEtiquetaAndIdNot("TEST-2030-II", -1L)).isTrue();
    }

    // ---- tipos de documento ----

    @Test
    void tipoDocumento_buscaPorNombreYPorCodigos() {
        fixtures.tipoDocumento("TEST Certificado raro");

        var tipos = tipoRepository.buscar("%certificado raro%", PageRequest.of(0, 10)).getContent();

        assertThat(tipos).hasSize(1);
        assertThat(tipoRepository.findByCodigoIn(List.of(tipos.get(0).getCodigo()))).hasSize(1);
        assertThat(tipoRepository.findByCodigo(tipos.get(0).getCodigo())).isPresent();
    }

    // ---- documentos requeridos: reactivar ----

    @Test
    void reactivar_unParEliminado_loVuelveAActivarYDevuelveUno() {
        long convocatoria = fixtures.convocatoria("CV10");
        long tipo = fixtures.tipoDocumento("TEST tipo reactivable");
        fixtures.exigeDocumento(convocatoria, tipo, false);
        jdbc.update("UPDATE convocatoria_documento SET eliminado_en = now() "
                + "WHERE convocatoria_id = ? AND tipo_documento_id = ?", convocatoria, tipo);
        em.clear();
        assertThat(documentoRepository.findByConvocatoriaId(convocatoria)).isEmpty();

        int afectadas = documentoRepository.reactivar(convocatoria, tipo, true);
        em.clear();

        assertThat(afectadas).isEqualTo(1);
        List<ConvocatoriaDocumento> documentos = documentoRepository.findByConvocatoriaId(convocatoria);
        assertThat(documentos).hasSize(1);
        assertThat(documentos.get(0).isObligatorio()).isTrue();
        assertThat(documentos.get(0).getTipoDocumento().getNombre()).isEqualTo("TEST tipo reactivable");
    }

    @Test
    void reactivar_unParActivoOQueNuncaExistio_devuelveCero() {
        long convocatoria = fixtures.convocatoria("CV11");
        long tipo = fixtures.tipoDocumento("TEST tipo activo");
        fixtures.exigeDocumento(convocatoria, tipo, true);
        long otroTipo = fixtures.tipoDocumento("TEST tipo nunca agregado");

        assertThat(documentoRepository.reactivar(convocatoria, tipo, true)).isZero();
        assertThat(documentoRepository.reactivar(convocatoria, otroTipo, true)).isZero();
    }

    @Test
    void findByConvocatoriaId_traeSoloLosVigentesOrdenadosPorNombre() {
        long convocatoria = fixtures.convocatoria("CV12");
        long b = fixtures.tipoDocumento("TEST B");
        long a = fixtures.tipoDocumento("TEST A");
        long quitado = fixtures.tipoDocumento("TEST C");
        fixtures.exigeDocumento(convocatoria, b, true);
        fixtures.exigeDocumento(convocatoria, a, false);
        fixtures.exigeDocumento(convocatoria, quitado, false);
        jdbc.update("UPDATE convocatoria_documento SET eliminado_en = now() "
                + "WHERE convocatoria_id = ? AND tipo_documento_id = ?", convocatoria, quitado);
        em.clear();

        assertThat(documentoRepository.findByConvocatoriaId(convocatoria))
                .extracting(d -> d.getTipoDocumento().getNombre()).containsExactly("TEST A", "TEST B");
    }

    // ---- postulaciones por convocatoria ----

    @Test
    void existsByConvocatoriaId_ignoraLasPostulacionesEliminadas() {
        long convocatoria = fixtures.convocatoria("CV13");
        long postulante = fixtures.postulante("9300001");
        long postulacion = fixtures.postulacion(convocatoria, postulante, "pendiente");
        em.clear();

        assertThat(postulacionRepository.existsByConvocatoriaId(convocatoria)).isTrue();

        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", postulacion);
        em.clear();

        assertThat(postulacionRepository.existsByConvocatoriaId(convocatoria)).isFalse();
    }
}
