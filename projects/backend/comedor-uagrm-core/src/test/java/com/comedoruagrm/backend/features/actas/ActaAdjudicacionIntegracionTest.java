package com.comedoruagrm.backend.features.actas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CorreoNotificacionService;
import com.comedoruagrm.backend.features.notificaciones.service.MensajePush;
import com.comedoruagrm.backend.features.notificaciones.service.ResultadoPush;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/**
 * CU08 de punta a punta contra un Postgres real y el almacenamiento simulado en memoria: generar el acta con los
 * resultados publicados (fila, numero, archivo, PDF y bitacora), los rechazos (409/404/403/401/405), la doble
 * peticion y la concurrencia, el correlativo por año, la vista previa sin huella, el listado, la consulta y la
 * descarga, y que nada mas cambie (estados, avisos, la publicacion del CU22).
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class ActaAdjudicacionIntegracionTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant NOVIEMBRE_2026 = Instant.parse("2026-11-13T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AlmacenamientoService almacenamiento;

    @MockitoBean
    private Clock reloj;

    @MockitoBean
    private CanalPush canalPush;

    @MockitoBean
    private CorreoNotificacionService correoService;

    /** Lo que se «sube» a MinIO: ruta -> bytes. */
    private final Map<String, byte[]> archivos = new ConcurrentHashMap<>();

    private volatile Instant ahora;
    private ExpedienteFixtures fx;
    private RequestPostProcessor admin;
    private RequestPostProcessor postulante;
    private long adminId;
    private long convocatoriaId;
    private UUID convocatoria;
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
    void preparar() throws Exception {
        borrarTodo();
        archivos.clear();
        ahora = NOVIEMBRE_2026;
        reset(almacenamiento, reloj, canalPush, correoService);
        when(reloj.instant()).thenAnswer(i -> ahora);
        doAnswer(i -> {
            archivos.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(almacenamiento).subir(anyString(), any(byte[].class), anyString());
        when(almacenamiento.existe(anyString())).thenAnswer(i -> archivos.containsKey((String) i.getArgument(0)));
        when(almacenamiento.urlTemporal(anyString(), any())).thenReturn("http://minio.test/firmada?firma=abc");
        when(correoService.enviar(any(), any(), any())).thenReturn(true);
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenAnswer(i -> new ResultadoPush(((List<?>) i.getArgument(0)).size(), List.of(), List.of()));

        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 2100100;
        crearConClave("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS");
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        fx.postulante("2100099");
        conClave("2100099");
        admin = bearer(login("admin@dubss.test"));
        postulante = bearer(login("2100099@prueba.test"));

        convocatoriaId = convocatoriaPublicada("ACTINT", NOVIEMBRE_2026.minusSeconds(3600));
        convocatoria = codigoConvocatoria(convocatoriaId);
        // Orden esperado en el acta: Beto y Carla (90.00, por posicion), Ana (80.00). Dora y Elsa no entran.
        finalista(convocatoriaId, "Ana", "Uno", 3, "adjudicada", "ADJUDICADA", "80.00");
        finalista(convocatoriaId, "Beto", "Dos", 1, "adjudicada", "ADJUDICADA", "90.00");
        finalista(convocatoriaId, "Carla", "Tres", 2, "adjudicada", "ADJUDICADA", "90.00");
        finalista(convocatoriaId, "Dora", "Cuatro", 4, "no_adjudicada", "NO_ADJUDICADA", "99.00");
        finalista(convocatoriaId, "Elsa", "Cinco", 5, "calificada", null, null);
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- generar con resultados publicados (CA01, CA06, CA07, CA08, CA16, CA18) ----

    @Test
    void generar_conResultadosPublicados_guardaElActa_subeElPdf_yAnotaLaBitacoraSinDatosSensibles()
            throws Exception {
        String estadosAntes = instantanea("SELECT id || ':' || estado::text FROM postulacion ORDER BY id");
        String adjudicacionesAntes = instantanea("SELECT id || ':' || resultado::text || ':' || puntaje_final "
                + "FROM adjudicacion ORDER BY id");

        generar(convocatoria).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Acta generada correctamente"))
                .andExpect(jsonPath("$.data.numero").value("001/2026"))
                .andExpect(jsonPath("$.data.convocatoria.codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.convocatoria.nombre").value("Conv ACTINT"))
                .andExpect(jsonPath("$.data.gestion").value("G-ACTINT"))
                .andExpect(jsonPath("$.data.adjudicados").value(3))
                .andExpect(jsonPath("$.data.generadaPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.storagePath").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());

        Map<String, Object> fila = jdbc.queryForMap("SELECT convocatoria_id, numero, storage_path, generada_por "
                + "FROM acta_adjudicacion");
        assertThat(fila).containsEntry("convocatoria_id", convocatoriaId).containsEntry("numero", "001/2026")
                .containsEntry("generada_por", adminId);
        String ruta = "actas/" + convocatoria + "/acta-001-2026.pdf";
        assertThat(fila.get("storage_path")).isEqualTo(ruta);
        assertThat(archivos).containsOnlyKeys(ruta);

        String texto = textoDelPdf(archivos.get(ruta));
        assertThat(texto).contains("001/2026").contains("Conv ACTINT").contains("G-ACTINT")
                .contains("Adjudicados: 3 de").contains("Generada por: Admin Prueba");
        assertThat(texto.indexOf("Beto Dos")).isPositive().isLessThan(texto.indexOf("Carla Tres"));
        assertThat(texto.indexOf("Carla Tres")).isLessThan(texto.indexOf("Ana Uno"));
        assertThat(texto).contains("2100101").contains("90,00").contains("80,00");
        // Solo adjudicados: ni la no adjudicada ni la finalista sin decision.
        assertThat(texto).doesNotContain("Dora").doesNotContain("Elsa").doesNotContain("@");

        Map<String, Object> bitacora = jdbc.queryForMap(
                "SELECT usuario_id, entidad, entidad_id, detalle::text AS detalle FROM bitacora "
                        + "WHERE accion = 'GENERACION_ACTA'");
        assertThat(bitacora).containsEntry("usuario_id", adminId).containsEntry("entidad", "acta_adjudicacion");
        assertThat(bitacora.get("entidad_id")).isEqualTo(jdbc.queryForObject("SELECT id FROM acta_adjudicacion",
                Long.class));
        String detalle = (String) bitacora.get("detalle");
        assertThat(detalle).contains("\"numero\": \"001/2026\"").contains("\"adjudicados\": 3")
                .contains(convocatoria.toString());
        assertThat(detalle).doesNotContain("Ana", "Beto", "Carla", "Uno", "Dos", "2100101", "actas/", "http", "@");

        // Nada mas cambia: ni postulaciones, ni adjudicaciones, ni la convocatoria, ni avisos.
        assertThat(instantanea("SELECT id || ':' || estado::text FROM postulacion ORDER BY id"))
                .isEqualTo(estadosAntes);
        assertThat(instantanea("SELECT id || ':' || resultado::text || ':' || puntaje_final FROM adjudicacion "
                + "ORDER BY id")).isEqualTo(adjudicacionesAntes);
        assertThat(estadoConvocatoria()).isEqualTo("PUBLICADA");
        assertThat(contar("notificacion")).isZero();
    }

    @Test
    void generar_sinAdjudicados_generaIgualConLaLeyendaSinAdjudicados() throws Exception {
        long vacia = convocatoriaPublicada("ACTVAC", NOVIEMBRE_2026.minusSeconds(60));
        finalista(vacia, "Zoe", "Sola", 1, "no_adjudicada", "NO_ADJUDICADA", "50.00");

        generar(codigoConvocatoria(vacia)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.adjudicados").value(0));

        String texto = textoDelPdf(archivos.values().iterator().next());
        assertThat(texto).contains("Sin adjudicados").doesNotContain("Zoe");
    }

    // ---- rechazos de negocio (CA02, CA03, CA04, CA19) ----

    @Test
    void generar_sinPublicar_da409ErrAct01_yNoGuardaNiSubeNada() throws Exception {
        jdbc.update("DELETE FROM publicacion_resultado");

        generar(convocatoria).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_ACT_01"));

        assertNadaSeGenero();
    }

    @Test
    void generar_conUnaConvocatoriaInexistente_da404ErrPri01() throws Exception {
        generar(UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_PRI_01"));

        assertNadaSeGenero();
    }

    @Test
    void generar_dosVeces_laSegundaDa409ErrAct02_yQuedaUnaSolaFilaYUnSoloArchivo() throws Exception {
        generar(convocatoria).andExpect(status().isCreated());

        generar(convocatoria).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_ACT_02"));

        assertThat(contar("acta_adjudicacion")).isEqualTo(1);
        assertThat(archivos).hasSize(1);
        assertThat(contar("bitacora WHERE accion = 'GENERACION_ACTA'")).isEqualTo(1);
    }

    @Test
    void generar_dosPeticionesSimultaneasDeLaMismaConvocatoria_unaCreaYLaOtraRecibe409() throws Exception {
        List<MvcResult> resultados = enParalelo(convocatoria, convocatoria);

        List<Integer> estados = resultados.stream().map(r -> r.getResponse().getStatus()).sorted().toList();
        assertThat(estados).containsExactly(201, 409);
        MvcResult rechazada = resultados.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst()
                .orElseThrow();
        assertThat(JSON.readTree(rechazada.getResponse().getContentAsString()).get("error").asText())
                .isEqualTo("ERR_ACT_02");
        assertThat(contar("acta_adjudicacion")).isEqualTo(1);
        assertThat(archivos).hasSize(1);
    }

    @Test
    void generar_dosConvocatoriasALaVez_recibenNumerosDistintosYConsecutivos() throws Exception {
        long otra = convocatoriaPublicada("ACTOTRA", NOVIEMBRE_2026.minusSeconds(30));
        finalista(otra, "Hugo", "Otro", 1, "adjudicada", "ADJUDICADA", "70.00");

        List<MvcResult> resultados = enParalelo(convocatoria, codigoConvocatoria(otra));

        assertThat(resultados).allSatisfy(r -> assertThat(r.getResponse().getStatus()).isEqualTo(201));
        List<String> numeros = new ArrayList<>();
        for (MvcResult r : resultados) {
            numeros.add(JSON.readTree(r.getResponse().getContentAsString()).get("data").get("numero").asText());
        }
        assertThat(numeros).containsExactlyInAnyOrder("001/2026", "002/2026");
        assertThat(contar("acta_adjudicacion")).isEqualTo(2);
    }

    @Test
    void generar_enUnAnioNuevo_elCorrelativoReiniciaEn001() throws Exception {
        long otra = convocatoriaPublicada("ACTAN", NOVIEMBRE_2026.minusSeconds(30));
        generar(convocatoria).andExpect(status().isCreated()).andExpect(jsonPath("$.data.numero").value("001/2026"));

        // 03:00 UTC del 5 de enero son las 23:00 del 4 de enero en La Paz: ya es 2027.
        ahora = Instant.parse("2027-01-05T03:00:00Z");
        generar(codigoConvocatoria(otra)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value("001/2027"));

        assertThat(jdbc.queryForList("SELECT numero FROM acta_adjudicacion ORDER BY numero", String.class))
                .containsExactly("001/2026", "001/2027");
    }

    // ---- fallas de infraestructura (CA12) ----

    @Test
    void generar_siFallaMinio_da500Generico_yNoQuedaFilaNiBitacora() throws Exception {
        doThrow(new AlmacenamientoException("MinIO caído", null)).when(almacenamiento)
                .subir(anyString(), any(byte[].class), anyString());

        generar(convocatoria).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_00"));

        assertNadaSeGenero();
    }

    @Test
    void generar_despuesDeUnFalloDeMinio_alReintentarGeneraConElMismoNumero() throws Exception {
        doThrow(new AlmacenamientoException("MinIO caído", null)).when(almacenamiento)
                .subir(anyString(), any(byte[].class), anyString());
        generar(convocatoria).andExpect(status().isInternalServerError());
        doAnswer(i -> {
            archivos.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(almacenamiento).subir(anyString(), any(byte[].class), anyString());

        generar(convocatoria).andExpect(status().isCreated()).andExpect(jsonPath("$.data.numero").value("001/2026"));
    }

    // ---- vista previa (CA10) ----

    @Test
    void vistaPrevia_devuelveElPdfBorrador_sinFilaArchivoNumeroNiBitacora() throws Exception {
        MvcResult resultado = mockMvc.perform(get(rutaActa(convocatoria) + "/vista-previa").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String texto = textoDelPdf(resultado.getResponse().getContentAsByteArray());
        assertThat(texto).contains("BORRADOR").contains("N.º borrador").contains("Beto Dos");
        assertThat(contar("acta_adjudicacion")).isZero();
        assertThat(archivos).isEmpty();
        assertThat(contar("bitacora WHERE accion IN ('GENERACION_ACTA', 'DESCARGA_ACTA')")).isZero();

        // No consumio numero: la primera acta real sigue siendo la 001.
        generar(convocatoria).andExpect(status().isCreated()).andExpect(jsonPath("$.data.numero").value("001/2026"));
    }

    @Test
    void vistaPrevia_conActaExistenteOSinPublicar_da409() throws Exception {
        generar(convocatoria).andExpect(status().isCreated());
        mockMvc.perform(get(rutaActa(convocatoria) + "/vista-previa").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACT_02"));

        long sinPublicar = fx.convocatoria("ACTSP");
        mockMvc.perform(get(rutaActa(codigoConvocatoria(sinPublicar)) + "/vista-previa").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACT_01"));
    }

    // ---- listado y pendientes (CA17) ----

    @Test
    void listar_paginaBuscaYOrdena_yUnSortInvalidoDa400() throws Exception {
        generar(convocatoria).andExpect(status().isCreated());
        long beta = convocatoriaPublicada("BETA", NOVIEMBRE_2026.minusSeconds(100));
        long gamma = convocatoriaPublicada("GAMMA", NOVIEMBRE_2026.minusSeconds(90));
        insertarActa(beta, "002/2026", "2026-11-11T10:00:00Z");
        insertarActa(gamma, "003/2026", "2026-11-12T10:00:00Z");

        mockMvc.perform(get("/api/v1/actas").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].numero").value("001/2026"))
                .andExpect(jsonPath("$.data[0].adjudicados").value(3))
                .andExpect(jsonPath("$.data[0].storagePath").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(3))
                .andExpect(jsonPath("$.meta.pageSize").value(20));
        mockMvc.perform(get("/api/v1/actas").with(admin).param("sort", "numero").param("pageSize", "2"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].numero").value("002/2026"))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
        mockMvc.perform(get("/api/v1/actas").with(admin).param("search", "gamma"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].numero").value("003/2026"));
        mockMvc.perform(get("/api/v1/actas").with(admin).param("sort", "storagePath"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/actas").with(admin).param("filter[numero]", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void pendientes_soloTraeLasPublicadasSinActa() throws Exception {
        long sinPublicar = fx.convocatoria("ACTNOPUB");
        finalista(sinPublicar, "Nadie", "Publicado", 1, "adjudicada", "ADJUDICADA", "70.00");
        long conActa = convocatoriaPublicada("ACTCONACTA", NOVIEMBRE_2026.minusSeconds(10));
        insertarActa(conActa, "009/2026", "2026-11-11T10:00:00Z");

        mockMvc.perform(get("/api/v1/actas/pendientes").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].codigo").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data[0].nombre").value("Conv ACTINT"))
                .andExpect(jsonPath("$.data[0].adjudicados").value(3))
                .andExpect(jsonPath("$.meta.totalItems").value(1));

        generar(convocatoria).andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/actas/pendientes").with(admin))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---- consultar y descargar (CA13, CA14, CA15, CA16) ----

    @Test
    void acta_sinActa_da404ErrAct03_yConActaLaDevuelve() throws Exception {
        mockMvc.perform(get(rutaActa(convocatoria)).with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACT_03"));
        mockMvc.perform(get(rutaActa(UUID.randomUUID())).with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));

        generar(convocatoria).andExpect(status().isCreated());
        mockMvc.perform(get(rutaActa(convocatoria)).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numero").value("001/2026"))
                .andExpect(jsonPath("$.data.url").doesNotExist());
        assertThat(contar("bitacora WHERE accion = 'DESCARGA_ACTA'")).isZero();
    }

    @Test
    void descarga_devuelveLaDireccionTemporal_anotaLaDescarga_yNoIncluyeLaRuta() throws Exception {
        generar(convocatoria).andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get(rutaActa(convocatoria) + "/descarga").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://minio.test/firmada?firma=abc"))
                .andExpect(jsonPath("$.data.expiraEn").exists())
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("actas/");
        Map<String, Object> bitacora = jdbc.queryForMap("SELECT usuario_id, entidad, detalle::text AS detalle "
                + "FROM bitacora WHERE accion = 'DESCARGA_ACTA'");
        assertThat(bitacora).containsEntry("usuario_id", adminId).containsEntry("entidad", "acta_adjudicacion");
        assertThat((String) bitacora.get("detalle")).contains(convocatoria.toString())
                .doesNotContain("actas/", "http", "firma", "Beto", "2100101");
    }

    @Test
    void descarga_sinActa_da404ErrAct03_yConElArchivoAusenteDa404ErrAct04SinAnotar() throws Exception {
        mockMvc.perform(get(rutaActa(convocatoria) + "/descarga").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACT_03"));

        generar(convocatoria).andExpect(status().isCreated());
        archivos.clear();
        mockMvc.perform(get(rutaActa(convocatoria) + "/descarga").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACT_04"));

        assertThat(contar("bitacora WHERE accion = 'DESCARGA_ACTA'")).isZero();
    }

    // ---- no rompe el CU22 y el acta no se modifica (CA20) ----

    @Test
    void publicarResultados_sigueFuncionandoSinActa() throws Exception {
        long cerrada = fx.convocatoriaConCupos("ACTPUB", 10, "60.00", 5);
        finalista(cerrada, "Pia", "Nueva", 1, "adjudicada", "ADJUDICADA", "75.00");

        mockMvc.perform(post("/api/v1/convocatorias/" + codigoConvocatoria(cerrada) + "/resultados/publicar")
                        .with(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.convocatoria.estado").value("PUBLICADA"));

        assertThat(contar("acta_adjudicacion")).isZero();
    }

    @Test
    void putPatchYDeleteSobreElActa_dan405() throws Exception {
        generar(convocatoria).andExpect(status().isCreated());

        mockMvc.perform(put(rutaActa(convocatoria)).with(admin)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch(rutaActa(convocatoria)).with(admin)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(rutaActa(convocatoria)).with(admin)).andExpect(status().isMethodNotAllowed());

        assertThat(contar("acta_adjudicacion")).isEqualTo(1);
    }

    // ---- permisos (CA19) ----

    @Test
    void unPostulante_da403EnTodasLasRutas_yNoSeGeneraNada() throws Exception {
        for (MockHttpServletRequestBuilder peticion : peticiones()) {
            mockMvc.perform(peticion.with(postulante))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }

        assertNadaSeGenero();
    }

    @Test
    void sinSesion_todasLasRutasDan401() throws Exception {
        for (MockHttpServletRequestBuilder peticion : peticiones()) {
            mockMvc.perform(peticion).andExpect(status().isUnauthorized());
        }

        assertNadaSeGenero();
    }

    private List<MockHttpServletRequestBuilder> peticiones() {
        return List.of(get("/api/v1/actas"), get("/api/v1/actas/pendientes"), get(rutaActa(convocatoria)),
                get(rutaActa(convocatoria) + "/descarga"), get(rutaActa(convocatoria) + "/vista-previa"),
                post(rutaActa(convocatoria)));
    }

    // ---- ayudas ----

    private ResultActions generar(UUID codigo) throws Exception {
        return mockMvc.perform(post(rutaActa(codigo)).with(admin));
    }

    private static String rutaActa(UUID codigo) {
        return "/api/v1/convocatorias/" + codigo + "/acta";
    }

    /** Dispara una generacion por convocatoria a la vez (todas arrancan juntas) y espera sus respuestas. */
    private List<MvcResult> enParalelo(UUID... codigos) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(codigos.length);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<MvcResult>> futuros = new ArrayList<>();
            for (UUID codigo : codigos) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return mockMvc.perform(post(rutaActa(codigo)).with(admin)).andReturn();
                }));
            }
            salida.countDown();
            List<MvcResult> resultados = new ArrayList<>();
            for (Future<MvcResult> futuro : futuros) {
                resultados.add(futuro.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    /** No se genero nada: ni fila, ni archivo, ni bitacora, y la convocatoria sigue publicada. */
    private void assertNadaSeGenero() {
        assertThat(contar("acta_adjudicacion")).isZero();
        assertThat(archivos).isEmpty();
        assertThat(contar("bitacora WHERE accion IN ('GENERACION_ACTA', 'DESCARGA_ACTA')")).isZero();
    }

    private static String textoDelPdf(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder todo = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                todo.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return todo.toString();
        } finally {
            lector.close();
        }
    }

    /** Una convocatoria con cupos y minimo, cerrada y con sus resultados publicados en `publicadoEn`. */
    private long convocatoriaPublicada(String etiqueta, Instant publicadoEn) {
        long id = fx.convocatoriaConCupos(etiqueta, 50, "60.00", 5);
        fx.estadoConvocatoria(id, "PUBLICADA");
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en) "
                + "VALUES (?, ?, CAST(? AS timestamptz))", id, adminId, publicadoEn.toString());
        return id;
    }

    private void insertarActa(long convocatoria, String numero, String generadaEn) {
        jdbc.update("INSERT INTO acta_adjudicacion (convocatoria_id, numero, storage_path, generada_por, "
                + "generada_en) VALUES (?, ?, 'actas/x.pdf', ?, CAST(? AS timestamptz))", convocatoria, numero,
                adminId, generadaEn);
    }

    private UUID codigoConvocatoria(long id) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class, id);
    }

    private String estadoConvocatoria() {
        return jdbc.queryForObject("SELECT estado::text FROM convocatoria WHERE id = ?", String.class, convocatoriaId);
    }

    /** Un finalista; con `resultado` no nulo, ademas su adjudicacion. El CI sale de un contador desde 2100100. */
    private void finalista(long convocatoriaDe, String nombres, String apellidos, int posicion, String estado,
            String resultado, String puntaje) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        long postulacion = fx.postulacion(convocatoriaDe, usuario, estado);
        fx.finalista(postulacion, posicion);
        if (resultado != null) {
            fx.adjudicacion(postulacion, resultado, puntaje, adminId);
        }
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
        jdbc.update("DELETE FROM acta_adjudicacion");
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
