package com.comedoruagrm.backend.features.cotejos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU11 de punta a punta contra un Postgres real: los datos y los registros digitales de una planilla (solo los de su
 * periodo y su convocatoria, sin CI), el registro del cotejo con sus reglas, el historial, la bitacora sin datos
 * sensibles, el rol y que no haya rutas de edicion.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class CotejoIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // 02:30 UTC del 17/11 es el 16/11 22:30 en La Paz.
    private static final Instant AHORA = Instant.parse("2026-11-17T02:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private Clock reloj;

    private ExpedienteFixtures fx;
    private RequestPostProcessor admin;
    private RequestPostProcessor personal;
    private long adminId;
    private long personalId;
    private long convocatoriaId;
    private UUID planilla;
    private long planillaId;
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
        reset(reloj);
        when(reloj.instant()).thenReturn(AHORA);
        fx = new ExpedienteFixtures(jdbc);
        siguienteCi = 3300100;
        usuarioConClave("0000001", "ADMINISTRADOR_DUBSS", "admin@dubss.test");
        usuarioConClave("0000010", "PERSONAL_COMEDOR", "comedor@dubss.test");
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        personalId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000010'", Long.class);
        admin = bearer(login("admin@dubss.test"));
        personal = bearer(login("comedor@dubss.test"));

        convocatoriaId = fx.convocatoria("COTINT");
        // La planilla se «generó» dentro de una hora: las becas de abajo (creadas ahora) ya existían.
        planillaId = planilla(convocatoriaId, "2026-09-01", "2026-09-30", 2, "now() + interval '1 hour'");
        planilla = jdbc.queryForObject("SELECT codigo_planilla_asistencia FROM planilla_asistencia WHERE id = ?",
                UUID.class, planillaId);
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- datos y registros digitales ----

    @Test
    void datos_cuentaSoloLosRegistrosDelPeriodoDeLaConvocatoriaDeLaPlanilla() throws Exception {
        long ana = beca(convocatoriaId, "Ana", "Uno", "VIGENTE");
        long beto = beca(convocatoriaId, "Beto", "Dos", "BAJA");
        asistencia(ana, "2026-09-05");
        asistencia(ana, "2026-09-06");
        asistencia(beto, "2026-09-05");
        asistencia(ana, "2026-10-01"); // fuera del periodo
        long otra = fx.convocatoria("COTOTRA");
        asistencia(beca(otra, "Carla", "Tres", "VIGENTE"), "2026-09-05"); // otra convocatoria

        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/datos", planilla).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planilla").value(planilla.toString()))
                .andExpect(jsonPath("$.data.convocatoriaNombre").value("Conv COTINT"))
                .andExpect(jsonPath("$.data.fechaDesde").value("2026-09-01"))
                .andExpect(jsonPath("$.data.fechaHasta").value("2026-09-30"))
                .andExpect(jsonPath("$.data.totalBecados").value(2))
                .andExpect(jsonPath("$.data.registrosDigitales").value(3))
                .andExpect(jsonPath("$.data.becadosConAsistencia").value(2))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void datos_sinRegistrosDigitales_devuelveCeroSinError() throws Exception {
        beca(convocatoriaId, "Ana", "Uno", "VIGENTE");

        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/datos", planilla).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.registrosDigitales").value(0))
                .andExpect(jsonPath("$.data.becadosConAsistencia").value(0));
    }

    @Test
    void datos_noCuentaLasBecasCreadasDespuesDeGenerarLaPlanilla() throws Exception {
        jdbc.update("UPDATE planilla_asistencia SET generada_en = now() - interval '1 hour' WHERE id = ?", planillaId);
        long tardia = beca(convocatoriaId, "Dina", "Cuatro", "VIGENTE");
        asistencia(tardia, "2026-09-05");

        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/datos", planilla).with(admin))
                .andExpect(jsonPath("$.data.registrosDigitales").value(0));
    }

    @Test
    void registros_devuelveLaPaginaOrdenadaSinCiNiIds() throws Exception {
        long ana = beca(convocatoriaId, "Ana", "Zeta", "VIGENTE");
        long beto = beca(convocatoriaId, "Beto", "Alfa", "VIGENTE");
        asistencia(ana, "2026-09-05");
        asistencia(beto, "2026-09-06");
        asistencia(beto, "2026-09-05");

        // Por defecto: fecha y apellidos.
        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/registros", planilla).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].apellidos").value("Alfa"))
                .andExpect(jsonPath("$.data[0].fecha").value("2026-09-05"))
                .andExpect(jsonPath("$.data[1].apellidos").value("Zeta"))
                .andExpect(jsonPath("$.data[2].fecha").value("2026-09-06"))
                .andExpect(jsonPath("$.data[0].carrera").exists())
                .andExpect(jsonPath("$.data[0].horaRegistro").exists())
                .andExpect(jsonPath("$.data[0].ci").doesNotExist())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(3));

        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/registros", planilla).param("sort", "-apellidos")
                .param("pageSize", "2").with(admin))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].apellidos").value("Zeta"))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void registros_conSortOFiltroNoSoportado_400ErrSys05() throws Exception {
        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/registros", planilla).param("sort", "ci").with(admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/registros", planilla).param("filter[x]", "1")
                .with(admin)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void datosRegistrosYListado_conPlanillaInexistente_404ErrCot01() throws Exception {
        UUID otra = UUID.randomUUID();

        for (String ruta : new String[] {"/datos", "/registros", ""}) {
            mockMvc.perform(get("/api/v1/planillas/{c}/cotejos" + ruta, otra).with(admin))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_COT_01"));
        }
    }

    // ---- registrar ----

    @Test
    void registrar_conforme_guardaElCotejoYAnotaLaBitacoraSinDatosSensibles() throws Exception {
        registrar(admin, planilla, 880, 880, 0, "CONFORME", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.planilla").value(planilla.toString()))
                .andExpect(jsonPath("$.data.fechaCotejo").value("2026-11-16"))
                .andExpect(jsonPath("$.data.resultado").value("CONFORME"))
                .andExpect(jsonPath("$.data.realizadoPor").value("Personal Prueba"))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        Map<String, Object> fila = jdbc.queryForMap("SELECT planilla_id, realizado_por, total_registros, "
                + "coincidencias, discrepancias, resultado, observaciones FROM cotejo_asistencia");
        assertThat(fila).containsEntry("planilla_id", planillaId).containsEntry("realizado_por", adminId)
                .containsEntry("total_registros", 880).containsEntry("coincidencias", 880)
                .containsEntry("discrepancias", 0).containsEntry("resultado", "CONFORME")
                .containsEntry("observaciones", null);

        Map<String, Object> bitacora = jdbc.queryForMap("SELECT usuario_id, entidad, detalle::text AS detalle "
                + "FROM bitacora WHERE accion = 'COTEJO_ASISTENCIA'");
        assertThat(bitacora).containsEntry("usuario_id", adminId).containsEntry("entidad", "cotejo_asistencia");
        assertThat((String) bitacora.get("detalle")).contains(planilla.toString(), "CONFORME", "880");
    }

    @Test
    void registrar_conDiscrepancias_guardaLasObservacionesPeroNoLasLlevaALaBitacora() throws Exception {
        registrar(admin, planilla, 880, 861, 19, "CON_DISCREPANCIAS", "  Faltan firmas del 05/09  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.observaciones").value("Faltan firmas del 05/09"))
                .andExpect(jsonPath("$.data.discrepancias").value(19));

        assertThat(jdbc.queryForObject("SELECT observaciones FROM cotejo_asistencia", String.class))
                .isEqualTo("Faltan firmas del 05/09");
        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = "
                + "'COTEJO_ASISTENCIA'", String.class);
        assertThat(detalle).doesNotContain("Faltan firmas").contains("CON_DISCREPANCIAS");
    }

    @Test
    void registrar_noModificaLasAsistenciasNiLaPlanilla() throws Exception {
        long ana = beca(convocatoriaId, "Ana", "Uno", "VIGENTE");
        asistencia(ana, "2026-09-05");
        String antes = instantanea("SELECT id || ':' || fecha FROM asistencia ORDER BY id")
                + instantanea("SELECT id || ':' || total_becados FROM planilla_asistencia");

        registrar(admin, planilla, 1, 1, 0, "CONFORME", null).andExpect(status().isCreated());

        assertThat(instantanea("SELECT id || ':' || fecha FROM asistencia ORDER BY id")
                + instantanea("SELECT id || ':' || total_becados FROM planilla_asistencia")).isEqualTo(antes);
    }

    @Test
    void registrar_variasVeces_conservaTodosLosCotejos() throws Exception {
        registrar(admin, planilla, 10, 10, 0, "CONFORME", null).andExpect(status().isCreated());
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(3600));
        registrar(admin, planilla, 10, 8, 2, "CON_DISCREPANCIAS", "Dos sin firma").andExpect(status().isCreated());

        assertThat(contar("cotejo_asistencia")).isEqualTo(2);
        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos", planilla).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].resultado").value("CON_DISCREPANCIAS"))
                .andExpect(jsonPath("$.data[1].resultado").value("CONFORME"))
                .andExpect(jsonPath("$.meta.totalItems").value(2));
        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos", planilla).param("sort", "creadoEn").with(admin))
                .andExpect(jsonPath("$.data[0].resultado").value("CONFORME"));
    }

    @Test
    void registrar_conReglasIncumplidas_400ConSuCodigoYNoGuarda() throws Exception {
        registrar(admin, planilla, 10, 5, 2, "CON_DISCREPANCIAS", "x").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_COT_02"));
        registrar(admin, planilla, 10, 8, 2, "CONFORME", "x").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_COT_03"));
        registrar(admin, planilla, 10, 10, 0, "CON_DISCREPANCIAS", "x").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_COT_03"));
        registrar(admin, planilla, 10, 8, 2, "CON_DISCREPANCIAS", "   ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_COT_04"));
        registrar(admin, planilla, 10, 8, 2, "CON_DISCREPANCIAS", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_COT_04"));
        assertThat(contar("cotejo_asistencia")).isZero();
        assertThat(contar("bitacora WHERE accion = 'COTEJO_ASISTENCIA'")).isZero();
    }

    @Test
    void registrar_conCamposInvalidos_400ErrSys01() throws Exception {
        mockMvc.perform(post("/api/v1/planillas/{c}/cotejos", planilla).with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        registrar(admin, planilla, -1, 0, 0, "CONFORME", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        registrar(admin, planilla, 1, 1, 0, "CONFORME", "a".repeat(2001)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_conPlanillaInexistente_404ErrCot01() throws Exception {
        registrar(admin, UUID.randomUUID(), 1, 1, 0, "CONFORME", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_COT_01"));
    }

    // ---- rol y rutas ----

    @Test
    void conOtroRol_403ErrAuth07_ySinSesion_401() throws Exception {
        registrar(personal, planilla, 1, 1, 0, "CONFORME", null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        for (String ruta : new String[] {"/datos", "/registros", ""}) {
            mockMvc.perform(get("/api/v1/planillas/{c}/cotejos" + ruta, planilla).with(personal))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
        mockMvc.perform(get("/api/v1/planillas/{c}/cotejos/datos", planilla)).andExpect(status().isUnauthorized());
        assertThat(contar("cotejo_asistencia")).isZero();
    }

    @Test
    void editarOEliminar_noTieneRutas_405() throws Exception {
        mockMvc.perform(put("/api/v1/planillas/{c}/cotejos", planilla).with(admin))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/v1/planillas/{c}/cotejos", planilla).with(admin))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/planillas/{c}/cotejos", planilla).with(admin))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---- ayudas ----

    private ResultActions registrar(RequestPostProcessor sesion, UUID planillaCodigo, int total, int coincidencias,
            int discrepancias, String resultado, String observaciones) throws Exception {
        String cuerpo = JSON.createObjectNode().put("totalRegistros", total).put("coincidencias", coincidencias)
                .put("discrepancias", discrepancias).put("resultado", resultado)
                .put("observaciones", observaciones).toString();
        return mockMvc.perform(post("/api/v1/planillas/{c}/cotejos", planillaCodigo).with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private long planilla(long convocatoria, String desde, String hasta, int total, String generadaEn) {
        return jdbc.queryForObject("INSERT INTO planilla_asistencia (convocatoria_id, generada_por, fecha_desde, "
                + "fecha_hasta, total_becados, storage_path, generada_en) VALUES (?, ?, CAST(? AS date), "
                + "CAST(? AS date), ?, 'planillas/x.pdf', " + generadaEn + ") RETURNING id", Long.class,
                convocatoria, adminId, desde, hasta, total);
    }

    /** Un postulante con su beca en el estado dado; devuelve el id de la beca. */
    private long beca(long convocatoria, String nombres, String apellidos, String estado) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        return jdbc.queryForObject("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), DATE '2026-09-01') RETURNING id""", Long.class, convocatoria,
                usuario, estado);
    }

    private void asistencia(long beca, String fecha) {
        jdbc.update("INSERT INTO asistencia (beca_id, registrado_por, fecha) VALUES (?, ?, CAST(? AS date))", beca,
                personalId, fecha);
    }

    private void usuarioConClave(String ci, String rol, String correo) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, 'Personal', 'Prueba' FROM rol WHERE nombre = ?""", ci, correo,
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
        return String.join("|", jdbc.queryForList(consulta, String.class));
    }

    private long contar(String tablaYCondicion) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM " + tablaYCondicion, Long.class);
        return total == null ? 0 : total;
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues sobre la misma base (un solo
    // Postgres por JVM) no deben ver lo que estos flujos commitean.
    private void borrarTodo() {
        jdbc.update("DELETE FROM cotejo_asistencia");
        jdbc.update("DELETE FROM asistencia");
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
