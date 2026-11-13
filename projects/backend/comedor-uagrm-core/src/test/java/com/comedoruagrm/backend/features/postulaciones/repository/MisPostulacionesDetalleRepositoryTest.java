package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

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

import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.CabeceraDetalle;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaHistorial;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.JdbcContadorDeConsultas;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Las lecturas del detalle de «Mis postulaciones» (CU04) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MisPostulacionesDetalleRepositoryTest {

    private static final String TITULO_FINALISTA = "Entrega de documentación física - Beca Comedor";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private ExpedienteFixtures fx;
    private JdbcContadorDeConsultas contador;
    private MisPostulacionesConsultaRepository consulta;
    private long yo;
    private long otro;
    private long admin;
    private long convocatoria;

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
        contador = new JdbcContadorDeConsultas(dataSource);
        consulta = new MisPostulacionesConsultaRepository(new NamedParameterJdbcTemplate(contador));
        yo = fx.postulante("9830001");
        otro = fx.postulante("9830002");
        admin = fx.usuario("9830003", "ADMINISTRADOR_DUBSS");
        convocatoria = fx.convocatoria("MPD");
    }

    private long postulacion(long usuario, String estado) {
        return fx.postulacion(convocatoria, usuario, estado);
    }

    /** Otra postulacion del mismo usuario, en una convocatoria propia (una por postulante y convocatoria). */
    private long otraPostulacion(long usuario, String estado, String etiqueta) {
        return fx.postulacion(fx.convocatoria(etiqueta), usuario, estado);
    }

    private UUID codigo(long postulacion) {
        return jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class, postulacion);
    }

    private void historial(long postulacion, String anterior, String nuevo, String motivo, String creadoEn) {
        jdbc.update("INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo, "
                + "motivo, creado_en) VALUES (?, ?, CAST(? AS estado_postulacion), CAST(? AS estado_postulacion), ?, "
                + "CAST(? AS timestamptz))", postulacion, admin, anterior, nuevo, motivo, creadoEn);
    }

    private void aviso(long usuario, long postulacion, String titulo, String mensaje, String creadoEn) {
        jdbc.update("INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo, postulacion_id, creado_en) "
                + "VALUES (?, ?, ?, CAST('INFO' AS tipo_notificacion), ?, CAST(? AS timestamptz))", usuario, titulo,
                mensaje, postulacion, creadoEn);
    }

    // ---- cabecera ----

    @Test
    void cabecera_deUnaPostulacionPropia_devuelveSusDatosYLasMarcas() {
        long p = postulacion(yo, "adjudicada");
        fx.adjudicacion(p, "ADJUDICADA", "80.00", admin);
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)", convocatoria,
                admin);

        CabeceraDetalle cabecera = consulta.cabecera(yo, codigo(p)).orElseThrow();

        assertThat(cabecera.id()).isEqualTo(p);
        assertThat(cabecera.fila().codigo()).isEqualTo(codigo(p));
        assertThat(cabecera.fila().estado()).isEqualTo("adjudicada");
        assertThat(cabecera.fila().convocatoriaNombre()).isEqualTo("Conv MPD");
        assertThat(cabecera.fila().gestionNombre()).isEqualTo("G-MPD");
        assertThat(cabecera.fila().resultadosPublicados()).isTrue();
        assertThat(cabecera.fila().decisionFinal()).isTrue();
    }

    @Test
    void cabecera_sinPublicarNiDecision_traeLasMarcasEnFalso() {
        long p = postulacion(yo, "no_adjudicada");

        var fila = consulta.cabecera(yo, codigo(p)).orElseThrow().fila();

        assertThat(fila.resultadosPublicados()).isFalse();
        assertThat(fila.decisionFinal()).isFalse();
    }

    @Test
    void cabecera_ajenaInexistenteBorradorOEliminada_devuelveVacio() {
        long ajena = postulacion(otro, "pendiente");
        long borrador = otraPostulacion(yo, "borrador", "MPDB");
        long eliminada = otraPostulacion(yo, "pendiente", "MPDE");
        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", eliminada);

        assertThat(consulta.cabecera(yo, codigo(ajena))).isEmpty();
        assertThat(consulta.cabecera(yo, codigo(borrador))).isEmpty();
        assertThat(consulta.cabecera(yo, codigo(eliminada))).isEmpty();
        assertThat(consulta.cabecera(yo, UUID.randomUUID())).isEmpty();
    }

    // ---- historial ----

    @Test
    void historial_vieneEnOrdenSinElAutor_yNoMezclaOtrasPostulaciones() {
        long p = postulacion(yo, "calificada");
        long q = postulacion(otro, "calificada");
        historial(p, "pendiente", "calificada", null, "2026-11-03T10:00:00Z");
        historial(p, "borrador", "pendiente", null, "2026-11-01T10:00:00Z");
        historial(q, "pendiente", "calificada", null, "2026-11-02T10:00:00Z");

        var filas = consulta.historial(p);

        assertThat(filas).extracting(FilaHistorial::estadoNuevo).containsExactly("pendiente", "calificada");
        assertThat(filas.get(0).fecha()).isBefore(filas.get(1).fecha());
        assertThat(FilaHistorial.class.getRecordComponents()).extracting("name")
                .containsExactly("estadoNuevo", "fecha", "motivo");
    }

    @Test
    void historial_traeElMotivoYNoCuentaLasFilasEliminadas() {
        long p = postulacion(yo, "devuelta");
        historial(p, "calificada", "devuelta", "Falta el CI", "2026-11-03T10:00:00Z");
        historial(p, "pendiente", "calificada", null, "2026-11-02T10:00:00Z");
        jdbc.update("UPDATE postulacion_historial SET eliminado_en = now() WHERE estado_nuevo = "
                + "CAST('calificada' AS estado_postulacion)");

        var filas = consulta.historial(p);

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).motivo()).isEqualTo("Falta el CI");
    }

    // ---- finalista, decision y aviso ----

    @Test
    void finalista_traePlazoYSiLaDocumentacionFueRecibida_yVacioSiNoEsFinalista() {
        long p = postulacion(yo, "calificada");
        long sinFinalista = postulacion(otro, "calificada");
        long finalista = fx.finalista(p, 1);
        jdbc.update("UPDATE finalista SET plazo_entrega = DATE '2026-11-20' WHERE id = ?", finalista);

        var antes = consulta.finalista(p).orElseThrow();
        fx.entregaRecibida(finalista);
        var despues = consulta.finalista(p).orElseThrow();

        assertThat(antes.plazoEntrega()).isEqualTo(LocalDate.parse("2026-11-20"));
        assertThat(antes.documentacionRecibida()).isFalse();
        assertThat(despues.documentacionRecibida()).isTrue();
        assertThat(consulta.finalista(sinFinalista)).isEmpty();
    }

    @Test
    void decisionFinal_devuelveElResultadoDeLaAdjudicacion_yVacioSinFila() {
        long adj = postulacion(yo, "adjudicada");
        long no = postulacion(otro, "no_adjudicada");
        fx.adjudicacion(adj, "ADJUDICADA", "80.00", admin);

        assertThat(consulta.decisionFinal(adj)).contains("ADJUDICADA");
        assertThat(consulta.decisionFinal(no)).isEmpty();
    }

    @Test
    void mensajeDelAvisoDeFinalista_esElUltimoDeEsaPostulacion_yIgnoraOtrosAvisos() {
        long p = postulacion(yo, "calificada");
        long q = postulacion(otro, "calificada");
        aviso(yo, p, TITULO_FINALISTA, "primero", "2026-11-10T10:00:00Z");
        aviso(yo, p, TITULO_FINALISTA, "ultimo", "2026-11-12T10:00:00Z");
        aviso(yo, p, "Tu postulación fue devuelta - Beca Comedor", "otro evento", "2026-11-13T10:00:00Z");
        aviso(otro, q, TITULO_FINALISTA, "de otra persona", "2026-11-14T10:00:00Z");

        assertThat(consulta.mensajeDelAvisoDeFinalista(p)).contains("ultimo");
        assertThat(consulta.mensajeDelAvisoDeFinalista(otraPostulacion(yo, "pendiente", "MPDS"))).isEmpty();
    }

    // ---- sin efectos secundarios y numero fijo de consultas ----

    @Test
    void lasLecturasNoModificanNada() {
        long p = postulacion(yo, "calificada");
        fx.finalista(p, 1);
        historial(p, "pendiente", "calificada", null, "2026-11-02T10:00:00Z");
        aviso(yo, p, TITULO_FINALISTA, "Documentos a presentar: CI.", "2026-11-10T10:00:00Z");
        String antes = instantanea();

        consulta.cabecera(yo, codigo(p));
        consulta.historial(p);
        consulta.finalista(p);
        consulta.decisionFinal(p);
        consulta.mensajeDelAvisoDeFinalista(p);
        consulta.listar(yo, null, false, org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(instantanea()).isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT leida FROM notificacion WHERE postulacion_id = ?", Boolean.class, p))
                .isFalse();
    }

    @Test
    void elDetalleHaceElMismoNumeroDeConsultasConPocoOMuchoHistorial() {
        long p = postulacion(yo, "calificada");
        for (int i = 0; i < 3; i++) {
            historial(p, "pendiente", "calificada", null, "2026-11-0" + (i + 1) + "T10:00:00Z");
        }
        int con3 = contarConsultasDelDetalle(p);
        for (int i = 3; i < 9; i++) {
            historial(p, "pendiente", "calificada", null, "2026-11-0" + (i + 1) + "T10:00:00Z");
        }

        assertThat(con3).isEqualTo(4);
        assertThat(contarConsultasDelDetalle(p)).isEqualTo(con3);
    }

    private int contarConsultasDelDetalle(long p) {
        contador.reiniciar();
        consulta.cabecera(yo, codigo(p));
        consulta.historial(p);
        consulta.finalista(p);
        consulta.decisionFinal(p);
        return contador.consultas();
    }

    private String instantanea() {
        return jdbc.queryForList("SELECT 'p' || id || estado::text || actualizado_en::text FROM postulacion "
                + "UNION ALL SELECT 'h' || id || actualizado_en::text FROM postulacion_historial "
                + "UNION ALL SELECT 'n' || id || leida::text FROM notificacion "
                + "UNION ALL SELECT 'b' || id FROM bitacora ORDER BY 1", String.class).toString();
    }
}
