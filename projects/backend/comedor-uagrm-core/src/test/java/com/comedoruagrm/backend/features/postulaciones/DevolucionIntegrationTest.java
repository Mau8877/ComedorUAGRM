package com.comedoruagrm.backend.features.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.comedoruagrm.backend.features.notificaciones.service.MensajePush;
import com.comedoruagrm.backend.features.notificaciones.service.ResultadoPush;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU19 de punta a punta contra un Postgres real: devolver desde el expediente, el historial, el aviso por
 * bandeja, correo y push, la bitacora sin datos personales, que nada mas cambie, los rechazos (409/400/404),
 * la simultaneidad, la reanudacion tras el reenvio y los permisos. El push, el correo y MinIO estan simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DevolucionIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FINALISTAS = "/api/v1/finalistas";
    private static final String MOTIVO = "{\"motivo\":\"Falta la fotocopia de luz\"}";

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
    private RequestPostProcessor postulante;
    private long auditor;
    private long convocatoriaId;
    private long postulacion;
    private long finalistaId;
    private UUID finalista;
    private UUID convocatoria;
    private String base;

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
        crearConClave("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS");
        crearConClave("otro@uagrm.test", "0000002", "POSTULANTE");
        auditor = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        convocatoriaId = fx.convocatoria("DEVINT");
        convocatoria = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?",
                UUID.class, convocatoriaId);
        postulacion = fx.postulacion(convocatoriaId, fx.postulante("1000001"), "calificada");
        finalistaId = fx.finalista(postulacion, 1);
        finalista = jdbc.queryForObject("SELECT codigo_finalista FROM finalista WHERE id = ?", UUID.class,
                finalistaId);
        base = FINALISTAS + "/" + finalista;
        fx.entregaRecibida(finalistaId);
        fx.puntajeIa(postulacion, "55.50", "{\"ingresos\": 20}");
        long carnet = fx.tipoDocumento("Carnet de identidad");
        fx.exigeDocumento(convocatoriaId, carnet, true);
        fx.documentoAdjunto(postulacion, carnet, "carnet.pdf", "prueba/ruta/carnet.pdf");
        admin = bearer(login("admin@dubss.test"));
        postulante = bearer(login("otro@uagrm.test"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    private void auditada() {
        fx.auditoria(finalistaId, auditor, "OBSERVADO", "Falta la luz", null, "2026-11-06T10:00:00Z");
    }

    // ---- devolver (CA01, CA02, CA04, CA05, CA06) ----

    @Test
    void devolver_cambiaElEstado_guardaHistorial_avisaYNoTocaNadaMas() throws Exception {
        auditada();
        String documentosAntes = instantanea("SELECT id || ':' || storage_path FROM documento_adjunto");
        String finalistaAntes = instantanea("SELECT id || ':' || posicion || ':' || documentacion_recibida_en FROM finalista");
        String auditoriasAntes = instantanea("SELECT id || ':' || resultado FROM auditoria_expediente");
        String puntajeAntes = instantanea("SELECT id || ':' || puntaje_total || ':' || vigente FROM puntaje_postulacion");

        devolver(MOTIVO).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.estado").value("devuelta"))
                .andExpect(jsonPath("$.data.motivo").value("Falta la fotocopia de luz"))
                .andExpect(jsonPath("$.data.devueltaPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT estado::text FROM postulacion WHERE id = ?", String.class,
                postulacion)).isEqualTo("devuelta");
        assertThat(jdbc.queryForMap("""
                SELECT estado_anterior::text AS anterior, estado_nuevo::text AS nuevo, motivo, usuario_id
                FROM postulacion_historial WHERE postulacion_id = ?""", postulacion))
                .containsEntry("anterior", "calificada").containsEntry("nuevo", "devuelta")
                .containsEntry("motivo", "Falta la fotocopia de luz").containsEntry("usuario_id", auditor);
        assertThat(instantanea("SELECT id || ':' || storage_path FROM documento_adjunto")).isEqualTo(documentosAntes);
        assertThat(instantanea("SELECT id || ':' || posicion || ':' || documentacion_recibida_en FROM finalista"))
                .isEqualTo(finalistaAntes);
        assertThat(instantanea("SELECT id || ':' || resultado FROM auditoria_expediente")).isEqualTo(auditoriasAntes);
        assertThat(instantanea("SELECT id || ':' || puntaje_total || ':' || vigente FROM puntaje_postulacion"))
                .isEqualTo(puntajeAntes);
    }

    @Test
    void devolver_avisaPorBandejaCorreoYPush_conLasObservaciones() throws Exception {
        auditada();

        devolver(MOTIVO).andExpect(status().isOk());

        assertThat(jdbc.queryForMap("SELECT titulo, mensaje FROM notificacion WHERE postulacion_id = ?", postulacion))
                .containsEntry("titulo", "Tu postulación fue devuelta - Beca Comedor")
                .satisfies(m -> assertThat((String) m.get("mensaje")).contains("Falta la fotocopia de luz"));
        verify(correoService, timeout(3000)).enviar(any(), any(), any());
    }

    @Test
    void devolver_laBitacoraNoLlevaElMotivoNiDatosPersonales() throws Exception {
        auditada();

        devolver(MOTIVO).andExpect(status().isOk());

        String detalle = jdbc.queryForObject(
                "SELECT detalle::text FROM bitacora WHERE accion = 'DEVOLUCION_POSTULACION'", String.class);
        assertThat(detalle).contains(finalista.toString(), "calificada", "devuelta")
                .doesNotContain("fotocopia").doesNotContain("Ana").doesNotContain("Rojas").doesNotContain("1000001");
    }

    @Test
    void devolver_siElAvisoFalla_laDevolucionIgualQueda() throws Exception {
        auditada();
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenThrow(new RuntimeException("fcm caido"));
        when(correoService.enviar(any(), any(), any())).thenThrow(new RuntimeException("smtp caido"));

        devolver(MOTIVO).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT estado::text FROM postulacion WHERE id = ?", String.class,
                postulacion)).isEqualTo("devuelta");
        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("notificacion WHERE postulacion_id = " + postulacion)).isEqualTo(1);
    }

    // ---- rechazos (CA07, CA08, CA09, CA10, CA11) ----

    @Test
    void devolver_sinAuditoria_da409ErrDev02_yNoCambiaNada() throws Exception {
        devolver(MOTIVO).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_02"));

        assertNadaCambio();
    }

    @Test
    void devolver_dosVecesSeguidas_laSegundaDa409ErrDev01_yQuedaUnaSolaFila() throws Exception {
        auditada();
        devolver(MOTIVO).andExpect(status().isOk());

        devolver(MOTIVO).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_01"));

        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("bitacora WHERE accion = 'DEVOLUCION_POSTULACION'")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(1);
    }

    @Test
    void devolver_unaPostulacionNoCalificada_da409ErrDev01() throws Exception {
        auditada();
        jdbc.update("UPDATE postulacion SET estado = CAST('archivada' AS estado_postulacion) WHERE id = ?",
                postulacion);

        devolver(MOTIVO).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_01"));

        assertThat(contar("postulacion_historial")).isZero();
    }

    @Test
    void devolver_motivoInvalido_da400ErrSys01_yNoCambiaNada() throws Exception {
        auditada();
        String[] cuerpos = {"{}", "{\"motivo\":\"   \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}"};
        for (String cuerpo : cuerpos) {
            devolver(cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }

        assertNadaCambio();
    }

    @Test
    void devolver_conFinalistaInexistente_da404ErrFin01() throws Exception {
        mockMvc.perform(post(FINALISTAS + "/" + UUID.randomUUID() + "/devolver").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    @Test
    void devolver_dosAdministradoresALaVez_unoGanaYElOtroRecibe409() throws Exception {
        auditada();
        RequestPostProcessor admin2 = admin;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tareas.add(() -> mockMvc.perform(post(base + "/devolver").with(admin2)
                        .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                        .andReturn().getResponse().getStatus());
            }
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(tareas)) {
                codigos.add(f.get());
            }
            assertThat(codigos).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("bitacora WHERE accion = 'DEVOLUCION_POSTULACION'")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(1);
    }

    // ---- efectos en otros casos de uso (CA12, CA13, CA14) ----

    @Test
    void trasDevolver_noSePuedeAuditar_yLaListaDeFinalistasMuestraDevuelta() throws Exception {
        auditada();
        devolver(MOTIVO).andExpect(status().isOk());

        mockMvc.perform(post(base + "/auditorias").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_02"));

        mockMvc.perform(get(FINALISTAS + "?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].estadoPostulacion").value("devuelta"));
        mockMvc.perform(get("/api/v1/lista-priorizada?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void trasElReenvio_hayQueAuditarDeNuevoAntesDeDevolver_yAhoraSiSePuede() throws Exception {
        auditada();
        devolver(MOTIVO).andExpect(status().isOk());
        // El reenvio (CU02) deja la postulacion otra vez calificada; la auditoria anterior es previa a la devolucion.
        jdbc.update("UPDATE postulacion SET estado = CAST('calificada' AS estado_postulacion) WHERE id = ?",
                postulacion);

        devolver(MOTIVO).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_DEV_02"));

        fx.auditoria(finalistaId, auditor, "OBSERVADO", "Sigue sin la luz", null, "2099-01-01T00:00:00Z");
        devolver("{\"motivo\":\"Segunda vez\"}").andExpect(status().isOk());

        assertThat(contar("postulacion_historial")).isEqualTo(2);
    }

    // ---- permisos (CA15) ----

    @Test
    void sinSesionDa401_yUnPostulanteDa403() throws Exception {
        auditada();
        mockMvc.perform(post(base + "/devolver").contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(base + "/devolver").with(postulante).contentType(MediaType.APPLICATION_JSON)
                .content(MOTIVO)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));

        assertNadaCambio();
    }

    // ---- ayudas ----

    private ResultActions devolver(String cuerpo) throws Exception {
        return mockMvc.perform(post(base + "/devolver").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private void assertNadaCambio() {
        assertThat(jdbc.queryForObject("SELECT estado::text FROM postulacion WHERE id = ?", String.class,
                postulacion)).isEqualTo("calificada");
        assertThat(contar("postulacion_historial")).isZero();
        assertThat(contar("bitacora WHERE accion = 'DEVOLUCION_POSTULACION'")).isZero();
        assertThat(contar("notificacion")).isZero();
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
