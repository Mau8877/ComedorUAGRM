package com.comedoruagrm.backend.features.preseleccion.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas y escrituras del reprocesamiento de puntajes (CU26) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReprocesamientoConsultaRepository.class)
class ReprocesamientoConsultaRepositoryTest {

    @Autowired
    private ReprocesamientoConsultaRepository consulta;

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

    private long modelo(String version) {
        return jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (version, descripcion, activo) VALUES (?, 'prueba', false)
                RETURNING id""", Long.class, version);
    }

    private void puntaje(long postulacion, long modelo, boolean vigente) {
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, 50, now(), ?)""", modelo, postulacion, vigente);
    }

    /** Una postulacion con sus respuestas, en el estado dado. */
    private long postulacion(ExpedienteFixtures fx, long convocatoria, String ci, String estado) {
        long usuario = fx.postulante(ci);
        long postulacion = fx.postulacion(convocatoria, usuario, estado);
        fx.respuestaSocioeconomica(postulacion, "70.00");
        return postulacion;
    }

    private long convocatoria(ExpedienteFixtures fx, String etiqueta) {
        return fx.convocatoria(etiqueta);
    }

    @Test
    void convocatoria_devuelveSusDatosPorCodigoYVacioSiNoExiste() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long id = convocatoria(fx, "RP1");
        UUID codigo = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                id);

        var datos = consulta.convocatoria(codigo).orElseThrow();

        assertThat(datos.id()).isEqualTo(id);
        assertThat(datos.nombre()).isEqualTo("Conv RP1");
        assertThat(consulta.convocatoria(UUID.randomUUID())).isEmpty();
    }

    @Test
    void resultadosPublicados_soloSiHayUnaPublicacion() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long id = convocatoria(fx, "RP2");
        long admin = fx.usuario("9610001", "ADMINISTRADOR_DUBSS");
        assertThat(consulta.resultadosPublicados(id)).isFalse();

        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)", id, admin);

        assertThat(consulta.resultadosPublicados(id)).isTrue();
    }

    @Test
    void calificadasYPorReprocesar_cuentanSoloLasCalificadasSinPuntajeVigenteDeLaVersion() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = convocatoria(fx, "RP3");
        long m13 = modelo("v9.13");
        long m14 = modelo("v9.14");
        long yaConLaVersion = postulacion(fx, conv, "9610010", "calificada");
        long conOtraVersion = postulacion(fx, conv, "9610011", "calificada");
        long sinPuntaje = postulacion(fx, conv, "9610012", "calificada");
        long soloHistorial = postulacion(fx, conv, "9610013", "calificada");
        postulacion(fx, conv, "9610014", "pendiente");
        postulacion(fx, conv, "9610015", "devuelta");
        puntaje(yaConLaVersion, m14, true);
        puntaje(conOtraVersion, m13, true);
        puntaje(soloHistorial, m14, false); // el de la version vigente es historial: falta recalcular

        assertThat(consulta.calificadas(conv)).isEqualTo(4);
        assertThat(consulta.porReprocesar(conv, m14)).containsExactlyInAnyOrder(conOtraVersion, sinPuntaje,
                soloHistorial);
    }

    @Test
    void porReprocesar_noMezclaLasPostulacionesDeOtraConvocatoria() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convA = convocatoria(fx, "RP4A");
        long convB = convocatoria(fx, "RP4B");
        long m = modelo("v9.15");
        long deA = postulacion(fx, convA, "9610020", "calificada");
        postulacion(fx, convB, "9610021", "calificada");

        assertThat(consulta.porReprocesar(convA, m)).containsExactly(deA);
    }

    @Test
    void lote_seCreaPendienteAvanzaYTermina() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = convocatoria(fx, "RP5");
        long admin = fx.usuario("9610030", "ADMINISTRADOR_DUBSS");
        long m = modelo("v9.16");

        var lote = consulta.crearLote(conv, m, admin, 10);
        assertThat(lote.estado()).isEqualTo("PENDIENTE");
        assertThat(lote.total()).isEqualTo(10);
        assertThat(lote.convocatoriaId()).isEqualTo(conv);
        assertThat(lote.modeloVersion()).isEqualTo("v9.16");
        assertThat(consulta.loteEnCurso(conv)).isPresent();

        consulta.marcarEnProceso(lote.id(), 8);
        consulta.sumarAvance(lote.id(), false);
        consulta.sumarAvance(lote.id(), true);
        var avanzado = consulta.lote(lote.codigo()).orElseThrow();
        assertThat(avanzado.estado()).isEqualTo("EN_PROCESO");
        assertThat(avanzado.total()).isEqualTo(8);
        assertThat(avanzado.procesadas()).isEqualTo(2);
        assertThat(avanzado.fallidas()).isEqualTo(1);
        assertThat(avanzado.iniciadoEn()).isNotNull();

        consulta.terminar(lote.id(), "COMPLETADO");
        var terminado = consulta.loteDe(lote.id()).orElseThrow();
        assertThat(terminado.estado()).isEqualTo("COMPLETADO");
        assertThat(terminado.terminadoEn()).isNotNull();
        assertThat(consulta.loteEnCurso(conv)).isEmpty();
        // terminado el primero, se puede crear otro
        assertThat(consulta.crearLote(conv, m, admin, 2).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void marcarHuerfanos_cierraComoFallidosLosLotesEnCurso() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = convocatoria(fx, "RP6");
        long admin = fx.usuario("9610040", "ADMINISTRADOR_DUBSS");
        var lote = consulta.crearLote(conv, modelo("v9.17"), admin, 3);

        assertThat(consulta.marcarHuerfanos()).isGreaterThanOrEqualTo(1);

        assertThat(consulta.loteDe(lote.id()).orElseThrow().estado()).isEqualTo("FALLIDO");
        assertThat(consulta.loteEnCurso(conv)).isEmpty();
    }

    @Test
    void crearLote_conOtroEnCursoParaLaMismaConvocatoria_violaElIndiceUnico() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = convocatoria(fx, "RP7");
        long admin = fx.usuario("9610050", "ADMINISTRADOR_DUBSS");
        long m = modelo("v9.18");
        consulta.crearLote(conv, m, admin, 3);

        assertThatThrownBy(() -> consulta.crearLote(conv, m, admin, 3))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
