package com.comedoruagrm.backend.features.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.features.dashboard.service.RefrescoIndicadoresService;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU12 de punta a punta contra un Postgres real: los indicadores del Directivo salen de la vista (gestion vigente,
 * filtros, sin datos, borradores y resultados sin publicar que no cuentan, asistencia sin datos), el PDF, que
 * consultar no escribe nada, los permisos (403/401/404), el rendimiento con 20 000 postulaciones y el refresco.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class DashboardIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BASE = "/api/v1/dashboard";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefrescoIndicadoresService refresco;

    private ExpedienteFixtures fx;
    private RequestPostProcessor directivo;
    private RequestPostProcessor admin;
    private RequestPostProcessor postulante;
    private long adminId;
    private long gestionVigente;
    private long convocatoriaA;
    private long convocatoriaB;
    private long gestionVieja;
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
        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 3100100;
        crearConClave("directivo@dubss.test", "0000002", "DIRECTIVO");
        crearConClave("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS");
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        fx.postulante("3100099");
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE ci = '3100099'", passwordEncoder.encode("secreta1"));
        directivo = bearer(login("directivo@dubss.test"));
        admin = bearer(login("admin@dubss.test"));
        postulante = bearer(login("3100099@prueba.test"));

        // Gestion vigente (contiene hoy) con dos convocatorias; otra gestion, vieja y sin datos.
        gestionVigente = gestion("GI-VIG", "current_date - 30", "current_date + 30");
        gestionVieja = gestion("GI-OLD", "DATE '2020-01-01'", "DATE '2020-06-30'");
        convocatoriaA = convocatoria(gestionVigente, "Conv A", "PUBLICADA");
        convocatoriaB = convocatoria(gestionVigente, "Conv B", "CERRADA");

        // A: 3 enviadas (dos en septiembre, una en octubre), un borrador, 2 finalistas, 1 adjudicada y publicada.
        long a1 = enviada(convocatoriaA, "2026-09-10T15:00:00Z");
        enviada(convocatoriaA, "2026-09-20T15:00:00Z");
        long a3 = enviada(convocatoriaA, "2026-10-05T15:00:00Z");
        fx.postulacion(convocatoriaA, fx.postulante(String.valueOf(siguienteCi++)), "borrador");
        fx.finalista(a1, 1);
        fx.finalista(a3, 2);
        adjudicar(a1, "ADJUDICADA", "2026-10-20T15:00:00Z");
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)",
                convocatoriaA, adminId);

        // B: 2 enviadas en octubre, 1 finalista adjudicada pero SIN publicar.
        long b1 = enviada(convocatoriaB, "2026-10-08T15:00:00Z");
        enviada(convocatoriaB, "2026-10-09T15:00:00Z");
        fx.finalista(b1, 1);
        adjudicar(b1, "ADJUDICADA", "2026-10-21T15:00:00Z");

        // Los finalistas se crean en octubre y, el de B, en diciembre (deja noviembre vacio en la serie).
        jdbc.update("UPDATE finalista SET creado_en = CAST('2026-10-15T15:00:00Z' AS timestamptz)");
        jdbc.update("UPDATE finalista SET creado_en = CAST('2026-12-03T15:00:00Z' AS timestamptz) WHERE postulacion_id = ?", b1);

        assertThat(refresco.refrescar()).isTrue();
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
        // La vista guarda lo que vio la ultima vez: se refresca vacia para que otros tests no la vean sucia.
        jdbc.execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
    }

    // ---- CA01, CA05: gestion vigente y lote ----

    @Test
    void sinFiltros_muestraLaGestionVigenteConSusTotalesYLaFechaDeActualizacion() throws Exception {
        consultar("").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.gestion.etiqueta").value("GI-VIG"))
                .andExpect(jsonPath("$.data.convocatoria").doesNotExist())
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(5))
                .andExpect(jsonPath("$.data.totales.finalistas").value(3))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(1))
                .andExpect(jsonPath("$.data.actualizadoEn").isNotEmpty())
                .andExpect(jsonPath("$.data.sinDatos").value(false));
    }

    @Test
    void laSerieMensual_vaDeSeptiembreADiciembreSinHuecos() throws Exception {
        JsonNode data = datos(consultar("").andExpect(status().isOk()));

        List<String> meses = new java.util.ArrayList<>();
        data.get("porMes").forEach(m -> meses.add(m.get("mes").asText()));
        assertThat(meses).containsExactly("2026-09", "2026-10", "2026-11", "2026-12");
        assertThat(data.get("porMes").get(0).get("postulaciones").asInt()).isEqualTo(2);
        assertThat(data.get("porMes").get(1).get("postulaciones").asInt()).isEqualTo(3);
        assertThat(data.get("porMes").get(1).get("adjudicadas").asInt()).isEqualTo(1);
        assertThat(data.get("porMes").get(2).get("postulaciones").asInt()).isZero();
        assertThat(data.get("porMes").get(3).get("finalistas").asInt()).isEqualTo(1);
    }

    @Test
    void sinRefrescarLaVista_losDatosNuevosNoAparecen_yDespuesDelLoteSi() throws Exception {
        enviada(convocatoriaA, "2026-10-12T15:00:00Z");

        consultar("").andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(5));

        assertThat(refresco.refrescar()).isTrue();
        consultar("").andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(6));
    }

    // ---- CA02, CA03: filtros ----

    @Test
    void filtrarPorConvocatoria_cambiaLosTotalesYLaSerie() throws Exception {
        consultar("?filter[convocatoria]=" + codigoConvocatoria(convocatoriaA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.convocatoria.nombre").value("Conv A"))
                .andExpect(jsonPath("$.data.gestion.etiqueta").value("GI-VIG"))
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(3))
                .andExpect(jsonPath("$.data.totales.finalistas").value(2))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(1));
        consultar("?filter[gestion]=" + codigoGestion(gestionVigente) + "&filter[convocatoria]="
                + codigoConvocatoria(convocatoriaB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.convocatoria.nombre").value("Conv B"))
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(2))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(0));
    }

    @Test
    void filtrarPorGestion_yConvocatoriaQueNoEsDeLaGestion_da404ErrDas01() throws Exception {
        consultar("?filter[gestion]=" + codigoGestion(gestionVieja)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gestion.etiqueta").value("GI-OLD"));
        consultar("?filter[gestion]=" + codigoGestion(gestionVieja) + "&filter[convocatoria]="
                + codigoConvocatoria(convocatoriaA))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_DAS_01"));
    }

    // ---- CA04 ----

    @Test
    void unaGestionSinDatos_responde200ConCerosYSinDatos() throws Exception {
        consultar("?filter[gestion]=" + codigoGestion(gestionVieja)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sinDatos").value(true))
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(0))
                .andExpect(jsonPath("$.data.totales.finalistas").value(0))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(0))
                .andExpect(jsonPath("$.data.porMes").isEmpty());
    }

    // ---- CA06, CA07, CA08 ----

    @Test
    void losBorradoresNoSuman_yLasAdjudicacionesSinPublicarTampoco_hastaQuePublican() throws Exception {
        consultar("?filter[convocatoria]=" + codigoConvocatoria(convocatoriaB))
                .andExpect(jsonPath("$.data.totales.finalistas").value(1))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(0));
        consultar("?filter[convocatoria]=" + codigoConvocatoria(convocatoriaA))
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(3));

        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)",
                convocatoriaB, adminId);
        refresco.refrescar();

        consultar("?filter[convocatoria]=" + codigoConvocatoria(convocatoriaB))
                .andExpect(jsonPath("$.data.totales.becasAdjudicadas").value(1));
    }

    @Test
    void laAsistencia_vieneNulaEnLosTotalesYEnCadaMes() throws Exception {
        JsonNode data = datos(consultar("").andExpect(status().isOk()));

        assertThat(data.get("totales").get("asistenciaPromedio").isNull()).isTrue();
        data.get("porMes").forEach(m -> assertThat(m.get("asistencia").isNull()).isTrue());
    }

    // ---- CA09 ----

    @Test
    void consultarYDescargar_noDejanFilasNuevasEnLaBitacoraNiCambianNingunaTabla() throws Exception {
        String antes = huella();

        consultar("").andExpect(status().isOk());
        consultar("?filter[convocatoria]=" + codigoConvocatoria(convocatoriaA)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/filtros").with(directivo)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/indicadores/pdf").with(directivo)).andExpect(status().isOk());

        assertThat(huella()).isEqualTo(antes);
    }

    // ---- CA10 ----

    @Test
    void unAdministradorOUnPostulante_dan403ErrAuth07_enTodasLasRutas() throws Exception {
        for (RequestPostProcessor sesion : List.of(admin, postulante)) {
            for (String ruta : List.of("/filtros", "/indicadores", "/indicadores/pdf")) {
                mockMvc.perform(get(BASE + ruta).with(sesion)).andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
            }
        }
    }

    @Test
    void sinSesion_dan401_yUnaGestionInexistenteDa404() throws Exception {
        for (String ruta : List.of("/filtros", "/indicadores", "/indicadores/pdf")) {
            mockMvc.perform(get(BASE + ruta)).andExpect(status().isUnauthorized());
        }
        consultar("?filter[gestion]=" + UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_DAS_01"));
        mockMvc.perform(get(BASE + "/indicadores/pdf").with(directivo).param("filter[convocatoria]",
                UUID.randomUUID().toString())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_DAS_01"));
    }

    @Test
    void elCatalogoDeFiltros_traeLasGestionesConSusConvocatoriasYLaVigenteMarcada() throws Exception {
        mockMvc.perform(get(BASE + "/filtros").with(directivo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gestiones[0].etiqueta").value("GI-VIG"))
                .andExpect(jsonPath("$.data.gestiones[0].vigente").value(true))
                .andExpect(jsonPath("$.data.gestiones[0].convocatorias.length()").value(2))
                .andExpect(jsonPath("$.data.gestiones[1].etiqueta").value("GI-OLD"))
                .andExpect(jsonPath("$.data.gestiones[1].vigente").value(false))
                .andExpect(jsonPath("$.data.gestiones[1].convocatorias.length()").value(0));
    }

    // ---- CA11, CA12 ----

    @Test
    void elPdf_empiezaPorPdfYSeDescargaComoAdjunto() throws Exception {
        MvcResult resultado = mockMvc.perform(get(BASE + "/indicadores/pdf").with(directivo)).andExpect(status().isOk())
                .andReturn();

        byte[] pdf = resultado.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(resultado.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"indicadores-gi-vig-").endsWith(".pdf\"");
    }

    @Test
    void sinDatos_noHayPdf_ErrDas02() throws Exception {
        mockMvc.perform(get(BASE + "/indicadores/pdf").with(directivo).param("filter[gestion]",
                codigoGestion(gestionVieja).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_DAS_02"));
    }

    // ---- CA13 ----

    @Test
    void conVeinteMilPostulaciones_laConsultaRespondeEnTresSegundosOMenos() throws Exception {
        long gestion = gestion("GI-PERF", "current_date + 100", "current_date + 200");
        long conv = convocatoria(gestion, "Conv perf", "CERRADA");
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT (SELECT id FROM rol WHERE nombre = 'POSTULANTE'), 'PF' || n, 'pf' || n || '@perf.test', '!',
                       'Perf', 'Test' FROM generate_series(1, 20000) n""");
        jdbc.update("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                SELECT u.id, (SELECT min(id) FROM carrera), 'RU-' || u.ci, '70000000'
                FROM usuario u WHERE u.ci LIKE 'PF%'""");
        jdbc.update("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                SELECT ?, u.id, CAST('pendiente' AS estado_postulacion),
                       TIMESTAMPTZ '2026-08-15 12:00:00+00' + ((u.id % 4) || ' months')::interval, 1
                FROM usuario u WHERE u.ci LIKE 'PF%'""", conv);
        refresco.refrescar();

        long inicio = System.nanoTime();
        consultar("?filter[convocatoria]=" + codigoConvocatoria(conv)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totales.postulacionesRecibidas").value(20000));
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(tardo).isLessThanOrEqualTo(Duration.ofSeconds(3));
    }

    // ---- Refresco ----

    @Test
    void refrescar_dejaCalculadoEnMasReciente_yRefrescarSiEstaVaciaNoHaceNadaConDatos() {
        Timestamp antes = jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria",
                Timestamp.class);
        assertThat(refresco.refrescarSiEstaVacia()).isFalse();
        assertThat(jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria", Timestamp.class))
                .isEqualTo(antes);

        assertThat(refresco.refrescar()).isTrue();

        assertThat(jdbc.queryForObject("SELECT max(calculado_en) FROM mv_indicadores_convocatoria", Timestamp.class))
                .isAfter(antes);
    }

    // ---- ayudas ----

    private ResultActions consultar(String query) throws Exception {
        return mockMvc.perform(get(BASE + "/indicadores" + query).with(directivo));
    }

    private JsonNode datos(ResultActions acciones) throws Exception {
        return JSON.readTree(acciones.andReturn().getResponse().getContentAsString()).get("data");
    }

    private long gestion(String etiqueta, String inicioSql, String finSql) {
        return jdbc.queryForObject("INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin) VALUES (?, "
                + inicioSql + ", " + finSql + ") RETURNING id", Long.class, etiqueta);
    }

    private long convocatoria(long gestion, String nombre, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, ?, now() - interval '20 days', now() + interval '20 days',
                        CAST(? AS estado_convocatoria)) RETURNING id""", Long.class, gestion, nombre, estado);
    }

    private long enviada(long convocatoria, String envio) {
        long postulacion = fx.postulacion(convocatoria, fx.postulante(String.valueOf(siguienteCi++)), "pendiente");
        fx.fechaEnvio(postulacion, envio);
        return postulacion;
    }

    private void adjudicar(long postulacion, String resultado, String fecha) {
        fx.adjudicacion(postulacion, resultado, "80.00", adminId);
        jdbc.update("UPDATE adjudicacion SET fecha_adjudicacion = CAST(? AS timestamptz) WHERE postulacion_id = ?",
                fecha, postulacion);
    }

    private UUID codigoGestion(long id) {
        return jdbc.queryForObject("SELECT codigo_gestion_academica FROM gestion_academica WHERE id = ?", UUID.class,
                id);
    }

    private UUID codigoConvocatoria(long id) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class, id);
    }

    /** Cuantas filas hay en cada tabla que el dashboard podria tocar, para comprobar que no escribe nada. */
    private String huella() {
        return String.join(",", jdbc.queryForList("""
                SELECT 'bitacora=' || (SELECT count(*) FROM bitacora)
                    || ',postulacion=' || (SELECT count(*) FROM postulacion)
                    || ',finalista=' || (SELECT count(*) FROM finalista)
                    || ',adjudicacion=' || (SELECT count(*) FROM adjudicacion)
                    || ',publicacion=' || (SELECT count(*) FROM publicacion_resultado)
                    || ',convocatoria=' || (SELECT count(*) FROM convocatoria)
                    || ',vista=' || (SELECT coalesce(sum(postulaciones_recibidas), 0) FROM mv_indicadores_convocatoria)
                    || ',calculado=' || (SELECT max(calculado_en) FROM mv_indicadores_convocatoria)
                """, String.class));
    }

    private void crearConClave(String correo, String ci, String rol) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, 'Persona', 'Prueba' FROM rol WHERE nombre = ?""", ci, correo,
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
