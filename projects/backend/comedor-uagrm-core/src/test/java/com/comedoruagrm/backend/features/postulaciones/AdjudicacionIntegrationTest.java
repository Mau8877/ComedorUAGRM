package com.comedoruagrm.backend.features.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CorreoNotificacionService;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU05 de punta a punta contra un Postgres real: el panel de cupos, seleccionar finalistas, adjudicar y no
 * adjudicar, el historial, la bitacora sin datos sensibles, que no se avise a nadie, los rechazos
 * (409/400/404/401/403), la doble peticion y la competencia por el ultimo cupo, y el efecto en las listas.
 * El push, el correo y MinIO estan simulados.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class AdjudicacionIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MOTIVO = "{\"motivo\":\"No cumple los requisitos\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CanalPush canalPush;

    @MockitoBean
    private CorreoNotificacionService correoService;

    @MockitoBean
    private AlmacenamientoService almacenamiento;

    /** Un finalista creado para un caso. */
    private record Caso(long postulacion, long finalistaId, UUID finalista, UUID postulacionCodigo) {
    }

    private ExpedienteFixtures fx;
    private RequestPostProcessor admin;
    private RequestPostProcessor postulante;
    private long auditor;
    private long convocatoriaId;
    private UUID convocatoria;
    private int contadorCi;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @BeforeEach
    void preparar() throws Exception {
        borrarTodo();
        reset(canalPush, correoService, almacenamiento);
        fx = new ExpedienteFixtures(jdbc);
        contadorCi = 1000000;
        crearConClave("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS");
        crearConClave("otro@uagrm.test", "0000002", "POSTULANTE");
        crearConClave("directivo@uagrm.test", "0000003", "DIRECTIVO");
        crearConClave("comedor@uagrm.test", "0000004", "PERSONAL_COMEDOR");
        auditor = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        // 3 cupos y minimo 60.
        convocatoriaId = fx.convocatoriaConCupos("ADJINT", 3, "60", 5);
        convocatoria = codigoConvocatoria(convocatoriaId);
        admin = bearer(login("admin@dubss.test"));
        postulante = bearer(login("otro@uagrm.test"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- panel (CA01, CA22) ----

    @Test
    void panel_entregaCuposAdjudicadosDisponiblesMinimoYCantidadDeFinalistas() throws Exception {
        Caso a = finalista(convocatoriaId, "45", "VALIDADO", "25");
        finalista(convocatoriaId, "40", null, null);
        calificada(convocatoriaId, "50");
        calificada(convocatoriaId, "55");
        decidir(a, "adjudicar").andExpect(status().isOk());

        mockMvc.perform(get(panelUrl(convocatoria)).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.convocatoria.codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.convocatoria.estado").value("CERRADA"))
                .andExpect(jsonPath("$.data.cupos").value(3))
                .andExpect(jsonPath("$.data.puntajeMinimo").value(60.0))
                .andExpect(jsonPath("$.data.adjudicados").value(1))
                .andExpect(jsonPath("$.data.noAdjudicados").value(0))
                .andExpect(jsonPath("$.data.cuposDisponibles").value(2))
                .andExpect(jsonPath("$.data.cantidadFinalistas").value(5))
                .andExpect(jsonPath("$.data.totalFinalistas").value(2))
                .andExpect(jsonPath("$.data.calificadasSinSeleccionar").value(2))
                .andExpect(jsonPath("$.data.resultadosPublicados").value(false))
                .andExpect(jsonPath("$.data.decisionesHabilitadas").value(true));
    }

    @Test
    void panel_sinCuposOMinimo_noHabilitaLasDecisionesYNoInventaValores() throws Exception {
        long sin = fx.convocatoriaConCupos("ADJSIN", null, null, null);

        mockMvc.perform(get(panelUrl(codigoConvocatoria(sin))).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cupos").doesNotExist())
                .andExpect(jsonPath("$.data.puntajeMinimo").doesNotExist())
                .andExpect(jsonPath("$.data.cuposDisponibles").doesNotExist())
                .andExpect(jsonPath("$.data.cantidadFinalistas").doesNotExist())
                .andExpect(jsonPath("$.data.decisionesHabilitadas").value(false));
    }

    @Test
    void panel_enUnaConvocatoriaAbierta_seConsultaPeroNoHabilitaLasDecisiones() throws Exception {
        fx.estadoConvocatoria(convocatoriaId, "ABIERTA");

        mockMvc.perform(get(panelUrl(convocatoria)).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.convocatoria.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.data.decisionesHabilitadas").value(false));
    }

    // ---- seleccionar finalistas (CA02, CA03, CA14) ----

    @Test
    void seleccionar_conCantidadDelRequest_creaLosFinalistasEnOrdenConSuPosicionDeLaListaCompleta() throws Exception {
        UUID p60 = calificada(convocatoriaId, "60");
        UUID p55 = calificada(convocatoriaId, "55");
        calificada(convocatoriaId, "50");
        calificada(convocatoriaId, "45");

        seleccionar("{\"cantidad\":2}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seleccionados").value(2))
                .andExpect(jsonPath("$.data.finalistas[0].postulacion").value(p60.toString()))
                .andExpect(jsonPath("$.data.finalistas[0].posicion").value(1))
                .andExpect(jsonPath("$.data.finalistas[1].postulacion").value(p55.toString()))
                .andExpect(jsonPath("$.data.finalistas[1].posicion").value(2));

        assertThat(contar("finalista")).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT f.posicion FROM finalista f ORDER BY f.posicion""", Integer.class)).containsExactly(1, 2);
        assertThat(contar("bitacora WHERE accion = 'SELECCION_FINALISTAS'")).isEqualTo(1);
        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = 'SELECCION_FINALISTAS'",
                String.class);
        assertThat(detalle).contains(convocatoria.toString()).doesNotContain("Ana").doesNotContain("Rojas");
    }

    @Test
    void seleccionar_sinCantidad_usaLaConfiguradaEnLaConvocatoria() throws Exception {
        jdbc.update("UPDATE convocatoria SET cantidad_finalistas = 2 WHERE id = ?", convocatoriaId);
        for (String ia : new String[] {"60", "55", "50"}) {
            calificada(convocatoriaId, ia);
        }

        mockMvc.perform(post(seleccionUrl(convocatoria)).with(admin)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seleccionados").value(2))
                .andExpect(jsonPath("$.data.finalistas.length()").value(2));

        assertThat(contar("finalista")).isEqualTo(2);
    }

    @Test
    void seleccionar_unaSegundaVezNoRepiteAlosAnterioresYContinuaLasPosiciones() throws Exception {
        for (String ia : new String[] {"60", "55", "50", "45"}) {
            calificada(convocatoriaId, ia);
        }

        seleccionar("{\"cantidad\":2}").andExpect(status().isCreated());
        seleccionar("{\"cantidad\":1}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.finalistas[0].posicion").value(3));

        assertThat(contar("finalista")).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT posicion FROM finalista ORDER BY posicion", Integer.class))
                .containsExactly(1, 2, 3);
    }

    @Test
    void seleccionar_conCantidadInvalida_da400ErrSys01_yNoCreaNada() throws Exception {
        for (String ia : new String[] {"60", "55"}) {
            calificada(convocatoriaId, ia);
        }

        for (String cuerpo : new String[] {"{\"cantidad\":0}", "{\"cantidad\":-1}", "{\"cantidad\":3}"}) {
            seleccionar(cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }

        assertThat(contar("finalista")).isZero();
    }

    @Test
    void seleccionar_conCantidadMayorQueLosCupos_esPermitida() throws Exception {
        for (int i = 0; i < 5; i++) {
            calificada(convocatoriaId, String.valueOf(40 + i));
        }

        seleccionar("{\"cantidad\":5}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seleccionados").value(5));

        assertThat(contar("finalista")).isEqualTo(5);
    }

    @Test
    void seleccionar_enUnaConvocatoriaNoCerradaOSinCuposOMinimo_da409ErrAdj05() throws Exception {
        calificada(convocatoriaId, "60");
        fx.estadoConvocatoria(convocatoriaId, "ABIERTA");
        seleccionar("{\"cantidad\":1}").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_05"));
        fx.estadoConvocatoria(convocatoriaId, "CERRADA");
        jdbc.update("UPDATE convocatoria SET cupos = NULL WHERE id = ?", convocatoriaId);
        seleccionar("{\"cantidad\":1}").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_05"));
        jdbc.update("UPDATE convocatoria SET cupos = 3, puntaje_minimo = NULL WHERE id = ?", convocatoriaId);
        seleccionar("{\"cantidad\":1}").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_05"));

        assertThat(contar("finalista")).isZero();
    }

    @Test
    void seleccionar_conConvocatoriaInexistente_da404ErrPri01() throws Exception {
        mockMvc.perform(post(seleccionUrl(UUID.randomUUID())).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- adjudicar (CA04, CA15, CA16, CA17, CA20) ----

    @Test
    void adjudicar_cambiaElEstado_guardaLaAdjudicacionElHistorial_yNoAvisaANadie() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        String auditoriasAntes = instantanea("SELECT id || ':' || resultado FROM auditoria_expediente");
        String puntajeAntes = instantanea("SELECT id || ':' || puntaje_total || ':' || vigente FROM puntaje_postulacion");

        decidir(c, "adjudicar").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Beca adjudicada correctamente"))
                .andExpect(jsonPath("$.data.resultado").value("ADJUDICADA"))
                .andExpect(jsonPath("$.data.estado").value("adjudicada"))
                .andExpect(jsonPath("$.data.puntajeFinal").value(70.0))
                .andExpect(jsonPath("$.data.decididaPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.cuposDisponibles").value(2))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        assertThat(estado(c)).isEqualTo("adjudicada");
        assertThat(jdbc.queryForMap("""
                SELECT resultado::text AS resultado, puntaje_final, decidido_por, fecha_adjudicacion
                FROM adjudicacion WHERE postulacion_id = ?""", c.postulacion()))
                .containsEntry("resultado", "ADJUDICADA").containsEntry("decidido_por", auditor)
                .satisfies(m -> {
                    assertThat((BigDecimal) m.get("puntaje_final")).isEqualByComparingTo("70");
                    assertThat(m.get("fecha_adjudicacion")).isNotNull();
                });
        assertThat(jdbc.queryForMap("""
                SELECT estado_anterior::text AS anterior, estado_nuevo::text AS nuevo, motivo, usuario_id
                FROM postulacion_historial WHERE postulacion_id = ?""", c.postulacion()))
                .containsEntry("anterior", "calificada").containsEntry("nuevo", "adjudicada")
                .containsEntry("motivo", null).containsEntry("usuario_id", auditor);
        assertThat(contar("notificacion")).isZero();
        assertThat(instantanea("SELECT id || ':' || resultado FROM auditoria_expediente")).isEqualTo(auditoriasAntes);
        assertThat(instantanea("SELECT id || ':' || puntaje_total || ':' || vigente FROM puntaje_postulacion"))
                .isEqualTo(puntajeAntes);
        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = 'ADJUDICACION_BECA'",
                String.class);
        assertThat(detalle).contains(c.finalista().toString(), "ADJUDICADA", "calificada", "adjudicada")
                .doesNotContain("Ana").doesNotContain("Rojas").doesNotContain("1000001");
    }

    @Test
    void adjudicar_elPuntajeFinalNoCambiaSiLaIaSeReprocesaDespues() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        decidir(c, "adjudicar").andExpect(status().isOk());

        jdbc.update("UPDATE puntaje_postulacion SET puntaje_total = 10 WHERE postulacion_id = ?", c.postulacion());

        assertThat(jdbc.queryForObject("SELECT puntaje_final FROM adjudicacion WHERE postulacion_id = ?",
                BigDecimal.class, c.postulacion())).isEqualByComparingTo("70");
    }

    @Test
    void adjudicar_sinAuditoriaOConUltimaNoValidada_da409ErrAdj02() throws Exception {
        Caso sin = finalista(convocatoriaId, "45", null, null);
        Caso observado = finalista(convocatoriaId, "45", "OBSERVADO", null);
        Caso noValido = finalista(convocatoriaId, "45", "NO_VALIDO", null);

        for (Caso c : new Caso[] {sin, observado, noValido}) {
            decidir(c, "adjudicar").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_02"));
        }

        assertThat(contar("adjudicacion")).isZero();
    }

    @Test
    void adjudicar_conPuntajeBajoElMinimo_da409ErrAdj03_yConPuntajeIgualAlMinimoEsPermitido() throws Exception {
        Caso bajo = finalista(convocatoriaId, "30", "VALIDADO", "20");
        Caso igual = finalista(convocatoriaId, "40", "VALIDADO", "20");

        decidir(bajo, "adjudicar").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_03"));
        decidir(igual, "adjudicar").andExpect(status().isOk()).andExpect(jsonPath("$.data.puntajeFinal").value(60.0));

        assertThat(estado(bajo)).isEqualTo("calificada");
        assertThat(contar("adjudicacion")).isEqualTo(1);
    }

    @Test
    void adjudicar_conCuposAgotados_da409ErrAdj04_yAunAsiSePuedeNoAdjudicar() throws Exception {
        jdbc.update("UPDATE convocatoria SET cupos = 1 WHERE id = ?", convocatoriaId);
        Caso primero = finalista(convocatoriaId, "45", "VALIDADO", "25");
        Caso segundo = finalista(convocatoriaId, "45", "VALIDADO", "25");
        decidir(primero, "adjudicar").andExpect(status().isOk());

        decidir(segundo, "adjudicar").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_04"));
        decidir(segundo, "no-adjudicar", MOTIVO).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuposDisponibles").value(0));

        assertThat(estado(segundo)).isEqualTo("no_adjudicada");
    }

    // ---- no adjudicar (CA05, CA06, CA08) ----

    @Test
    void noAdjudicar_conCadaResultadoDeAuditoria_guardaLaDecisionConMotivo_sinTocarLosCupos() throws Exception {
        Caso validado = finalista(convocatoriaId, "45", "VALIDADO", "25");
        Caso observado = finalista(convocatoriaId, "45", "OBSERVADO", null);
        Caso noValido = finalista(convocatoriaId, "45", "NO_VALIDO", null);

        for (Caso c : new Caso[] {validado, observado, noValido}) {
            decidir(c, "no-adjudicar", MOTIVO).andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Postulación marcada como no adjudicada correctamente"))
                    .andExpect(jsonPath("$.data.resultado").value("NO_ADJUDICADA"))
                    .andExpect(jsonPath("$.data.estado").value("no_adjudicada"))
                    .andExpect(jsonPath("$.data.cuposDisponibles").value(3));
            assertThat(estado(c)).isEqualTo("no_adjudicada");
            assertThat(jdbc.queryForMap("""
                    SELECT estado_nuevo::text AS nuevo, motivo FROM postulacion_historial WHERE postulacion_id = ?""",
                    c.postulacion())).containsEntry("nuevo", "no_adjudicada")
                    .containsEntry("motivo", "No cumple los requisitos");
        }

        assertThat(contar("adjudicacion WHERE resultado = 'NO_ADJUDICADA'")).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT puntaje_final FROM adjudicacion WHERE postulacion_id = ?",
                BigDecimal.class, validado.postulacion())).isEqualByComparingTo("70");
        assertThat(jdbc.queryForObject("SELECT puntaje_final FROM adjudicacion WHERE postulacion_id = ?",
                BigDecimal.class, observado.postulacion())).isNull();
        assertThat(contar("notificacion")).isZero();
        assertThat(contar("bitacora WHERE accion = 'NO_ADJUDICACION_BECA'")).isEqualTo(3);
        for (String detalle : jdbc.queryForList(
                "SELECT detalle::text FROM bitacora WHERE accion = 'NO_ADJUDICACION_BECA'", String.class)) {
            assertThat(detalle).doesNotContain("requisitos").doesNotContain("Ana").doesNotContain("Rojas");
        }
    }

    @Test
    void noAdjudicar_sinAuditoria_da409ErrAdj02() throws Exception {
        Caso sin = finalista(convocatoriaId, "45", null, null);

        decidir(sin, "no-adjudicar", MOTIVO).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_ADJ_02"));

        assertThat(contar("adjudicacion")).isZero();
    }

    @Test
    void noAdjudicar_conMotivoInvalido_da400ErrSys01_yNadaCambia() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");

        for (String cuerpo : new String[] {"{}", "{\"motivo\":\"   \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}"}) {
            decidir(c, "no-adjudicar", cuerpo).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }

        assertThat(estado(c)).isEqualTo("calificada");
        assertThat(contar("adjudicacion")).isZero();
        assertThat(contar("postulacion_historial")).isZero();
        decidir(c, "no-adjudicar", "{\"motivo\":\"" + "x".repeat(500) + "\"}").andExpect(status().isOk());
    }

    // ---- doble peticion y concurrencia (CA10, CA11, CA13) ----

    @Test
    void unaSegundaDecisionSeguida_da409ErrAdj01_yQuedaUnaSolaFila() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        decidir(c, "adjudicar").andExpect(status().isOk());

        decidir(c, "adjudicar").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_01"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("adjudicada")));
        decidir(c, "no-adjudicar", MOTIVO).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_ADJ_01"));

        assertThat(contar("adjudicacion")).isEqualTo(1);
        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("bitacora WHERE accion IN ('ADJUDICACION_BECA', 'NO_ADJUDICACION_BECA')")).isEqualTo(1);
    }

    @Test
    void unaPostulacionYaDevueltaArchivadaORechazada_da409ErrAdj01() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        for (String estado : new String[] {"devuelta", "archivada", "no_adjudicada", "adjudicada"}) {
            jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?", estado,
                    c.postulacion());

            decidir(c, "adjudicar").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_01"));
            decidir(c, "no-adjudicar", MOTIVO).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("ERR_ADJ_01"));
        }

        assertThat(contar("adjudicacion")).isZero();
    }

    @Test
    void dosPeticionesALaVezParaElMismoFinalista_unaGanaYLaOtraRecibe409_conUnaSolaAnotacion() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");

        List<Integer> codigos = enParalelo(
                () -> decidir(c, "adjudicar").andReturn().getResponse().getStatus(),
                () -> decidir(c, "adjudicar").andReturn().getResponse().getStatus());

        assertThat(codigos).containsExactlyInAnyOrder(200, 409);
        assertThat(contar("adjudicacion")).isEqualTo(1);
        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("bitacora WHERE accion = 'ADJUDICACION_BECA'")).isEqualTo(1);
    }

    @Test
    void dosAdjudicacionesALaVezConUnSoloCupoLibre_unaGanaYLaOtraRecibeErrAdj04() throws Exception {
        jdbc.update("UPDATE convocatoria SET cupos = 1 WHERE id = ?", convocatoriaId);
        Caso a = finalista(convocatoriaId, "45", "VALIDADO", "25");
        Caso b = finalista(convocatoriaId, "44", "VALIDADO", "25");

        List<Integer> codigos = enParalelo(
                () -> decidir(a, "adjudicar").andReturn().getResponse().getStatus(),
                () -> decidir(b, "adjudicar").andReturn().getResponse().getStatus());

        assertThat(codigos).containsExactlyInAnyOrder(200, 409);
        assertThat(contar("adjudicacion WHERE resultado = 'ADJUDICADA'")).isEqualTo(1);
        mockMvc.perform(get(panelUrl(convocatoria)).with(admin))
                .andExpect(jsonPath("$.data.adjudicados").value(1))
                .andExpect(jsonPath("$.data.cuposDisponibles").value(0));
    }

    // ---- efectos en las listas y en otros casos de uso (CA15, CA19) ----

    @Test
    void trasDecidir_saleDeLaListaPriorizada_seMuestraEnLaDeFinalistas_yNoSePuedeAuditar() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        String puntajes = instantanea("SELECT id FROM puntaje_postulacion");
        mockMvc.perform(get("/api/v1/lista-priorizada?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));

        decidir(c, "adjudicar").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/lista-priorizada?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/v1/finalistas?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].estadoPostulacion").value("adjudicada"));
        mockMvc.perform(post("/api/v1/finalistas/" + c.finalista() + "/auditorias").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_02"));
        assertThat(contar("auditoria_expediente")).isEqualTo(1);
        assertThat(instantanea("SELECT id FROM puntaje_postulacion")).isEqualTo(puntajes);
    }

    @Test
    void elHistorialConservaTodasLasFilasAnteriores() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                   motivo, creado_en)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), CAST('devuelta' AS estado_postulacion),
                        'Falta el carnet', now() - interval '2 days'),
                       (?, ?, CAST('devuelta' AS estado_postulacion), CAST('calificada' AS estado_postulacion),
                        NULL, now() - interval '1 day')""", c.postulacion(), auditor, c.postulacion(), auditor);

        decidir(c, "adjudicar").andExpect(status().isOk());

        assertThat(jdbc.queryForList("""
                SELECT estado_nuevo::text FROM postulacion_historial WHERE postulacion_id = ?
                ORDER BY creado_en, id""", String.class, c.postulacion()))
                .containsExactly("devuelta", "calificada", "adjudicada");
    }

    // ---- 404, 401 y 403 (CA21) ----

    @Test
    void conFinalistaOConvocatoriaInexistente_da404() throws Exception {
        for (String accion : new String[] {"adjudicar", "no-adjudicar"}) {
            mockMvc.perform(post("/api/v1/finalistas/" + UUID.randomUUID() + "/" + accion).with(admin)
                    .contentType(MediaType.APPLICATION_JSON).content(MOTIVO)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("ERR_FIN_01"));
        }
        mockMvc.perform(get(panelUrl(UUID.randomUUID())).with(admin)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    @Test
    void sinSesionDa401_yOtrosRolesDan403() throws Exception {
        Caso c = finalista(convocatoriaId, "45", "VALIDADO", "25");
        String[] correos = {"otro@uagrm.test", "directivo@uagrm.test", "comedor@uagrm.test"};

        mockMvc.perform(get(panelUrl(convocatoria))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(seleccionUrl(convocatoria)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(accionUrl(c, "adjudicar"))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(accionUrl(c, "no-adjudicar")).contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isUnauthorized());
        for (String correo : correos) {
            RequestPostProcessor otro = bearer(login(correo));
            mockMvc.perform(get(panelUrl(convocatoria)).with(otro)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
            mockMvc.perform(post(seleccionUrl(convocatoria)).with(otro).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cantidad\":1}")).andExpect(status().isForbidden());
            mockMvc.perform(post(accionUrl(c, "adjudicar")).with(otro)).andExpect(status().isForbidden());
            mockMvc.perform(post(accionUrl(c, "no-adjudicar")).with(otro).contentType(MediaType.APPLICATION_JSON)
                    .content(MOTIVO)).andExpect(status().isForbidden());
        }

        assertThat(postulante).isNotNull();
        assertThat(estado(c)).isEqualTo("calificada");
        assertThat(contar("adjudicacion")).isZero();
        assertThat(contar("finalista")).isEqualTo(1);
    }

    // ---- ayudas ----

    private String panelUrl(UUID conv) {
        return "/api/v1/convocatorias/" + conv + "/adjudicacion";
    }

    private String seleccionUrl(UUID conv) {
        return "/api/v1/convocatorias/" + conv + "/finalistas";
    }

    private String accionUrl(Caso c, String accion) {
        return "/api/v1/finalistas/" + c.finalista() + "/" + accion;
    }

    private ResultActions seleccionar(String cuerpo) throws Exception {
        return mockMvc.perform(post(seleccionUrl(convocatoria)).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions decidir(Caso c, String accion) throws Exception {
        return decidir(c, accion, null);
    }

    private ResultActions decidir(Caso c, String accion, String cuerpo) throws Exception {
        var peticion = post(accionUrl(c, accion)).with(admin);
        if (cuerpo != null) {
            peticion = peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mockMvc.perform(peticion);
    }

    /** Ejecuta las dos tareas a la vez y devuelve sus resultados. */
    @SafeVarargs
    private List<Integer> enParalelo(Callable<Integer>... tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.length);
        try {
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(List.of(tareas))) {
                resultados.add(f.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Una postulacion calificada con puntaje de la IA, sin finalista. Devuelve su codigo publico. */
    private UUID calificada(long conv, String ia) {
        long postulacion = fx.postulacion(conv, fx.postulante(String.valueOf(++contadorCi)), "calificada");
        fx.puntajeIa(postulacion, ia, "{}");
        return jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class, postulacion);
    }

    /** Una postulacion calificada que ya es finalista, con su ultima auditoria (`resultado` nulo = sin ninguna). */
    private Caso finalista(long conv, String ia, String resultado, String academico) {
        long postulacion = fx.postulacion(conv, fx.postulante(String.valueOf(++contadorCi)), "calificada");
        fx.puntajeIa(postulacion, ia, "{}");
        long finalistaId = fx.finalista(postulacion, (int) contar("finalista") + 1);
        fx.entregaRecibida(finalistaId);
        if (resultado != null) {
            fx.auditoria(finalistaId, auditor, resultado, "VALIDADO".equals(resultado) ? null : "Observaciones",
                    academico, "2026-11-06T10:00:00Z");
        }
        return new Caso(postulacion, finalistaId,
                jdbc.queryForObject("SELECT codigo_finalista FROM finalista WHERE id = ?", UUID.class, finalistaId),
                jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class,
                        postulacion));
    }

    private UUID codigoConvocatoria(long id) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class, id);
    }

    private String estado(Caso c) {
        return jdbc.queryForObject("SELECT estado::text FROM postulacion WHERE id = ?", String.class, c.postulacion());
    }

    private void crearConClave(String correo, String ci, String rol) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, 'Admin', 'Prueba' FROM rol WHERE nombre = ?""", ci, correo,
                passwordEncoder.encode("secreta1"), rol);
    }

    private JsonNode login(String correo) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"secreta1\"}".formatted(correo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(cuerpo).get("data");
    }

    private RequestPostProcessor bearer(JsonNode sesion) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.get("accessToken").asText());
            return request;
        };
    }

    private String instantanea(String consulta) {
        return jdbc.queryForList(consulta, String.class).toString();
    }

    private long contar(String tablaYCondicion) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tablaYCondicion, Long.class);
    }

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
        jdbc.update("DELETE FROM adjudicacion");
        jdbc.update("DELETE FROM postulacion_historial");
        jdbc.update("DELETE FROM auditoria_expediente");
        jdbc.update("DELETE FROM documento_adjunto");
        jdbc.update("DELETE FROM respuesta_socioeconomica");
        jdbc.update("DELETE FROM convocatoria_documento");
        jdbc.update("DELETE FROM tipo_documento");
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM notificacion");
        jdbc.update("DELETE FROM dispositivo_fcm");
        jdbc.update("DELETE FROM finalista");
        jdbc.update("DELETE FROM puntaje_postulacion");
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM modelo_preseleccion");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
