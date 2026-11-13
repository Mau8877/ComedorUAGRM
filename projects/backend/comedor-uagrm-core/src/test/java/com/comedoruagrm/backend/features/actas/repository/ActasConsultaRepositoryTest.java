package com.comedoruagrm.backend.features.actas.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaPendienteActaResponse;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.Adjudicado;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.JdbcContadorDeConsultas;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas del acta de adjudicacion (CU08) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActasConsultaRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private ExpedienteFixtures fx;
    private JdbcContadorDeConsultas contador;
    private ActasConsultaRepository consulta;
    private long convocatoria;
    private long admin;
    private int siguienteCi;

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
        consulta = new ActasConsultaRepository(new NamedParameterJdbcTemplate(contador));
        convocatoria = fx.convocatoria("ACONS");
        admin = fx.usuario("9720001", "ADMINISTRADOR_DUBSS");
        siguienteCi = 9730000;
    }

    /** Un finalista con la posicion dada; con `resultado` no nulo, ademas su adjudicacion. */
    private long finalista(long convocatoriaId, String nombres, String apellidos, int posicion, String resultado,
            String puntaje) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        long postulacion = fx.postulacion(convocatoriaId, usuario, "calificada");
        fx.finalista(postulacion, posicion);
        if (resultado != null) {
            fx.adjudicacion(postulacion, resultado, puntaje, admin);
        }
        return postulacion;
    }

    private UUID codigo(long convocatoriaId) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoriaId);
    }

    private void publicar(long convocatoriaId, String instante) {
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en) "
                + "VALUES (?, ?, CAST(? AS timestamptz))", convocatoriaId, admin, instante);
    }

    private long acta(long convocatoriaId, String numero, String generadaEn) {
        return jdbc.queryForObject("INSERT INTO acta_adjudicacion (convocatoria_id, numero, storage_path, "
                + "generada_por, generada_en) VALUES (?, ?, 'actas/x.pdf', ?, CAST(? AS timestamptz)) RETURNING id",
                Long.class, convocatoriaId, numero, admin, generadaEn);
    }

    private int consultas(Runnable operacion) {
        contador.reiniciar();
        operacion.run();
        return contador.consultas();
    }

    private static PageRequest sinOrden() {
        return PageRequest.of(0, 20);
    }

    private static PageRequest ordenadoPor(String campo, Sort.Direction sentido) {
        return PageRequest.of(0, 20, Sort.by(sentido, campo));
    }

    // ---- convocatoria, bloqueo, publicacion ----

    @Test
    void convocatoria_existente_devuelveSusDatosYUnaInexistenteVacio() {
        jdbc.update("UPDATE convocatoria SET cupos = 50, puntaje_minimo = 60 WHERE id = ?", convocatoria);

        var datos = consulta.convocatoria(codigo(convocatoria)).orElseThrow();

        assertThat(datos.id()).isEqualTo(convocatoria);
        assertThat(datos.nombre()).isEqualTo("Conv ACONS");
        assertThat(datos.estado()).isEqualTo("CERRADA");
        assertThat(datos.gestion()).isEqualTo("G-ACONS");
        assertThat(datos.cupos()).isEqualTo(50);
        assertThat(datos.puntajeMinimo()).isEqualByComparingTo("60");
        assertThat(datos.fechaApertura()).isBefore(datos.fechaCierre());
        assertThat(consulta.convocatoria(UUID.randomUUID())).isEmpty();
    }

    @Test
    void convocatoria_sinCuposNiMinimo_losDevuelveNulos() {
        var datos = consulta.convocatoria(codigo(convocatoria)).orElseThrow();

        assertThat(datos.cupos()).isNull();
        assertThat(datos.puntajeMinimo()).isNull();
    }

    @Test
    void convocatoria_eliminada_noSeEncuentra() {
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", convocatoria);

        assertThat(consulta.convocatoria(codigo(convocatoria))).isEmpty();
    }

    @Test
    void bloquearConvocatoria_conUnaExistente_noFalla() {
        consulta.bloquearConvocatoria(convocatoria);
    }

    @Test
    void publicacion_conFila_devuelveLaFechaYSinFilaOSiEstaEliminadaVacio() {
        assertThat(consulta.publicacion(convocatoria)).isEmpty();

        publicar(convocatoria, "2026-11-12T15:00:00Z");
        assertThat(consulta.publicacion(convocatoria)).contains(Instant.parse("2026-11-12T15:00:00Z"));

        jdbc.update("UPDATE publicacion_resultado SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);
        assertThat(consulta.publicacion(convocatoria)).isEmpty();
    }

    // ---- adjudicados ----

    @Test
    void adjudicadosDelActa_traeSoloAdjudicadosDeMayorAMenorPuntajeYLuegoPorPosicion() {
        finalista(convocatoria, "Ana", "Uno", 4, "ADJUDICADA", "80.00");
        finalista(convocatoria, "Beto", "Dos", 2, "ADJUDICADA", "90.50");
        finalista(convocatoria, "Cris", "Tres", 3, "ADJUDICADA", "90.50");
        finalista(convocatoria, "Dani", "Cuatro", 1, "NO_ADJUDICADA", "99.00");
        finalista(convocatoria, "Eva", "Cinco", 5, null, null);
        finalista(convocatoria, "Fede", "Seis", 6, "ADJUDICADA", null);

        List<Adjudicado> filas = consulta.adjudicadosDelActa(convocatoria);

        assertThat(filas).extracting(Adjudicado::nombres).containsExactly("Beto", "Cris", "Ana", "Fede");
        assertThat(filas.get(0).apellidos()).isEqualTo("Dos");
        assertThat(filas.get(0).ci()).startsWith("973");
        assertThat(filas.get(0).carrera()).isNotBlank();
        assertThat(filas.get(0).puntajeFinal()).isEqualByComparingTo(new BigDecimal("90.50"));
        assertThat(filas.get(3).puntajeFinal()).isNull();
        assertThat(consulta.contarAdjudicados(convocatoria)).isEqualTo(4);
    }

    @Test
    void adjudicadosDelActa_noMezclaConvocatoriasNiCuentaLoEliminado() {
        long otra = fx.convocatoria("ACONS2");
        finalista(convocatoria, "Ana", "Uno", 1, "ADJUDICADA", "80.00");
        finalista(otra, "Otra", "Convocatoria", 1, "ADJUDICADA", "99.00");
        long eliminada = finalista(convocatoria, "Gone", "Postulacion", 2, "ADJUDICADA", "70.00");
        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", eliminada);
        long sinAdjudicacion = finalista(convocatoria, "Gone", "Adjudicacion", 3, "ADJUDICADA", "60.00");
        jdbc.update("UPDATE adjudicacion SET eliminado_en = now() WHERE postulacion_id = ?", sinAdjudicacion);

        assertThat(consulta.adjudicadosDelActa(convocatoria)).extracting(Adjudicado::nombres).containsExactly("Ana");
        assertThat(consulta.contarAdjudicados(convocatoria)).isEqualTo(1);
        assertThat(consulta.contarAdjudicados(otra)).isEqualTo(1);
    }

    @Test
    void adjudicadosDelActa_haceUnNumeroFijoDeConsultasSinImportarCuantasFilas() {
        for (int i = 1; i <= 15; i++) {
            finalista(convocatoria, "N" + i, "A" + i, i, "ADJUDICADA", "80.00");
        }
        int con15 = consultas(() -> consulta.adjudicadosDelActa(convocatoria));
        for (int i = 16; i <= 30; i++) {
            finalista(convocatoria, "N" + i, "A" + i, i, "ADJUDICADA", "80.00");
        }

        int con30 = consultas(() -> consulta.adjudicadosDelActa(convocatoria));

        assertThat(con15).isEqualTo(1);
        assertThat(con30).isEqualTo(con15);
    }

    // ---- acta de una convocatoria ----

    @Test
    void acta_conActa_traeSusDatosConElNombreDeQuienLaGeneroYLosAdjudicados() {
        finalista(convocatoria, "Ana", "Uno", 1, "ADJUDICADA", "80.00");
        finalista(convocatoria, "Beto", "Dos", 2, "NO_ADJUDICADA", "70.00");
        acta(convocatoria, "001/2026", "2026-11-13T15:00:00Z");

        ActaResponse acta = consulta.acta(convocatoria).orElseThrow();

        assertThat(acta.numero()).isEqualTo("001/2026");
        assertThat(acta.convocatoria().codigo()).isEqualTo(codigo(convocatoria));
        assertThat(acta.convocatoria().nombre()).isEqualTo("Conv ACONS");
        assertThat(acta.gestion()).isEqualTo("G-ACONS");
        assertThat(acta.adjudicados()).isEqualTo(1);
        assertThat(acta.generadaPor()).isEqualTo("Ana Rojas");
        assertThat(acta.generadaEn()).isEqualTo(Instant.parse("2026-11-13T15:00:00Z"));
        assertThat(acta.codigo()).isNotNull();
    }

    @Test
    void acta_sinActaOEliminada_devuelveVacio() {
        assertThat(consulta.acta(convocatoria)).isEmpty();

        acta(convocatoria, "001/2026", "2026-11-13T15:00:00Z");
        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);
        assertThat(consulta.acta(convocatoria)).isEmpty();
    }

    // ---- listado de actas ----

    private void tresActas() {
        long b = fx.convocatoria("BETA");
        long c = fx.convocatoria("GAMMA");
        acta(convocatoria, "002/2026", "2026-11-10T10:00:00Z");
        acta(b, "001/2026", "2026-11-11T10:00:00Z");
        acta(c, "003/2026", "2026-11-12T10:00:00Z");
    }

    @Test
    void actas_pordefectoLaMasRecientePrimero_yPaginaConElTotal() {
        tresActas();

        Page<ActaResponse> primera = consulta.actas(null, PageRequest.of(0, 2));
        Page<ActaResponse> segunda = consulta.actas(null, PageRequest.of(1, 2));

        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.getTotalPages()).isEqualTo(2);
        assertThat(primera.getContent()).extracting(ActaResponse::numero).containsExactly("003/2026", "001/2026");
        assertThat(segunda.getContent()).extracting(ActaResponse::numero).containsExactly("002/2026");
    }

    @Test
    void actas_ordenaPorNumeroOPorFechaEnAmbosSentidos() {
        tresActas();

        assertThat(consulta.actas(null, ordenadoPor("numero", Sort.Direction.ASC)).getContent())
                .extracting(ActaResponse::numero).containsExactly("001/2026", "002/2026", "003/2026");
        assertThat(consulta.actas(null, ordenadoPor("numero", Sort.Direction.DESC)).getContent())
                .extracting(ActaResponse::numero).containsExactly("003/2026", "002/2026", "001/2026");
        assertThat(consulta.actas(null, ordenadoPor("generadaEn", Sort.Direction.ASC)).getContent())
                .extracting(ActaResponse::numero).containsExactly("002/2026", "001/2026", "003/2026");
    }

    @Test
    void actas_buscaPorNumeroOPorNombreDeLaConvocatoria() {
        tresActas();

        assertThat(consulta.actas("002/", sinOrden()).getContent()).extracting(ActaResponse::numero)
                .containsExactly("002/2026");
        assertThat(consulta.actas("gamma", sinOrden()).getContent()).extracting(ActaResponse::numero)
                .containsExactly("003/2026");
        assertThat(consulta.actas("no-existe", sinOrden()).getContent()).isEmpty();
        assertThat(consulta.actas("   ", sinOrden()).getTotalElements()).isEqualTo(3);
        // Un `%` del cliente se busca como texto, no como comodin.
        assertThat(consulta.actas("%", sinOrden()).getContent()).isEmpty();
    }

    @Test
    void actas_conUnSortNoSoportado_lanzaErrSys05() {
        tresActas();

        assertThatThrownBy(() -> consulta.actas(null, ordenadoPor("storagePath", Sort.Direction.ASC)))
                .isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo("ERR_SYS_05");
        assertThatThrownBy(() -> consulta.actas(null, PageRequest.of(0, 20, Sort.by("numero").and(Sort.by("generadaEn")))))
                .isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo("ERR_SYS_05");
    }

    @Test
    void actas_noCuentanLasEliminadasNiLasDeUnaConvocatoriaEliminada() {
        tresActas();
        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE numero = '001/2026'");
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE nombre = 'Conv GAMMA'");

        assertThat(consulta.actas(null, sinOrden()).getContent()).extracting(ActaResponse::numero)
                .containsExactly("002/2026");
    }

    @Test
    void actas_haceDosConsultasSinImportarCuantasActasHaya() {
        tresActas();
        int con3 = consultas(() -> consulta.actas(null, sinOrden()));
        for (int i = 0; i < 5; i++) {
            acta(fx.convocatoria("X" + i), "01" + i + "/2026", "2026-11-13T10:00:00Z");
        }

        int con8 = consultas(() -> consulta.actas(null, sinOrden()));

        assertThat(con3).isEqualTo(2);
        assertThat(con8).isEqualTo(con3);
    }

    // ---- convocatorias pendientes ----

    @Test
    void pendientes_soloLasPublicadasSinActa_laMasRecientePrimeroConSusAdjudicados() {
        long antigua = fx.convocatoria("PEND1");
        long conActa = fx.convocatoria("PEND2");
        long sinPublicar = fx.convocatoria("PEND3");
        finalista(convocatoria, "Ana", "Uno", 1, "ADJUDICADA", "80.00");
        finalista(convocatoria, "Beto", "Dos", 2, "ADJUDICADA", "70.00");
        finalista(sinPublicar, "Cris", "Tres", 1, "ADJUDICADA", "90.00");
        publicar(antigua, "2026-11-10T10:00:00Z");
        publicar(convocatoria, "2026-11-12T10:00:00Z");
        publicar(conActa, "2026-11-11T10:00:00Z");
        acta(conActa, "001/2026", "2026-11-13T10:00:00Z");

        Page<ConvocatoriaPendienteActaResponse> pagina = consulta.convocatoriasPendientes(PageRequest.of(0, 20));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(ConvocatoriaPendienteActaResponse::nombre)
                .containsExactly("Conv ACONS", "Conv PEND1");
        assertThat(pagina.getContent().get(0).adjudicados()).isEqualTo(2);
        assertThat(pagina.getContent().get(1).adjudicados()).isZero();
        assertThat(pagina.getContent().get(0).publicadoEn()).isEqualTo(Instant.parse("2026-11-12T10:00:00Z"));
    }

    @Test
    void pendientes_conUnActaEliminada_laConvocatoriaVuelveAEstarPendiente() {
        publicar(convocatoria, "2026-11-12T10:00:00Z");
        acta(convocatoria, "001/2026", "2026-11-13T10:00:00Z");
        assertThat(consulta.convocatoriasPendientes(PageRequest.of(0, 20)).getContent()).isEmpty();

        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE convocatoria_id = ?", convocatoria);

        assertThat(consulta.convocatoriasPendientes(PageRequest.of(0, 20)).getContent()).hasSize(1);
    }

    @Test
    void pendientes_paginaConElTotal() {
        for (int i = 0; i < 3; i++) {
            publicar(fx.convocatoria("PG" + i), "2026-11-1" + i + "T10:00:00Z");
        }

        Page<ConvocatoriaPendienteActaResponse> segunda = consulta.convocatoriasPendientes(PageRequest.of(1, 2));

        assertThat(segunda.getTotalElements()).isEqualTo(3);
        assertThat(segunda.getContent()).hasSize(1);
    }

    // ---- numeracion ----

    @Test
    void mayorCorrelativoDelAnio_sinActasEsCeroYCuentaTambienLasEliminadas() {
        assertThat(consulta.mayorCorrelativoDelAnio(2031)).isZero();

        acta(convocatoria, "007/2031", "2031-03-01T10:00:00Z");
        acta(fx.convocatoria("NUM2"), "012/2031", "2031-03-02T10:00:00Z");
        acta(fx.convocatoria("NUM3"), "999/2030", "2030-03-02T10:00:00Z");
        jdbc.update("UPDATE acta_adjudicacion SET eliminado_en = now() WHERE numero = '012/2031'");

        assertThat(consulta.mayorCorrelativoDelAnio(2031)).isEqualTo(12);
        assertThat(consulta.mayorCorrelativoDelAnio(2030)).isEqualTo(999);
        assertThat(consulta.mayorCorrelativoDelAnio(2032)).isZero();
    }

    @Test
    void mayorCorrelativoDelAnio_conMasDeTresDigitos_loLeeCompleto() {
        acta(convocatoria, "1000/2033", "2033-03-01T10:00:00Z");

        assertThat(consulta.mayorCorrelativoDelAnio(2033)).isEqualTo(1000);
    }

    @Test
    void bloquearNumeracion_dentroDeUnaTransaccion_noFalla() {
        consulta.bloquearNumeracion(2026);
    }
}
