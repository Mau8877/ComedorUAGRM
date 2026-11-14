package com.comedoruagrm.backend.features.resultados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;
import com.comedoruagrm.backend.features.comunicados.repository.ComunicadoConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.CierrePostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DevolucionConsultaRepository;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * La marca de publicacion del CU22 (la fila de `publicacion_resultado`) vista desde las consultas del CU05, el
 * CU19 y el CU20, y los destinatarios `ADJUDICADOS` del CU21, con Postgres real.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MarcaPublicacionRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    private ExpedienteFixtures fx;
    private CierrePostulacionConsultaRepository cierre;
    private AdjudicacionConsultaRepository adjudicacion;
    private DevolucionConsultaRepository devolucion;
    private ComunicadoConsultaRepository comunicados;
    private long convocatoria;
    private long admin;
    private long postulacion;

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
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbc);
        cierre = new CierrePostulacionConsultaRepository(named);
        adjudicacion = new AdjudicacionConsultaRepository(named, cierre);
        devolucion = new DevolucionConsultaRepository(named);
        comunicados = new ComunicadoConsultaRepository(named);
        convocatoria = fx.convocatoria("MARCA");
        admin = fx.usuario("9900001", "ADMINISTRADOR_DUBSS");
        postulacion = fx.postulacion(convocatoria, fx.postulante("9900002"), "calificada");
    }

    private void publicar() {
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)",
                convocatoria, admin);
    }

    @Test
    void sinLaFila_ningunaConsultaVeLaConvocatoriaComoPublicada() {
        assertThat(cierre.resultadosPublicados(convocatoria)).isFalse();
        assertThat(adjudicacion.resultadosPublicados(convocatoria)).isFalse();
        assertThat(devolucion.resultadosPublicadosDe(postulacion)).isFalse();
    }

    @Test
    void conLaFila_lasTresConsultasLaVenComoPublicada() {
        publicar();

        assertThat(cierre.resultadosPublicados(convocatoria)).isTrue();
        assertThat(adjudicacion.resultadosPublicados(convocatoria)).isTrue();
        assertThat(devolucion.resultadosPublicadosDe(postulacion)).isTrue();
    }

    @Test
    void laPublicacionDeUnaConvocatoria_noMarcaALasOtras() {
        long otra = fx.convocatoria("MARCA2");
        long otraPostulacion = fx.postulacion(otra, fx.postulante("9900003"), "calificada");
        publicar();

        assertThat(cierre.resultadosPublicados(otra)).isFalse();
        assertThat(devolucion.resultadosPublicadosDe(otraPostulacion)).isFalse();
    }

    @Test
    void unaPublicacionEliminadaLogicamente_noCuenta() {
        publicar();
        jdbc.update("UPDATE publicacion_resultado SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);

        assertThat(cierre.resultadosPublicados(convocatoria)).isFalse();
        assertThat(devolucion.resultadosPublicadosDe(postulacion)).isFalse();
    }

    @Test
    void destinatariosAdjudicados_soloLosFinalistasConAdjudicacionADJUDICADA_deEsaConvocatoria() {
        long adjudicado = fx.postulante("9900010");
        long noAdjudicado = fx.postulante("9900011");
        long calificado = fx.postulante("9900012");
        long archivado = fx.postulante("9900013");
        long rechazado = fx.postulante("9900014");
        long deOtra = fx.postulante("9900015");
        decidido(adjudicado, "adjudicada", "ADJUDICADA", convocatoria);
        decidido(noAdjudicado, "no_adjudicada", "NO_ADJUDICADA", convocatoria);
        decidido(calificado, "calificada", null, convocatoria);
        decidido(archivado, "archivada", null, convocatoria);
        decidido(rechazado, "no_adjudicada", null, convocatoria);
        decidido(deOtra, "adjudicada", "ADJUDICADA", fx.convocatoria("MARCA3"));

        List<Long> destinatarios = comunicados.destinatarios(DestinatariosComunicado.ADJUDICADOS, convocatoria);

        assertThat(destinatarios).containsExactly(adjudicado);
    }

    @Test
    void destinatariosAdjudicados_funcionanAntesYDespuesDePublicar() {
        long adjudicado = fx.postulante("9900020");
        decidido(adjudicado, "adjudicada", "ADJUDICADA", convocatoria);

        assertThat(comunicados.destinatarios(DestinatariosComunicado.ADJUDICADOS, convocatoria))
                .containsExactly(adjudicado);
        publicar();
        assertThat(comunicados.destinatarios(DestinatariosComunicado.ADJUDICADOS, convocatoria))
                .containsExactly(adjudicado);
    }

    @Test
    void destinatariosAdjudicados_unaCuentaDesactivada_noRecibe() {
        long adjudicado = fx.postulante("9900030");
        decidido(adjudicado, "adjudicada", "ADJUDICADA", convocatoria);
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", adjudicado);

        assertThat(comunicados.destinatarios(DestinatariosComunicado.ADJUDICADOS, convocatoria)).isEmpty();
    }

    /** Un finalista del postulante con su postulacion en ese estado y, si `resultado` no es nulo, su adjudicacion. */
    private void decidido(long postulanteUsuario, String estado, String resultado, long convocatoriaId) {
        long po = fx.postulacion(convocatoriaId, postulanteUsuario, estado);
        fx.finalista(po, 1);
        if (resultado != null) {
            fx.adjudicacion(po, resultado, "80", admin);
        }
    }
}
