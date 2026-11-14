package com.comedoruagrm.backend.features.resultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
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
import com.comedoruagrm.backend.features.notificaciones.service.MensajePush;
import com.comedoruagrm.backend.features.notificaciones.service.ResultadoPush;
import com.comedoruagrm.backend.support.ContadorDeSentenciasDataSource;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU22 de punta a punta contra un Postgres real: publicar los resultados, la fila de publicacion, el estado de la
 * convocatoria, el aviso a cada finalista con decision (bandeja, correo y push), la bitacora sin datos personales,
 * los rechazos (409/404/403/401), la doble peticion, que un fallo de envio no deshaga nada, que despues de publicar
 * nadie decida, la lista publica y el resultado propio, los avisos por lotes sin N+1 y los comunicados a
 * `ADJUDICADOS`. El push y el correo estan simulados.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class PublicacionResultadosIntegracionTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MOTIVO = "{\"motivo\":\"No cumple los requisitos\"}";

    /** Cuenta las sentencias SQL del hilo de la peticion para probar que los avisos no hacen N+1. */
    @TestConfiguration
    static class ContadorConfig {

        @Bean
        static BeanPostProcessor contadorDeSentencias() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) throws BeansException {
                    if (bean instanceof DataSource ds && !(bean instanceof ContadorDeSentenciasDataSource)) {
                        return new ContadorDeSentenciasDataSource(ds);
                    }
                    return bean;
                }
            };
        }
    }

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

    private ExpedienteFixtures fx;
    private RequestPostProcessor admin;
    private long adminId;
    private long convocatoriaId;
    private UUID convocatoria;
    private int siguienteCi;

    private long ana;
    private long beto;
    private long carla;
    private long dora;
    private long elsa;
    private long fede;
    private long gina;

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
        when(correoService.enviar(any(), any(), any())).thenReturn(true);
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenAnswer(i -> new ResultadoPush(((List<?>) i.getArgument(0)).size(), List.of(), List.of()));

        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 2000100;
        crearConClave("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS");
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        convocatoriaId = fx.convocatoria("PUBINT");
        convocatoria = codigoConvocatoria(convocatoriaId);

        ana = finalista(convocatoriaId, "2000001", "Ana", "Uno", "adjudicada", "ADJUDICADA", "80.00");
        beto = finalista(convocatoriaId, "2000002", "Beto", "Dos", "adjudicada", "ADJUDICADA", "70.00");
        carla = finalista(convocatoriaId, "2000003", "Carla", "Tres", "no_adjudicada", "NO_ADJUDICADA", "40.00");
        // Sin decision de adjudicacion: ya fueron avisados por el CU19 o el CU20 y no se avisa de nuevo.
        dora = finalista(convocatoriaId, "2000004", "Dora", "Cuatro", "archivada", null, null);
        elsa = finalista(convocatoriaId, "2000005", "Elsa", "Cinco", "no_adjudicada", null, null);
        fede = finalista(convocatoriaId, "2000006", "Fede", "Seis", "devuelta", null, null);
        gina = fx.postulante("2000099");
        jdbc.update("UPDATE usuario SET nombres = 'Gina', apellidos = 'Ajena' WHERE id = ?", gina);
        conClave("2000001");
        conClave("2000003");
        conClave("2000099");
        admin = bearer(login("admin@dubss.test"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- publicar con todo decidido (CA01, CA05, CA06, CA11, CA12, CA14) ----

    @Test
    void publicar_conTodoDecidido_registraLaPublicacion_cambiaElEstado_yAvisaACadaFinalistaConDecision()
            throws Exception {
        String estadosAntes = instantanea("SELECT id || ':' || estado::text FROM postulacion ORDER BY id");
        String adjudicacionesAntes = instantanea("SELECT id || ':' || resultado::text || ':' || puntaje_final "
                + "FROM adjudicacion ORDER BY id");

        publicar(convocatoria).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Resultados publicados correctamente"))
                .andExpect(jsonPath("$.data.convocatoria.codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.convocatoria.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.data.publicadoPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.adjudicados").value(2))
                .andExpect(jsonPath("$.data.noAdjudicados").value(1))
                .andExpect(jsonPath("$.data.avisados").value(3))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        assertThat(jdbc.queryForMap("""
                SELECT convocatoria_id, publicado_por FROM publicacion_resultado""")).containsEntry("convocatoria_id",
                convocatoriaId).containsEntry("publicado_por", adminId);
        assertThat(estadoConvocatoria()).isEqualTo("PUBLICADA");
        // Un aviso por finalista con adjudicacion, cada uno con su resultado; ninguno para archivada, rechazada ni devuelta.
        assertThat(avisosDe(ana)).hasSize(1).first().satisfies(a -> {
            assertThat(a.get("tipo")).isEqualTo("SUCCESS");
            assertThat(a.get("titulo")).isEqualTo("Beca Comedor: resultado de tu postulación");
            assertThat(a.get("mensaje")).isEqualTo("Ana, ¡felicidades! Tu postulación en Conv PUBINT fue adjudicada: "
                    + "obtuviste la Beca Comedor.");
            assertThat(a.get("postulacion_id")).isNotNull();
        });
        assertThat(avisosDe(beto)).hasSize(1);
        assertThat(avisosDe(carla)).hasSize(1).first().satisfies(a -> {
            assertThat(a.get("tipo")).isEqualTo("INFO");
            assertThat(a.get("mensaje")).isEqualTo("Carla, tu postulación en Conv PUBINT no fue adjudicada. "
                    + "Gracias por participar.");
        });
        assertThat(avisosDe(dora)).isEmpty();
        assertThat(avisosDe(elsa)).isEmpty();
        assertThat(avisosDe(fede)).isEmpty();
        assertThat(avisosDe(gina)).isEmpty();
        assertThat(contar("notificacion")).isEqualTo(3);

        // El correo y el push salen despues del commit, por cada uno de los tres.
        verify(correoService, timeout(5000)).enviar(correoDe(ana), "Beca Comedor: resultado de tu postulación",
                "Ana, ¡felicidades! Tu postulación en Conv PUBINT fue adjudicada: obtuviste la Beca Comedor.");
        verify(correoService, timeout(5000)).enviar(correoDe(beto), "Beca Comedor: resultado de tu postulación",
                "Beto, ¡felicidades! Tu postulación en Conv PUBINT fue adjudicada: obtuviste la Beca Comedor.");
        verify(correoService, timeout(5000)).enviar(correoDe(carla), "Beca Comedor: resultado de tu postulación",
                "Carla, tu postulación en Conv PUBINT no fue adjudicada. Gracias por participar.");
        verify(correoService, never()).enviar(org.mockito.ArgumentMatchers.eq(correoDe(dora)), any(), any());
        esperarEnviadas(3);

        // Ninguna postulacion ni adjudicacion cambia, y la bitacora no lleva datos personales.
        assertThat(instantanea("SELECT id || ':' || estado::text FROM postulacion ORDER BY id")).isEqualTo(estadosAntes);
        assertThat(instantanea("SELECT id || ':' || resultado::text || ':' || puntaje_final FROM adjudicacion ORDER BY id"))
                .isEqualTo(adjudicacionesAntes);
        assertThat(contar("postulacion_historial")).isZero();
        assertThat(contar("bitacora WHERE accion = 'PUBLICACION_RESULTADOS'")).isEqualTo(1);
        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = 'PUBLICACION_RESULTADOS'",
                String.class);
        assertThat(detalle).contains(convocatoria.toString()).contains("\"adjudicados\"").contains("\"avisados\"")
                .doesNotContain("Ana").doesNotContain("Carla").doesNotContain("Uno").doesNotContain("@")
                .doesNotContain("2000001").doesNotContain("Bearer").doesNotContain("token");
    }

    @Test
    void resumen_antesYDespuesDePublicar_yDecisionesDelAdministrador() throws Exception {
        mockMvc.perform(get(rutaAdmin(convocatoria, "resumen")).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adjudicados").value(2))
                .andExpect(jsonPath("$.data.noAdjudicados").value(1))
                .andExpect(jsonPath("$.data.calificadasPendientes").value(0))
                .andExpect(jsonPath("$.data.totalFinalistas").value(6))
                .andExpect(jsonPath("$.data.aAvisar").value(3))
                .andExpect(jsonPath("$.data.yaPublicada").value(false))
                .andExpect(jsonPath("$.data.puedePublicar").value(true));
        mockMvc.perform(get(rutaAdmin(convocatoria, "decisiones")).with(admin)
                        .param("filter[resultado]", "NO_ADJUDICADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombres").value("Carla"))
                .andExpect(jsonPath("$.data[0].resultado").value("NO_ADJUDICADA"))
                .andExpect(jsonPath("$.data[0].ci").doesNotExist())
                .andExpect(jsonPath("$.data[0].correo").doesNotExist())
                .andExpect(jsonPath("$.data[0].motivo").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get(rutaAdmin(convocatoria, "decisiones")).with(admin).param("sort", "-puntajeFinal"))
                .andExpect(jsonPath("$.data[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.data[2].nombres").value("Carla"));
        mockMvc.perform(get(rutaAdmin(convocatoria, "decisiones")).with(admin).param("sort", "ci"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));

        publicar(convocatoria).andExpect(status().isCreated());

        mockMvc.perform(get(rutaAdmin(convocatoria, "resumen")).with(admin))
                .andExpect(jsonPath("$.data.yaPublicada").value(true))
                .andExpect(jsonPath("$.data.publicadoPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.publicadoEn").exists())
                .andExpect(jsonPath("$.data.convocatoria.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.data.puedePublicar").value(false));
        esperarEnviadas(3);
    }

    // ---- con pendientes, ya publicada, no cerrada, inexistente (CA02, CA03, CA13) ----

    @Test
    void publicar_conFinalistasEnCalificada_da409ErrPub01ConLaCantidad_yNoGuardaNadaNiAvisa() throws Exception {
        finalista(convocatoriaId, "2000010", "Gaby", "Siete", "calificada", null, null);
        finalista(convocatoriaId, "2000011", "Hugo", "Ocho", "calificada", null, null);

        publicar(convocatoria).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_PUB_01"))
                .andExpect(jsonPath("$.message").value(
                        "Faltan 2 decisiones de adjudicación para publicar los resultados"));

        assertNadaSePublico();
        mockMvc.perform(get(rutaAdmin(convocatoria, "resumen")).with(admin))
                .andExpect(jsonPath("$.data.calificadasPendientes").value(2))
                .andExpect(jsonPath("$.data.puedePublicar").value(false));
    }

    @Test
    void publicar_dosVeces_laSegundaDa409ErrPub02_yHayUnaSolaPublicacionYUnSoloEnvio() throws Exception {
        publicar(convocatoria).andExpect(status().isCreated());
        esperarEnviadas(3);

        publicar(convocatoria).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_PUB_02"));

        assertThat(contar("publicacion_resultado")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(3);
        assertThat(contar("bitacora WHERE accion = 'PUBLICACION_RESULTADOS'")).isEqualTo(1);
        verify(correoService, timeout(2000).times(3)).enviar(any(), any(), any());
    }

    @Test
    void publicar_dosPeticionesAlaVez_unaGana_laOtraRecibe409ErrPub02_conUnSoloEnvio() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tareas.add(() -> mockMvc.perform(post(rutaAdmin(convocatoria, "publicar")).with(admin))
                        .andReturn().getResponse().getStatus());
            }
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(tareas)) {
                codigos.add(f.get());
            }

            assertThat(codigos).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }

        assertThat(contar("publicacion_resultado")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(3);
        assertThat(contar("bitacora WHERE accion = 'PUBLICACION_RESULTADOS'")).isEqualTo(1);
        esperarEnviadas(3);
        verify(correoService, timeout(2000).times(3)).enviar(any(), any(), any());
    }

    @Test
    void publicar_conLaConvocatoriaNoCerrada_da409ErrPub03() throws Exception {
        fx.estadoConvocatoria(convocatoriaId, "ABIERTA");

        publicar(convocatoria).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_PUB_03"));

        assertNadaSePublico("ABIERTA");
    }

    @Test
    void publicar_conUnaConvocatoriaInexistente_da404ErrPri01() throws Exception {
        publicar(UUID.randomUUID()).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
        mockMvc.perform(get(rutaAdmin(UUID.randomUUID(), "resumen")).with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
        mockMvc.perform(get(rutaAdmin(UUID.randomUUID(), "decisiones")).with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- fallo de envio (CA07) ----

    @Test
    void publicar_siFallaElEnvio_laPublicacionQueda_yElAvisoQuedaEnLaBandejaSinMarcarEnviado() throws Exception {
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenThrow(new IllegalStateException("fcm caido"));
        when(correoService.enviar(any(), any(), any())).thenReturn(false);

        publicar(convocatoria).andExpect(status().isCreated()).andExpect(jsonPath("$.data.avisados").value(3));

        verify(correoService, timeout(5000).times(3)).enviar(any(), any(), any());
        assertThat(contar("publicacion_resultado")).isEqualTo(1);
        assertThat(estadoConvocatoria()).isEqualTo("PUBLICADA");
        assertThat(contar("notificacion")).isEqualTo(3);
        // Pasado un momento ninguno se marca enviado: sigue en la bandeja para que se reintente.
        Thread.sleep(500);
        assertThat(contar("notificacion WHERE enviada_en IS NOT NULL")).isZero();
    }

    // ---- despues de publicar nadie decide (CA09, CA16) ----

    @Test
    void despuesDePublicar_adjudicarNoAdjudicarDevolverArchivarYRechazar_dan409YNoCambianNada() throws Exception {
        publicar(convocatoria).andExpect(status().isCreated());
        esperarEnviadas(3);
        // Se deja una postulacion en «calificada» a mano: lo unico que la frena es la publicacion.
        jdbc.update("UPDATE postulacion SET estado = CAST('calificada' AS estado_postulacion) "
                + "WHERE postulante_id = ?", dora);
        jdbc.update("DELETE FROM adjudicacion WHERE postulacion_id IN (SELECT id FROM postulacion "
                + "WHERE postulante_id = ?)", dora);
        String estadosAntes = instantanea("SELECT id || ':' || estado::text FROM postulacion ORDER BY id");
        long adjudicacionesAntes = contar("adjudicacion");
        UUID finalista = codigoFinalista(dora);

        mockMvc.perform(post("/api/v1/finalistas/" + finalista + "/adjudicar").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_01"))
                .andExpect(jsonPath("$.message").value("Los resultados de la convocatoria ya fueron publicados"));
        mockMvc.perform(post("/api/v1/finalistas/" + finalista + "/no-adjudicar").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_01"));
        mockMvc.perform(post("/api/v1/finalistas/" + finalista + "/devolver").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_01"));
        mockMvc.perform(post("/api/v1/finalistas/" + finalista + "/archivar").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ARC_01"));
        mockMvc.perform(post("/api/v1/finalistas/" + finalista + "/rechazar").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ARC_01"));
        mockMvc.perform(post("/api/v1/convocatorias/" + convocatoria + "/finalistas").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ADJ_01"));

        assertThat(instantanea("SELECT id || ':' || estado::text FROM postulacion ORDER BY id")).isEqualTo(estadosAntes);
        assertThat(contar("adjudicacion")).isEqualTo(adjudicacionesAntes);
        assertThat(contar("postulacion_historial")).isZero();
        mockMvc.perform(get("/api/v1/convocatorias/" + convocatoria + "/adjudicacion").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultadosPublicados").value(true))
                .andExpect(jsonPath("$.data.decisionesHabilitadas").value(false));
    }

    // ---- lista publica y resultado propio (CA10, CA18) ----

    @Test
    void listaPublica_antesDePublicar_da404ErrPub04() throws Exception {
        RequestPostProcessor sesion = bearer(login("2000099@prueba.test"));

        mockMvc.perform(get(rutaLista(convocatoria)).with(sesion))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PUB_04"));
        mockMvc.perform(get(rutaLista(convocatoria) + "/mio").with(sesion))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PUB_04"));
        mockMvc.perform(get("/api/v1/resultados/convocatorias").with(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void listaPublica_despuesDePublicar_traeSoloAdjudicadosConLosCincoCampos_paginadaYConSearch() throws Exception {
        publicar(convocatoria).andExpect(status().isCreated());
        esperarEnviadas(3);
        RequestPostProcessor sesion = bearer(login("2000099@prueba.test"));

        String cuerpo = mockMvc.perform(get(rutaLista(convocatoria)).with(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.data[0].apellidos").value("Uno"))
                .andExpect(jsonPath("$.data[0].nota").value(80.0))
                .andExpect(jsonPath("$.data[1].nombres").value("Beto"))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andReturn().getResponse().getContentAsString();
        Iterator<String> campos = JSON.readTree(cuerpo).get("data").get(0).fieldNames();
        List<String> nombres = new ArrayList<>();
        campos.forEachRemaining(nombres::add);
        assertThat(nombres).containsExactlyInAnyOrder("nombres", "apellidos", "nota", "carrera", "facultad");
        assertThat(JSON.readTree(cuerpo).get("data").get(0).get("carrera").asText()).isNotBlank();
        assertThat(JSON.readTree(cuerpo).get("data").get(0).get("facultad").asText()).isNotBlank();
        // Los no adjudicados, el CI y el correo no aparecen en ningun lado de la respuesta.
        assertThat(cuerpo).doesNotContain("Carla").doesNotContain("Dora").doesNotContain("2000001")
                .doesNotContain("@prueba.test").doesNotContain("motivo");

        mockMvc.perform(get(rutaLista(convocatoria)).with(sesion).param("pageSize", "1").param("page", "2"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombres").value("Beto"))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
        mockMvc.perform(get(rutaLista(convocatoria)).with(sesion).param("search", "beto"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombres").value("Beto"));
        mockMvc.perform(get(rutaLista(convocatoria)).with(sesion).param("sort", "-apellidos"))
                .andExpect(jsonPath("$.data[0].apellidos").value("Uno"));
        mockMvc.perform(get(rutaLista(convocatoria)).with(sesion).param("sort", "ci"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/resultados/convocatorias").with(sesion))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data[0].nombre").value("Conv PUBINT"))
                .andExpect(jsonPath("$.meta.totalItems").value(1));
    }

    @Test
    void mio_adjudicado_noAdjudicadoYUsuarioAjeno() throws Exception {
        publicar(convocatoria).andExpect(status().isCreated());
        esperarEnviadas(3);

        mockMvc.perform(get(rutaLista(convocatoria) + "/mio").with(bearer(login("2000001@prueba.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultado").value("ADJUDICADA"))
                .andExpect(jsonPath("$.data.nota").value(80.0))
                .andExpect(jsonPath("$.data.publicadoEn").exists());
        mockMvc.perform(get(rutaLista(convocatoria) + "/mio").with(bearer(login("2000003@prueba.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultado").value("NO_ADJUDICADA"))
                .andExpect(jsonPath("$.data.motivo").doesNotExist());
        mockMvc.perform(get(rutaLista(convocatoria) + "/mio").with(bearer(login("2000099@prueba.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultado").isEmpty())
                .andExpect(jsonPath("$.data.nota").isEmpty());
    }

    // ---- sin adjudicados (CA15) ----

    @Test
    void publicar_sinNingunAdjudicado_funcionaYCadaFinalistaRecibeNoAdjudicada() throws Exception {
        long otraId = fx.convocatoria("PUBCERO");
        UUID otra = codigoConvocatoria(otraId);
        long uno = finalista(otraId, "2000020", "Uno", "Cero", "no_adjudicada", "NO_ADJUDICADA", "30.00");
        long dos = finalista(otraId, "2000021", "Dos", "Cero", "no_adjudicada", "NO_ADJUDICADA", "20.00");

        publicar(otra).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.adjudicados").value(0))
                .andExpect(jsonPath("$.data.noAdjudicados").value(2))
                .andExpect(jsonPath("$.data.avisados").value(2));

        assertThat(avisosDe(uno)).hasSize(1).first().satisfies(a -> assertThat(a.get("mensaje"))
                .isEqualTo("Uno, tu postulación en Conv PUBCERO no fue adjudicada. Gracias por participar."));
        assertThat(avisosDe(dos)).hasSize(1);
        esperarEnviadas(2);
        mockMvc.perform(get(rutaLista(otra)).with(admin)).andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---- avisos por lotes, sin N+1 (CA17) ----

    @Test
    void publicar_conCincoOQuinientosFinalistas_hacelaMismaCantidadDeSentencias() throws Exception {
        long chica = fx.convocatoria("PUBN5");
        long grande = fx.convocatoria("PUBN500");
        for (int i = 0; i < 5; i++) {
            finalistaRapido(chica, i % 2 == 0 ? "ADJUDICADA" : "NO_ADJUDICADA");
        }
        for (int i = 0; i < 500; i++) {
            finalistaRapido(grande, i % 2 == 0 ? "ADJUDICADA" : "NO_ADJUDICADA");
        }

        ContadorDeSentenciasDataSource.reiniciar();
        publicar(codigoConvocatoria(chica)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.avisados").value(5));
        int conCinco = ContadorDeSentenciasDataSource.sentencias();
        ContadorDeSentenciasDataSource.reiniciar();
        publicar(codigoConvocatoria(grande)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.avisados").value(500));
        int conQuinientos = ContadorDeSentenciasDataSource.sentencias();

        assertThat(conCinco).isPositive();
        assertThat(conQuinientos).isEqualTo(conCinco);
        assertThat(contar("notificacion WHERE postulacion_id IN (SELECT id FROM postulacion WHERE convocatoria_id = "
                + grande + ")")).isEqualTo(500);
        verify(correoService, timeout(30000).times(505)).enviar(any(), any(), any());
    }

    // ---- comunicado a ADJUDICADOS (CU21) ----

    @Test
    void comunicadoAAdjudicados_llegaSoloAAdjudicados_conOSinPublicar() throws Exception {
        String cuerpo = "{\"titulo\":\"Entrega de carnets\",\"contenido\":\"Pasa por la DUBSS\","
                + "\"destinatarios\":\"ADJUDICADOS\",\"convocatoriaId\":\"%s\"}".formatted(convocatoria);

        mockMvc.perform(post("/api/v1/comunicados").with(admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.totalDestinatarios").value(2));

        assertThat(jdbc.queryForList("""
                SELECT u.nombres FROM notificacion n JOIN usuario u ON u.id = n.usuario_id
                WHERE n.comunicado_id IS NOT NULL ORDER BY u.nombres""", String.class)).containsExactly("Ana", "Beto");

        // Tambien despues de publicar (los adjudicados son los mismos).
        publicar(convocatoria).andExpect(status().isCreated());
        esperarEnviadas(3);
        mockMvc.perform(post("/api/v1/comunicados").with(admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.totalDestinatarios").value(2));
        // Sin la convocatoria no se sabe a quien: ERR_COM_02.
        mockMvc.perform(post("/api/v1/comunicados").with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"ADJUDICADOS\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_COM_02"));
    }

    // ---- permisos (CA13) ----

    @Test
    void unPostulante_da403EnPublicarResumenYDecisiones_yNadaSePublica() throws Exception {
        RequestPostProcessor postulante = bearer(login("2000099@prueba.test"));

        mockMvc.perform(post(rutaAdmin(convocatoria, "publicar")).with(postulante))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get(rutaAdmin(convocatoria, "resumen")).with(postulante))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get(rutaAdmin(convocatoria, "decisiones")).with(postulante))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));

        assertNadaSePublico();
    }

    @Test
    void sinSesion_todasLasRutasDan401() throws Exception {
        mockMvc.perform(post(rutaAdmin(convocatoria, "publicar"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaAdmin(convocatoria, "resumen"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaAdmin(convocatoria, "decisiones"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/resultados/convocatorias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaLista(convocatoria))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(rutaLista(convocatoria) + "/mio")).andExpect(status().isUnauthorized());

        assertNadaSePublico();
    }

    // ---- ayudas ----

    private ResultActions publicar(UUID codigo) throws Exception {
        return mockMvc.perform(post(rutaAdmin(codigo, "publicar")).with(admin));
    }

    private static String rutaAdmin(UUID codigo, String sufijo) {
        return "/api/v1/convocatorias/" + codigo + "/resultados/" + sufijo;
    }

    private static String rutaLista(UUID codigo) {
        return "/api/v1/convocatorias/" + codigo + "/resultados";
    }

    private UUID codigoConvocatoria(long id) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class, id);
    }

    /** El codigo publico del finalista de ese usuario. */
    private UUID codigoFinalista(long usuarioId) {
        return jdbc.queryForObject("""
                SELECT f.codigo_finalista FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.postulante_id = ?""", UUID.class, usuarioId);
    }

    private String estadoConvocatoria() {
        return jdbc.queryForObject("SELECT estado::text FROM convocatoria WHERE id = ?", String.class, convocatoriaId);
    }

    /** Nada se publico: ni fila, ni avisos, ni bitacora, y la convocatoria sigue en su estado. */
    private void assertNadaSePublico() {
        assertNadaSePublico("CERRADA");
    }

    private void assertNadaSePublico(String estado) {
        assertThat(contar("publicacion_resultado")).isZero();
        assertThat(contar("notificacion")).isZero();
        assertThat(contar("bitacora WHERE accion = 'PUBLICACION_RESULTADOS'")).isZero();
        assertThat(estadoConvocatoria()).isEqualTo(estado);
        verify(correoService, never()).enviar(any(), any(), any());
        verify(canalPush, never()).enviar(anyList(), any(MensajePush.class));
    }

    /** Un finalista (devuelve el id de su usuario). Con `resultado` no nulo, ademas su adjudicacion. */
    private long finalista(long convocatoriaDe, String ci, String nombres, String apellidos, String estado,
            String resultado, String puntaje) {
        long usuario = fx.postulante(ci);
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        long postulacion = fx.postulacion(convocatoriaDe, usuario, estado);
        fx.finalista(postulacion, 1);
        if (resultado != null) {
            fx.adjudicacion(postulacion, resultado, puntaje, adminId);
        }
        return usuario;
    }

    /** Igual pero sin tocar nombres: para los lotes grandes. */
    private void finalistaRapido(long convocatoriaDe, String resultado) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        long postulacion = fx.postulacion(convocatoriaDe, usuario,
                "ADJUDICADA".equals(resultado) ? "adjudicada" : "no_adjudicada");
        fx.finalista(postulacion, 1);
        fx.adjudicacion(postulacion, resultado, "75.00", adminId);
    }

    private String correoDe(long usuarioId) {
        return jdbc.queryForObject("SELECT correo FROM usuario WHERE id = ?", String.class, usuarioId);
    }

    private List<Map<String, Object>> avisosDe(long usuarioId) {
        return jdbc.queryForList("""
                SELECT titulo, mensaje, tipo::text AS tipo, postulacion_id, enviada_en
                FROM notificacion WHERE usuario_id = ? ORDER BY id""", usuarioId);
    }

    /** Espera a que el despacho asincrono marque `n` avisos como enviados (el push o el correo llegaron). */
    private void esperarEnviadas(int n) throws InterruptedException {
        long limite = System.currentTimeMillis() + 10_000;
        while (contar("notificacion WHERE enviada_en IS NOT NULL") < n && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
        }
        assertThat(contar("notificacion WHERE enviada_en IS NOT NULL")).isEqualTo(n);
    }

    private void crearConClave(String correo, String ci, String rol) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, 'Admin', 'Prueba' FROM rol WHERE nombre = ?""", ci, correo,
                passwordEncoder.encode("secreta1"), rol);
    }

    /** Le pone una contrasena conocida a un postulante de prueba para poder iniciar sesion con el. */
    private void conClave(String ci) {
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE ci = ?", passwordEncoder.encode("secreta1"), ci);
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
        jdbc.update("DELETE FROM publicacion_resultado");
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
        jdbc.update("DELETE FROM comunicado");
        jdbc.update("DELETE FROM puntaje_postulacion");
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM modelo_preseleccion");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
