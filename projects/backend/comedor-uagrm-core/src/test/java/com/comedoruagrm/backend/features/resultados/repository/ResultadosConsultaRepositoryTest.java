package com.comedoruagrm.backend.features.resultados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.resultados.dto.AdjudicadoPublicoResponse;
import com.comedoruagrm.backend.features.resultados.dto.DecisionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Conteos;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Destinatario;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.FiltroDecisiones;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Pagina;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.JdbcContadorDeConsultas;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas de la publicacion de resultados (CU22) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResultadosConsultaRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private ExpedienteFixtures fx;
    private JdbcContadorDeConsultas contador;
    private ResultadosConsultaRepository consulta;
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
        consulta = new ResultadosConsultaRepository(new NamedParameterJdbcTemplate(contador));
        convocatoria = fx.convocatoria("RCONS");
        admin = fx.usuario("9700001", "ADMINISTRADOR_DUBSS");
        siguienteCi = 9710000;
    }

    /** Un finalista con la postulacion en el estado dado; con `resultado` no nulo, ademas su adjudicacion. */
    private long finalista(String nombres, String apellidos, String estado, String resultado, String puntaje) {
        return finalistaEn(convocatoria, nombres, apellidos, estado, resultado, puntaje);
    }

    private long finalistaEn(long convocatoriaId, String nombres, String apellidos, String estado,
            String resultado, String puntaje) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        long postulacion = fx.postulacion(convocatoriaId, usuario, estado);
        long finalista = fx.finalista(postulacion, (int) (siguienteCi - 9710000));
        if (resultado != null) {
            fx.adjudicacion(postulacion, resultado, puntaje, admin);
        }
        return finalista;
    }

    private UUID codigoConvocatoria() {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoria);
    }

    // ---- convocatoria, bloqueo, publicacion ----

    @Test
    void convocatoria_existente_devuelveSusDatos_yUnaInexistenteVacio() {
        var datos = consulta.convocatoria(codigoConvocatoria()).orElseThrow();

        assertThat(datos.id()).isEqualTo(convocatoria);
        assertThat(datos.nombre()).isEqualTo("Conv RCONS");
        assertThat(datos.estado()).isEqualTo("CERRADA");
        assertThat(consulta.convocatoria(UUID.randomUUID())).isEmpty();
    }

    @Test
    void convocatoria_eliminada_noSeEncuentra() {
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", convocatoria);

        assertThat(consulta.convocatoria(codigoConvocatoria())).isEmpty();
    }

    @Test
    void bloquearConvocatoria_noFallaSobreUnaFilaExistente() {
        consulta.bloquearConvocatoria(convocatoria);
    }

    @Test
    void marcarConvocatoriaPublicada_cambiaSoloElEstado() {
        consulta.marcarConvocatoriaPublicada(convocatoria);

        assertThat(jdbc.queryForObject("SELECT estado::text FROM convocatoria WHERE id = ?", String.class,
                convocatoria)).isEqualTo("PUBLICADA");
    }

    @Test
    void publicacion_sinFila_esVacia_yConFilaTraeCuandoYQuien() {
        assertThat(consulta.publicacion(convocatoria)).isEmpty();

        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)",
                convocatoria, admin);

        var publicacion = consulta.publicacion(convocatoria).orElseThrow();
        assertThat(publicacion.publicadoEn()).isNotNull();
        assertThat(publicacion.publicadoPor()).isEqualTo("Ana Rojas");
    }

    // ---- conteos ----

    @Test
    void contarPorEstado_cuentaCadaEstadoDeFinalista() {
        finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "80");
        finalista("Beto", "Dos", "adjudicada", "ADJUDICADA", "75");
        finalista("Carla", "Tres", "no_adjudicada", "NO_ADJUDICADA", "40");
        finalista("Dora", "Cuatro", "calificada", null, null);
        finalista("Elsa", "Cinco", "archivada", null, null);
        finalista("Fede", "Seis", "no_adjudicada", null, null);
        finalista("Gina", "Siete", "devuelta", null, null);

        Conteos c = consulta.contarPorEstado(convocatoria);

        assertThat(c.adjudicados()).isEqualTo(2);
        assertThat(c.noAdjudicados()).isEqualTo(1);
        assertThat(c.calificadas()).isEqualTo(1);
        assertThat(c.totalFinalistas()).isEqualTo(7);
    }

    @Test
    void contarPorEstado_sinFinalistas_todoEnCero_yNoMezclaOtrasConvocatorias() {
        long otra = fx.convocatoria("ROTRA");
        finalistaEn(otra, "Ana", "Ajena", "calificada", null, null);

        Conteos c = consulta.contarPorEstado(convocatoria);

        assertThat(c).isEqualTo(new Conteos(0, 0, 0, 0));
    }

    // ---- destinatarios del aviso ----

    @Test
    void destinatariosDelAviso_soloFinalistasConAdjudicacion_cadaUnoConSuResultado() {
        finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "80");
        finalista("Carla", "Tres", "no_adjudicada", "NO_ADJUDICADA", "40");
        finalista("Dora", "Cuatro", "calificada", null, null);
        finalista("Elsa", "Cinco", "archivada", null, null);
        finalista("Fede", "Seis", "no_adjudicada", null, null);
        finalista("Gina", "Siete", "devuelta", null, null);

        List<Destinatario> destinatarios = consulta.destinatariosDelAviso(convocatoria);

        assertThat(destinatarios).extracting(Destinatario::nombres).containsExactly("Ana", "Carla");
        assertThat(destinatarios).extracting(Destinatario::resultado)
                .containsExactly("ADJUDICADA", "NO_ADJUDICADA");
        assertThat(destinatarios).allSatisfy(d -> {
            assertThat(d.correo()).endsWith("@prueba.test");
            assertThat(d.usuarioId()).isPositive();
            assertThat(d.postulacionId()).isPositive();
            assertThat(d.postulacionCodigo()).isNotNull();
        });
    }

    // ---- decisiones del Administrador ----

    @Test
    void decisiones_porDefectoVanPorPuntajeDescendente_conElTotal_yElNuloAlFinal() {
        finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "70");
        finalista("Beto", "Dos", "adjudicada", "ADJUDICADA", "90");
        finalista("Carla", "Tres", "no_adjudicada", "NO_ADJUDICADA", null);
        finalista("Dora", "Cuatro", "no_adjudicada", "NO_ADJUDICADA", "50");

        Pagina<DecisionResultadoResponse> pagina = consulta.decisiones(convocatoria,
                new FiltroDecisiones(null, null), PageRequest.of(0, 20));

        assertThat(pagina.total()).isEqualTo(4);
        assertThat(pagina.filas()).extracting(DecisionResultadoResponse::nombres)
                .containsExactly("Beto", "Ana", "Dora", "Carla");
        assertThat(pagina.filas().get(0).finalista()).isNotNull();
        assertThat(pagina.filas().get(0).fechaAdjudicacion()).isNotNull();
        assertThat(pagina.filas().get(3).puntajeFinal()).isNull();
    }

    @Test
    void decisiones_pagina_respetaPageSizeYDesplazamiento() {
        for (int i = 1; i <= 5; i++) {
            finalista("Nombre" + i, "Apellido" + i, "adjudicada", "ADJUDICADA", String.valueOf(50 + i));
        }

        Pagina<DecisionResultadoResponse> segunda = consulta.decisiones(convocatoria,
                new FiltroDecisiones(null, null), PageRequest.of(1, 2));

        assertThat(segunda.total()).isEqualTo(5);
        assertThat(segunda.filas()).extracting(DecisionResultadoResponse::nombres)
                .containsExactly("Nombre3", "Nombre2");
    }

    @Test
    void decisiones_filtroPorResultado_search_yOrdenPorApellidos() {
        finalista("Ana", "Zapata", "adjudicada", "ADJUDICADA", "80");
        finalista("Beto", "Aguilar", "adjudicada", "ADJUDICADA", "75");
        finalista("Carla", "Mendez", "no_adjudicada", "NO_ADJUDICADA", "40");

        var soloNo = consulta.decisiones(convocatoria, new FiltroDecisiones("NO_ADJUDICADA", null),
                PageRequest.of(0, 20));
        var porApellido = consulta.decisiones(convocatoria, new FiltroDecisiones(null, null),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "apellidos")));
        var buscada = consulta.decisiones(convocatoria, new FiltroDecisiones(null, "agui"),
                PageRequest.of(0, 20));
        var sinComodin = consulta.decisiones(convocatoria, new FiltroDecisiones(null, "%"),
                PageRequest.of(0, 20));

        assertThat(soloNo.filas()).extracting(DecisionResultadoResponse::nombres).containsExactly("Carla");
        assertThat(soloNo.total()).isEqualTo(1);
        assertThat(porApellido.filas()).extracting(DecisionResultadoResponse::apellidos)
                .containsExactly("Aguilar", "Mendez", "Zapata");
        assertThat(buscada.filas()).extracting(DecisionResultadoResponse::nombres).containsExactly("Beto");
        assertThat(sinComodin.filas()).isEmpty();
    }

    @Test
    void decisiones_sortOFiltroNoSoportado_fallaConErrSys05() {
        finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "80");

        assertThatThrownBy(() -> consulta.decisiones(convocatoria, new FiltroDecisiones(null, null),
                PageRequest.of(0, 20, Sort.by("ci"))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
        assertThatThrownBy(() -> consulta.decisiones(convocatoria, new FiltroDecisiones("TODAS", null),
                PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
        assertThatThrownBy(() -> consulta.decisiones(convocatoria, new FiltroDecisiones(null, null),
                PageRequest.of(0, 20, Sort.by("nombres").and(Sort.by("apellidos")))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void decisiones_noTraeCiNiCorreo_yNoMezclaOtrasConvocatorias() {
        finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "80");
        long otra = fx.convocatoria("RCONS2");
        finalistaEn(otra, "Zeta", "Ajena", "adjudicada", "ADJUDICADA", "99");

        var pagina = consulta.decisiones(convocatoria, new FiltroDecisiones(null, null), PageRequest.of(0, 20));

        assertThat(pagina.filas()).extracting(DecisionResultadoResponse::nombres).containsExactly("Ana");
        assertThat(DecisionResultadoResponse.class.getRecordComponents()).extracting("name")
                .doesNotContain("ci", "correo", "motivo");
    }

    // ---- lista publica ----

    @Test
    void adjudicadosPublicos_soloAdjudicados_conLosCincoCampos_yOrdenPorNotaDescendente() {
        finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "70.5");
        finalista("Beto", "Dos", "adjudicada", "ADJUDICADA", "90");
        finalista("Carla", "Tres", "no_adjudicada", "NO_ADJUDICADA", "99");
        finalista("Dora", "Cuatro", "calificada", null, null);

        Pagina<AdjudicadoPublicoResponse> pagina = consulta.adjudicadosPublicos(convocatoria, null,
                PageRequest.of(0, 20));

        assertThat(pagina.total()).isEqualTo(2);
        assertThat(pagina.filas()).extracting(AdjudicadoPublicoResponse::nombres).containsExactly("Beto", "Ana");
        AdjudicadoPublicoResponse primero = pagina.filas().get(0);
        assertThat(primero.apellidos()).isEqualTo("Dos");
        assertThat(primero.nota()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(primero.carrera()).isNotBlank();
        assertThat(primero.facultad()).isNotBlank();
        assertThat(AdjudicadoPublicoResponse.class.getRecordComponents()).extracting("name")
                .containsExactly("nombres", "apellidos", "nota", "carrera", "facultad");
    }

    @Test
    void adjudicadosPublicos_searchBuscaEnNombreApellidoCarreraYFacultad_yElOrdenSeElige() {
        finalista("Ana", "Zapata", "adjudicada", "ADJUDICADA", "80");
        finalista("Beto", "Aguilar", "adjudicada", "ADJUDICADA", "75");
        String carrera = jdbc.queryForObject("SELECT nombre FROM carrera ORDER BY id LIMIT 1", String.class);
        String facultad = jdbc.queryForObject("SELECT facultad FROM carrera ORDER BY id LIMIT 1", String.class);

        var porNombre = consulta.adjudicadosPublicos(convocatoria, "beto", PageRequest.of(0, 20));
        var porCarrera = consulta.adjudicadosPublicos(convocatoria, carrera, PageRequest.of(0, 20));
        var porFacultad = consulta.adjudicadosPublicos(convocatoria, facultad, PageRequest.of(0, 20));
        var porApellido = consulta.adjudicadosPublicos(convocatoria, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "apellidos")));
        var nadie = consulta.adjudicadosPublicos(convocatoria, "zzzz-no-existe", PageRequest.of(0, 20));

        assertThat(porNombre.filas()).extracting(AdjudicadoPublicoResponse::nombres).containsExactly("Beto");
        assertThat(porCarrera.total()).isEqualTo(2);
        assertThat(porFacultad.total()).isEqualTo(2);
        assertThat(porApellido.filas()).extracting(AdjudicadoPublicoResponse::apellidos)
                .containsExactly("Aguilar", "Zapata");
        assertThat(nadie.filas()).isEmpty();
        assertThat(nadie.total()).isZero();
    }

    @Test
    void adjudicadosPublicos_sortNoSoportado_fallaConErrSys05() {
        assertThatThrownBy(() -> consulta.adjudicadosPublicos(convocatoria, null,
                PageRequest.of(0, 20, Sort.by("ci"))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
    }

    // ---- mi resultado y convocatorias publicadas ----

    @Test
    void miResultado_devuelveElDelFinalistaConDecision_yVacioParaAjenosYSinDecision() {
        long adjudicado = finalista("Ana", "Uno", "adjudicada", "ADJUDICADA", "80");
        long noAdjudicado = finalista("Carla", "Tres", "no_adjudicada", "NO_ADJUDICADA", "40");
        long sinDecision = finalista("Dora", "Cuatro", "calificada", null, null);
        UUID ana = codigoUsuarioDeFinalista(adjudicado);
        UUID carla = codigoUsuarioDeFinalista(noAdjudicado);
        UUID dora = codigoUsuarioDeFinalista(sinDecision);
        UUID ajeno = UUID.randomUUID();

        assertThat(consulta.miResultado(convocatoria, ana)).hasValueSatisfying(r -> {
            assertThat(r.resultado()).isEqualTo("ADJUDICADA");
            assertThat(r.nota()).isEqualByComparingTo(new BigDecimal("80"));
        });
        assertThat(consulta.miResultado(convocatoria, carla)).hasValueSatisfying(
                r -> assertThat(r.resultado()).isEqualTo("NO_ADJUDICADA"));
        assertThat(consulta.miResultado(convocatoria, dora)).isEmpty();
        assertThat(consulta.miResultado(convocatoria, ajeno)).isEmpty();
    }

    private UUID codigoUsuarioDeFinalista(long finalistaId) {
        return jdbc.queryForObject("""
                SELECT u.codigo_usuario FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id
                JOIN usuario u ON u.id = po.postulante_id WHERE f.id = ?""", UUID.class, finalistaId);
    }

    @Test
    void convocatoriasPublicadas_soloLasConFila_laMasRecientePrimero_conPaginacion() {
        long segunda = fx.convocatoria("RCONS3");
        long sinPublicar = fx.convocatoria("RCONS4");
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en) "
                + "VALUES (?, ?, '2026-11-01T10:00:00Z')", convocatoria, admin);
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en) "
                + "VALUES (?, ?, '2026-11-05T10:00:00Z')", segunda, admin);

        var pagina = consulta.convocatoriasPublicadas(PageRequest.of(0, 20));
        var unaSola = consulta.convocatoriasPublicadas(PageRequest.of(0, 1));

        assertThat(sinPublicar).isNotEqualTo(segunda);
        assertThat(pagina.total()).isEqualTo(2);
        assertThat(pagina.filas()).extracting(c -> c.nombre()).containsExactly("Conv RCONS3", "Conv RCONS");
        assertThat(unaSola.filas()).hasSize(1);
        assertThat(unaSola.total()).isEqualTo(2);
    }

    // ---- sin N+1 ----

    @Test
    void listas_resuelvenTodoConUnNumeroConstanteDeConsultas() {
        for (int i = 0; i < 3; i++) {
            finalista("Pocos" + i, "Ap" + i, "adjudicada", "ADJUDICADA", "80");
        }
        int consultasConPocos = contarConsultasDeLasListas();

        for (int i = 0; i < 40; i++) {
            finalista("Muchos" + i, "Ap" + i, i % 2 == 0 ? "adjudicada" : "no_adjudicada",
                    i % 2 == 0 ? "ADJUDICADA" : "NO_ADJUDICADA", "70");
        }
        int consultasConMuchos = contarConsultasDeLasListas();

        assertThat(consultasConPocos).isPositive();
        assertThat(consultasConMuchos).isEqualTo(consultasConPocos);
    }

    private int contarConsultasDeLasListas() {
        contador.reiniciar();
        consulta.decisiones(convocatoria, new FiltroDecisiones(null, null), PageRequest.of(0, 50));
        consulta.adjudicadosPublicos(convocatoria, null, PageRequest.of(0, 50));
        consulta.destinatariosDelAviso(convocatoria);
        consulta.contarPorEstado(convocatoria);
        return contador.consultas();
    }
}
