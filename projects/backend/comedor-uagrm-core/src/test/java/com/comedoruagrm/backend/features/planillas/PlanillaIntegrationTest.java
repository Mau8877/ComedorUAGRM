package com.comedoruagrm.backend.features.planillas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/**
 * CU09 de punta a punta contra un Postgres real y el almacenamiento simulado en memoria: generar la planilla con
 * el padron vigente (fila, archivo, PDF y bitacora), los rechazos de periodo y de padron vacio, la lista, la
 * descarga, la vista previa sin huella, el rol y el hecho de que la planilla refleje el padron al generarla.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class PlanillaIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant NOVIEMBRE_2026 = Instant.parse("2026-11-16T15:00:00Z");

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

    /** Lo que se «sube» a MinIO: ruta -> bytes. */
    private final Map<String, byte[]> archivos = new ConcurrentHashMap<>();

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
        reset(almacenamiento, reloj);
        when(reloj.instant()).thenReturn(NOVIEMBRE_2026);
        doAnswer(i -> {
            archivos.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(almacenamiento).subir(anyString(), any(byte[].class), anyString());
        when(almacenamiento.existe(anyString())).thenAnswer(i -> archivos.containsKey((String) i.getArgument(0)));
        when(almacenamiento.urlTemporal(anyString(), any())).thenReturn("http://minio.test/firmada?firma=abc");

        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 2200100;
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '0000001', 'admin@dubss.test', ?, 'Admin', 'Prueba' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS'""", passwordEncoder.encode("secreta1"));
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        fx.postulante("2200099");
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE ci = '2200099'", passwordEncoder.encode("secreta1"));
        admin = bearer(login("admin@dubss.test"));
        postulante = bearer(login("2200099@prueba.test"));

        convocatoriaId = fx.convocatoria("PLAINT");
        convocatoria = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoriaId);
        beca("Ana", "Uno", "VIGENTE");
        beca("Beto", "Dos", "VIGENTE");
        beca("Carla", "Tres", "BAJA");
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- generar (flujo A) ----

    @Test
    void generar_conBecasVigentes_guardaLaPlanilla_subeElPdf_yAnotaLaBitacoraSinDatosSensibles() throws Exception {
        generar("2026-09-01", "2026-09-30").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Planilla generada correctamente"))
                .andExpect(jsonPath("$.data.convocatoria").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.fechaDesde").value("2026-09-01"))
                .andExpect(jsonPath("$.data.fechaHasta").value("2026-09-30"))
                .andExpect(jsonPath("$.data.totalBecados").value(2))
                .andExpect(jsonPath("$.data.generadaPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.storagePath").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());

        Map<String, Object> fila = jdbc.queryForMap("SELECT convocatoria_id, generada_por, total_becados, "
                + "storage_path FROM planilla_asistencia");
        assertThat(fila).containsEntry("convocatoria_id", convocatoriaId).containsEntry("generada_por", adminId)
                .containsEntry("total_becados", 2);
        String ruta = (String) fila.get("storage_path");
        assertThat(ruta).startsWith("planillas/" + convocatoria + "/planilla-2026-09-01-2026-09-30-");
        assertThat(archivos).containsOnlyKeys(ruta);

        String texto = textoDelPdf(archivos.get(ruta));
        assertThat(texto).contains("Conv PLAINT", "Becados vigentes: 2", "01/09/2026 al 30/09/2026",
                "Generada el", "Admin Prueba", "2200100", "2200101");
        // Por apellido: «Dos» va antes que «Uno». Carla (baja) no entra.
        assertThat(texto.indexOf("Dos Beto")).isPositive().isLessThan(texto.indexOf("Uno Ana"));
        assertThat(texto).doesNotContain("Tres Carla").doesNotContain("2200102");

        Map<String, Object> bitacora = jdbc.queryForMap("SELECT usuario_id, entidad, detalle::text AS detalle "
                + "FROM bitacora WHERE accion = 'GENERACION_PLANILLA'");
        assertThat(bitacora).containsEntry("usuario_id", adminId).containsEntry("entidad", "planilla_asistencia");
        assertThat((String) bitacora.get("detalle")).contains("2026-09-01", "2026-09-30")
                .doesNotContain("planillas/", "2200101", "Uno", "Ana");
        assertThat(contar("bitacora WHERE accion = 'GENERACION_PLANILLA'")).isEqualTo(1);
    }

    @Test
    void generar_noCambiaElPadron() throws Exception {
        String antes = instantanea("SELECT id || ':' || estado::text FROM beca ORDER BY id");

        generar("2026-09-01", "2026-09-30").andExpect(status().isCreated());

        assertThat(instantanea("SELECT id || ':' || estado::text FROM beca ORDER BY id")).isEqualTo(antes);
    }

    @Test
    void generar_laPlanillaReflejaElPadronAlMomentoDeGenerar() throws Exception {
        generar("2026-09-01", "2026-09-30").andExpect(status().isCreated());
        jdbc.update("UPDATE beca SET estado = CAST('BAJA' AS estado_beca) WHERE postulante_id = "
                + "(SELECT id FROM usuario WHERE ci = '2200101')");
        beca("Dina", "Cuatro", "VIGENTE");

        generar("2026-10-01", "2026-10-31").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalBecados").value(2));

        List<Integer> totales = jdbc.queryForList("SELECT total_becados FROM planilla_asistencia "
                + "ORDER BY fecha_desde", Integer.class);
        assertThat(totales).containsExactly(2, 2);
        String segundo = archivos.entrySet().stream().filter(e -> e.getKey().contains("2026-10-01"))
                .map(e -> textoDelPdfSinExcepcion(e.getValue())).findFirst().orElseThrow();
        assertThat(segundo).contains("Uno Ana", "Cuatro Dina").doesNotContain("Dos Beto");
        assertThat(archivos).hasSize(2);
    }

    @Test
    void generar_conPeriodoInvalido_da400ConSuCodigo_yNoGeneraNada() throws Exception {
        generar("2026-09-30", "2026-09-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed")).andExpect(jsonPath("$.error").value("ERR_PLA_01"));
        generar("2026-06-15", "2026-07-15").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_PLA_02"));
        generar("2026-12-01", "2027-01-05").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_PLA_02"));
        generar("2026-09-01", "2026-10-02").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_PLA_03"));

        assertNadaSeGenero();
    }

    @Test
    void generar_sinFechas_da400DeValidacion_yNoGeneraNada() throws Exception {
        mockMvc.perform(post(rutaPlanillas()).with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaDesde\":\"2026-09-01\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        assertNadaSeGenero();
    }

    @Test
    void generar_sinBecasVigentes_da409ErrPla04_yNoGeneraNada() throws Exception {
        jdbc.update("UPDATE beca SET estado = CAST('SUSPENDIDA' AS estado_beca)");

        generar("2026-09-01", "2026-09-30").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_PLA_04"));

        assertNadaSeGenero();
    }

    @Test
    void generar_convocatoriaInexistente_da404ErrPri01() throws Exception {
        mockMvc.perform(post("/api/v1/convocatorias/" + UUID.randomUUID() + "/planillas").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaDesde\":\"2026-09-01\",\"fechaHasta\":\"2026-09-30\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    // ---- consultar, descargar (flujo B) y resumen ----

    @Test
    void resumen_diceCuantosBecadosEntranYLosLimitesDeLaGestion() throws Exception {
        mockMvc.perform(get(rutaPlanillas() + "/resumen").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.becadosVigentes").value(2))
                .andExpect(jsonPath("$.data.gestion").value("G-PLAINT"))
                .andExpect(jsonPath("$.data.gestionDesde").value("2026-07-01"))
                .andExpect(jsonPath("$.data.gestionHasta").value("2026-12-31"));
    }

    @Test
    void listar_devuelveLasPlanillasDeLaConvocatoriaConMeta_yOrdena() throws Exception {
        generar("2026-09-01", "2026-09-30").andExpect(status().isCreated());
        generar("2026-08-01", "2026-08-31").andExpect(status().isCreated());

        mockMvc.perform(get(rutaPlanillas()).with(admin).param("sort", "fechaDesde")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fechaDesde").value("2026-08-01"))
                .andExpect(jsonPath("$.data[1].fechaDesde").value("2026-09-01"))
                .andExpect(jsonPath("$.data[0].totalBecados").value(2))
                .andExpect(jsonPath("$.data[0].storagePath").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(2)).andExpect(jsonPath("$.meta.page").value(1));
        mockMvc.perform(get(rutaPlanillas()).with(admin).param("sort", "storagePath"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(rutaPlanillas()).with(admin).param("filter[x]", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void descargar_daUnaDireccionTemporal_yAnotaLaDescargaEnLaBitacora() throws Exception {
        generar("2026-09-01", "2026-09-30").andExpect(status().isCreated());
        UUID planilla = UUID.fromString(jdbc.queryForObject("SELECT codigo_planilla_asistencia::text "
                + "FROM planilla_asistencia", String.class));

        mockMvc.perform(get("/api/v1/planillas/" + planilla + "/descarga").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://minio.test/firmada?firma=abc"))
                .andExpect(jsonPath("$.data.expiraEn").exists())
                .andExpect(jsonPath("$.data.storagePath").doesNotExist());

        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = 'DESCARGA_PLANILLA'",
                String.class);
        assertThat(detalle).contains(planilla.toString()).doesNotContain("planillas/", "minio", "firma");
    }

    @Test
    void descargar_inexistente_da404ErrPla05_yConArchivoFaltanteErrPla06SinBitacora() throws Exception {
        mockMvc.perform(get("/api/v1/planillas/" + UUID.randomUUID() + "/descarga").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PLA_05"));

        generar("2026-09-01", "2026-09-30").andExpect(status().isCreated());
        archivos.clear();
        UUID planilla = UUID.fromString(jdbc.queryForObject("SELECT codigo_planilla_asistencia::text "
                + "FROM planilla_asistencia", String.class));
        mockMvc.perform(get("/api/v1/planillas/" + planilla + "/descarga").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PLA_06"));

        assertThat(contar("bitacora WHERE accion = 'DESCARGA_PLANILLA'")).isZero();
    }

    // ---- vista previa ----

    @Test
    void vistaPrevia_devuelveElPdfEnBorrador_sinSubirNiGuardarNiAnotarNada() throws Exception {
        byte[] pdf = mockMvc.perform(get(rutaPlanillas() + "/vista-previa").with(admin).param("desde", "2026-09-01")
                        .param("hasta", "2026-09-30"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(textoDelPdf(pdf)).contains("BORRADOR", "Becados vigentes: 2");
        assertNadaSeGenero();
        assertThat(contar("bitacora WHERE accion LIKE '%PLANILLA%'")).isZero();

        mockMvc.perform(get(rutaPlanillas() + "/vista-previa").with(admin).param("desde", "2026-09-30")
                        .param("hasta", "2026-09-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PLA_01"));
    }

    // ---- rol, sesion y verbos ----

    @Test
    void unPostulante_recibe403ErrAuth07_enTodosLosEndpoints() throws Exception {
        for (var peticion : peticiones()) {
            mockMvc.perform(peticion.with(postulante)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
        assertNadaSeGenero();
    }

    @Test
    void sinSesion_recibe401_enTodosLosEndpoints() throws Exception {
        for (var peticion : peticiones()) {
            mockMvc.perform(peticion).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void unaPlanillaNoSeEditaNiSeElimina_405() throws Exception {
        mockMvc.perform(put(rutaPlanillas()).with(admin)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch(rutaPlanillas()).with(admin)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(rutaPlanillas()).with(admin)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/planillas/" + UUID.randomUUID()).with(admin))
                .andExpect(status().is4xxClientError());
    }

    private List<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> peticiones() {
        String cuerpo = "{\"fechaDesde\":\"2026-09-01\",\"fechaHasta\":\"2026-09-30\"}";
        return List.of(get(rutaPlanillas()), get(rutaPlanillas() + "/resumen"),
                get(rutaPlanillas() + "/vista-previa").param("desde", "2026-09-01").param("hasta", "2026-09-30"),
                post(rutaPlanillas()).contentType(MediaType.APPLICATION_JSON).content(cuerpo),
                get("/api/v1/planillas/" + UUID.randomUUID() + "/descarga"));
    }

    // ---- utilidades ----

    private String rutaPlanillas() {
        return "/api/v1/convocatorias/" + convocatoria + "/planillas";
    }

    private ResultActions generar(String desde, String hasta) throws Exception {
        return mockMvc.perform(post(rutaPlanillas()).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaDesde\":\"%s\",\"fechaHasta\":\"%s\"}".formatted(desde, hasta)));
    }

    private void assertNadaSeGenero() {
        assertThat(contar("planilla_asistencia")).isZero();
        assertThat(archivos).isEmpty();
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

    private static String textoDelPdfSinExcepcion(byte[] pdf) {
        try {
            return textoDelPdf(pdf);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Un postulante nuevo con su beca en el estado dado, en la convocatoria de la prueba. */
    private void beca(String nombres, String apellidos, String estado) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        jdbc.update("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), DATE '2026-09-01')""", convocatoriaId, usuario, estado);
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
        return String.join("|", jdbc.queryForList(consulta, String.class));
    }

    private long contar(String tablaYCondicion) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM " + tablaYCondicion, Long.class);
        return total == null ? 0 : total;
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues sobre la misma base (un solo
    // Postgres por JVM) no deben ver lo que estos flujos commitean.
    private void borrarTodo() {
        jdbc.update("DELETE FROM planilla_asistencia");
        jdbc.update("DELETE FROM movimiento_beca");
        jdbc.update("DELETE FROM beca");
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
