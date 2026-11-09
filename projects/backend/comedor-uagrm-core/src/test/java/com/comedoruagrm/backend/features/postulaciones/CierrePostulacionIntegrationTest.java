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
 * CU20 de punta a punta contra un Postgres real: archivar y rechazar desde el expediente, el historial, el aviso
 * por bandeja, correo y push, la bitacora sin datos personales, que nada mas cambie, los rechazos (409/400/404),
 * la simultaneidad, el efecto en las listas y los permisos. El push, el correo y MinIO estan simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CierrePostulacionIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FINALISTAS = "/api/v1/finalistas";
    private static final String MOTIVO = "{\"motivo\":\"No entregó su documentación\"}";

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
        convocatoriaId = fx.convocatoria("ARCINT");
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

    // ---- archivar y rechazar (CA01, CA02, CA14) ----

    @Test
    void archivar_cambiaElEstado_guardaHistorial_avisaYNoTocaNadaMas() throws Exception {
        auditada();
        String documentosAntes = instantanea("SELECT id || ':' || storage_path FROM documento_adjunto");
        String finalistaAntes = instantanea("SELECT id || ':' || posicion || ':' || documentacion_recibida_en FROM finalista");
        String auditoriasAntes = instantanea("SELECT id || ':' || resultado FROM auditoria_expediente");
        String puntajeAntes = instantanea("SELECT id || ':' || puntaje_total || ':' || vigente FROM puntaje_postulacion");

        cerrar("archivar", MOTIVO).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Postulación archivada correctamente"))
                .andExpect(jsonPath("$.data.estado").value("archivada"))
                .andExpect(jsonPath("$.data.motivo").value("No entregó su documentación"))
                .andExpect(jsonPath("$.data.decididaPor").value("Admin Prueba"))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        assertThat(estado()).isEqualTo("archivada");
        assertThat(jdbc.queryForMap("""
                SELECT estado_anterior::text AS anterior, estado_nuevo::text AS nuevo, motivo, usuario_id
                FROM postulacion_historial WHERE postulacion_id = ?""", postulacion))
                .containsEntry("anterior", "calificada").containsEntry("nuevo", "archivada")
                .containsEntry("motivo", "No entregó su documentación").containsEntry("usuario_id", auditor);
        assertThat(instantanea("SELECT id || ':' || storage_path FROM documento_adjunto")).isEqualTo(documentosAntes);
        assertThat(instantanea("SELECT id || ':' || posicion || ':' || documentacion_recibida_en FROM finalista"))
                .isEqualTo(finalistaAntes);
        assertThat(instantanea("SELECT id || ':' || resultado FROM auditoria_expediente")).isEqualTo(auditoriasAntes);
        assertThat(instantanea("SELECT id || ':' || puntaje_total || ':' || vigente FROM puntaje_postulacion"))
                .isEqualTo(puntajeAntes);
    }

    @Test
    void rechazar_dejaNoAdjudicadaConSuHistorial() throws Exception {
        auditada();

        cerrar("rechazar", MOTIVO).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Postulación rechazada correctamente"))
                .andExpect(jsonPath("$.data.estado").value("no_adjudicada"));

        assertThat(estado()).isEqualTo("no_adjudicada");
        assertThat(jdbc.queryForMap("""
                SELECT estado_anterior::text AS anterior, estado_nuevo::text AS nuevo, motivo, usuario_id
                FROM postulacion_historial WHERE postulacion_id = ?""", postulacion))
                .containsEntry("anterior", "calificada").containsEntry("nuevo", "no_adjudicada")
                .containsEntry("motivo", "No entregó su documentación").containsEntry("usuario_id", auditor);
    }

    @Test
    void archivarYRechazar_sinAuditoriasNiEntregaFisica_estaPermitido() throws Exception {
        assertThat(contar("auditoria_expediente")).isZero();

        cerrar("archivar", MOTIVO).andExpect(status().isOk());

        assertThat(estado()).isEqualTo("archivada");
    }

    @Test
    void rechazar_sinAuditorias_estaPermitido() throws Exception {
        cerrar("rechazar", MOTIVO).andExpect(status().isOk());

        assertThat(estado()).isEqualTo("no_adjudicada");
    }

    // ---- aviso y bitacora (CA08, CA09) ----

    @Test
    void archivar_avisaPorBandejaYCorreo_conElMotivo() throws Exception {
        cerrar("archivar", MOTIVO).andExpect(status().isOk());

        assertThat(jdbc.queryForMap("SELECT titulo, mensaje, tipo::text AS tipo FROM notificacion WHERE postulacion_id = ?",
                postulacion))
                .containsEntry("titulo", "Tu postulación fue archivada - Beca Comedor").containsEntry("tipo", "INFO")
                .satisfies(m -> assertThat((String) m.get("mensaje")).contains("No entregó su documentación"));
        verify(correoService, timeout(3000)).enviar(any(), any(), any());
    }

    @Test
    void rechazar_avisaConElTextoDeRechazo() throws Exception {
        cerrar("rechazar", MOTIVO).andExpect(status().isOk());

        assertThat(jdbc.queryForMap("SELECT titulo, mensaje, tipo::text AS tipo FROM notificacion WHERE postulacion_id = ?",
                postulacion))
                .containsEntry("titulo", "Tu postulación no fue adjudicada - Beca Comedor")
                .containsEntry("tipo", "ERROR")
                .satisfies(m -> assertThat((String) m.get("mensaje")).contains("No entregó su documentación"));
    }

    @Test
    void laBitacoraNoLlevaElMotivoNiDatosPersonales() throws Exception {
        cerrar("archivar", MOTIVO).andExpect(status().isOk());
        reiniciarPostulacion();
        cerrar("rechazar", MOTIVO).andExpect(status().isOk());

        for (String accion : new String[] {"ARCHIVO_POSTULACION", "RECHAZO_POSTULACION"}) {
            String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = ?", String.class,
                    accion);
            assertThat(detalle).contains(finalista.toString(), "calificada")
                    .doesNotContain("documentación").doesNotContain("Ana").doesNotContain("Rojas")
                    .doesNotContain("1000001");
        }
    }

    @Test
    void siElAvisoFalla_laDecisionIgualQueda() throws Exception {
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenThrow(new RuntimeException("fcm caido"));
        when(correoService.enviar(any(), any(), any())).thenThrow(new RuntimeException("smtp caido"));

        cerrar("archivar", MOTIVO).andExpect(status().isOk());

        assertThat(estado()).isEqualTo("archivada");
        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("notificacion WHERE postulacion_id = " + postulacion)).isEqualTo(1);
    }

    // ---- rechazos (CA03 a CA07) ----

    @Test
    void motivoInvalido_da400ErrSys01_yNoCambiaNada() throws Exception {
        String[] cuerpos = {"{}", "{\"motivo\":\"   \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}"};
        for (String accion : new String[] {"archivar", "rechazar"}) {
            for (String cuerpo : cuerpos) {
                cerrar(accion, cuerpo).andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
            }
        }

        assertNadaCambio();
    }

    @Test
    void motivoDeExactamente500Caracteres_esValido() throws Exception {
        cerrar("archivar", "{\"motivo\":\"" + "x".repeat(500) + "\"}").andExpect(status().isOk());

        assertThat(estado()).isEqualTo("archivada");
    }

    @Test
    void segundaDecisionSeguida_da409ErrArc01_yQuedaUnaSolaFila() throws Exception {
        cerrar("archivar", MOTIVO).andExpect(status().isOk());

        cerrar("archivar", MOTIVO).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ARC_01"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("archivada")));
        cerrar("rechazar", MOTIVO).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ARC_01"));

        assertThat(estado()).isEqualTo("archivada");
        assertThat(contar("postulacion_historial")).isEqualTo(1);
        assertThat(contar("bitacora WHERE accion IN ('ARCHIVO_POSTULACION', 'RECHAZO_POSTULACION')")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(1);
    }

    @Test
    void unaPostulacionYaDevueltaArchivadaONoAdjudicada_da409ErrArc01() throws Exception {
        for (String estado : new String[] {"devuelta", "archivada", "no_adjudicada"}) {
            jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?", estado,
                    postulacion);

            cerrar("archivar", MOTIVO).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("ERR_ARC_01"));
            cerrar("rechazar", MOTIVO).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("ERR_ARC_01"));
        }

        assertThat(contar("postulacion_historial")).isZero();
        assertThat(contar("bitacora WHERE accion IN ('ARCHIVO_POSTULACION', 'RECHAZO_POSTULACION')")).isZero();
        assertThat(contar("notificacion")).isZero();
    }

    @Test
    void dosPeticionesALaVez_unaGanaYLaOtraRecibe409_conUnSoloAvisoYUnaSolaAnotacion() throws Exception {
        RequestPostProcessor sesion = admin;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (String accion : new String[] {"archivar", "rechazar"}) {
                tareas.add(() -> mockMvc.perform(post(base + "/" + accion).with(sesion)
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
        assertThat(contar("bitacora WHERE accion IN ('ARCHIVO_POSTULACION', 'RECHAZO_POSTULACION')")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(1);
    }

    @Test
    void conFinalistaInexistente_da404ErrFin01() throws Exception {
        for (String accion : new String[] {"archivar", "rechazar"}) {
            mockMvc.perform(post(FINALISTAS + "/" + UUID.randomUUID() + "/" + accion).with(admin)
                    .contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
        }
    }

    // ---- efectos en las listas y en otros casos de uso (CA11, CA14) ----

    @Test
    void trasCerrar_saleDeLaListaPriorizada_seMuestraEnLaDeFinalistas_yNoSePuedeAuditar() throws Exception {
        auditada();
        mockMvc.perform(get("/api/v1/lista-priorizada?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));

        cerrar("archivar", MOTIVO).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/lista-priorizada?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get(FINALISTAS + "?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].estadoPostulacion").value("archivada"));
        mockMvc.perform(post(base + "/auditorias").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_02"));
        assertThat(contar("auditoria_expediente")).isEqualTo(1);
        assertThat(contar("puntaje_postulacion")).isEqualTo(1);
        assertThat(contar("documento_adjunto")).isEqualTo(1);
    }

    @Test
    void trasRechazar_laListaDeFinalistasMuestraNoAdjudicada() throws Exception {
        cerrar("rechazar", MOTIVO).andExpect(status().isOk());

        mockMvc.perform(get(FINALISTAS + "?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].estadoPostulacion").value("no_adjudicada"));
    }

    @Test
    void elHistorialConservaTodasLasFilasAnteriores() throws Exception {
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                   motivo, creado_en)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), CAST('devuelta' AS estado_postulacion),
                        'Falta el carnet', now() - interval '2 days'),
                       (?, ?, CAST('devuelta' AS estado_postulacion), CAST('calificada' AS estado_postulacion),
                        NULL, now() - interval '1 day')""", postulacion, auditor, postulacion, auditor);

        cerrar("archivar", MOTIVO).andExpect(status().isOk());

        assertThat(jdbc.queryForList("""
                SELECT estado_nuevo::text FROM postulacion_historial WHERE postulacion_id = ?
                ORDER BY creado_en, id""", String.class, postulacion))
                .containsExactly("devuelta", "calificada", "archivada");
    }

    // ---- permisos (CA12) ----

    @Test
    void sinSesionDa401_yUnPostulanteDa403() throws Exception {
        for (String accion : new String[] {"archivar", "rechazar"}) {
            mockMvc.perform(post(base + "/" + accion).contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(post(base + "/" + accion).with(postulante).contentType(MediaType.APPLICATION_JSON)
                    .content(MOTIVO)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }

        assertNadaCambio();
    }

    // ---- ayudas ----

    private ResultActions cerrar(String accion, String cuerpo) throws Exception {
        return mockMvc.perform(post(base + "/" + accion).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private String estado() {
        return jdbc.queryForObject("SELECT estado::text FROM postulacion WHERE id = ?", String.class, postulacion);
    }

    // Para decidir dos veces sobre la misma postulacion dentro de un test: la deja calificada y sin avisos.
    private void reiniciarPostulacion() {
        jdbc.update("UPDATE postulacion SET estado = CAST('calificada' AS estado_postulacion) WHERE id = ?",
                postulacion);
    }

    private void assertNadaCambio() {
        assertThat(estado()).isEqualTo("calificada");
        assertThat(contar("postulacion_historial")).isZero();
        assertThat(contar("bitacora WHERE accion IN ('ARCHIVO_POSTULACION', 'RECHAZO_POSTULACION')")).isZero();
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
