package com.comedoruagrm.backend.features.expedientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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

/**
 * CU07 de punta a punta contra un Postgres real: ver el expediente, registrar auditorias, la idempotencia,
 * el total (IA + academico) y su recalculo, los rechazos, el aviso al postulante, la bitacora sin datos
 * personales, que ninguna postulacion cambie, la lista de finalistas y los permisos. El push, el correo y
 * MinIO estan simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpedienteIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FINALISTAS = "/api/v1/finalistas";

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
    private long postulacion;
    private long finalistaId;
    private UUID finalista;
    private UUID convocatoria;
    private UUID documentoCarnet;
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
        when(almacenamiento.urlTemporal(any(), any())).thenReturn("http://localhost:9000/firmada");

        fx = new ExpedienteFixtures(jdbc);
        crearConClave("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS");
        crearConClave("otro@uagrm.test", "0000002", "POSTULANTE");
        long conv = fx.convocatoria("INT");
        convocatoria = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?",
                UUID.class, conv);
        long usuario = fx.postulante("1000001");
        postulacion = fx.postulacion(conv, usuario, "calificada");
        finalistaId = fx.finalista(postulacion, 1);
        finalista = jdbc.queryForObject("SELECT codigo_finalista FROM finalista WHERE id = ?", UUID.class,
                finalistaId);
        base = FINALISTAS + "/" + finalista;
        fx.entregaRecibida(finalistaId);
        fx.puntajeIa(postulacion, "55.50", "{\"ingresos\": 20, \"dependientes\": 10}");
        fx.respuestaSocioeconomica(postulacion, "68.50");
        long carnet = fx.tipoDocumento("Carnet de identidad");
        long notas = fx.tipoDocumento("Certificado de notas");
        fx.exigeDocumento(conv, carnet, true);
        fx.exigeDocumento(conv, notas, true);
        long doc = fx.documentoAdjunto(postulacion, carnet, "carnet.pdf", "prueba/ruta/secreta/carnet.pdf");
        documentoCarnet = jdbc.queryForObject(
                "SELECT codigo_documento_adjunto FROM documento_adjunto WHERE id = ?", UUID.class, doc);
        admin = bearer(login("admin@dubss.test"));
        postulante = bearer(login("otro@uagrm.test"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- ver el expediente (CA01, CA02, CA12) ----

    @Test
    void expediente_muestraTodoYMarcaLosDocumentosFaltantes() throws Exception {
        String estadosAntes = estados();

        mockMvc.perform(get(base + "/expediente").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.postulante.ci").value("1000001"))
                .andExpect(jsonPath("$.data.respuestaSocioeconomica.promedioAcademico").value(68.5))
                .andExpect(jsonPath("$.data.puntajeIa").value(55.5))
                .andExpect(jsonPath("$.data.desglose.ingresos").value(20))
                .andExpect(jsonPath("$.data.finalista.entrega.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.data.documentos.length()").value(2))
                .andExpect(jsonPath("$.data.documentos[0].tipo").value("Carnet de identidad"))
                .andExpect(jsonPath("$.data.documentos[0].estado").value("RECIBIDO"))
                .andExpect(jsonPath("$.data.documentos[1].estado").value("FALTA"))
                .andExpect(jsonPath("$.data.auditorias.length()").value(0))
                .andExpect(jsonPath("$.data.puntajeTotal").doesNotExist());
        String cuerpo = mockMvc.perform(get(base + "/expediente").with(admin)).andReturn().getResponse()
                .getContentAsString();
        assertThat(cuerpo).doesNotContain("storagePath").doesNotContain("secreta");
        assertThat(estados()).isEqualTo(estadosAntes);
    }

    @Test
    void acceso_aUnDocumentoPropio_daLaUrlTemporal_yUnoAjenoDa404() throws Exception {
        mockMvc.perform(get(base + "/documentos/" + documentoCarnet + "/acceso").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://localhost:9000/firmada"))
                .andExpect(jsonPath("$.data.expiraEn").exists());

        mockMvc.perform(get(base + "/documentos/" + UUID.randomUUID() + "/acceso").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_EXP_05"));
    }

    @Test
    void acceso_conMinioCaido_da500PeroElExpedienteSigueDisponible() throws Exception {
        when(almacenamiento.urlTemporal(any(), any())).thenThrow(new AlmacenamientoException("caido", null));

        mockMvc.perform(get(base + "/documentos/" + documentoCarnet + "/acceso").with(admin))
                .andExpect(status().isInternalServerError());
        mockMvc.perform(get(base + "/expediente").with(admin)).andExpect(status().isOk());
    }

    // ---- registrar la auditoria (CA03, CA09, CA11, CA12, CA17, CA20) ----

    @Test
    void validado_guardaAnotaAvisaYMuestraElTotal() throws Exception {
        String estadosAntes = estados();

        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.creada").value(true))
                .andExpect(jsonPath("$.data.auditoria.resultado").value("VALIDADO"))
                .andExpect(jsonPath("$.data.auditoria.auditor").value("Admin Prueba"));

        mockMvc.perform(get(base + "/expediente").with(admin))
                .andExpect(jsonPath("$.data.puntajeIa").value(55.5))
                .andExpect(jsonPath("$.data.puntajeAcademico").value(25.0))
                .andExpect(jsonPath("$.data.puntajeTotal").value(80.5))
                .andExpect(jsonPath("$.data.auditorias.length()").value(1));
        assertThat(contar("auditoria_expediente")).isEqualTo(1);
        assertThat(estados()).isEqualTo(estadosAntes);
        assertThat(jdbc.queryForObject("SELECT puntaje_total FROM puntaje_postulacion WHERE vigente",
                java.math.BigDecimal.class)).isEqualByComparingTo("55.50");
        assertThat(jdbc.queryForObject("""
                SELECT titulo FROM notificacion WHERE postulacion_id = ?""", String.class, postulacion))
                .isEqualTo("Expediente validado - Beca Comedor");
        String bitacora = jdbc.queryForObject(
                "SELECT detalle::text FROM bitacora WHERE accion = 'AUDITORIA_EXPEDIENTE'", String.class);
        assertThat(bitacora).contains(finalista.toString(), "VALIDADO", "25")
                .doesNotContain("Ana").doesNotContain("Rojas").doesNotContain("1000001");
    }

    @Test
    void repetirLaPeticionConLaMismaClave_guardaUnaSolaAuditoria() throws Exception {
        String cuerpo = "{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}";
        String clave = UUID.randomUUID().toString();
        auditar(cuerpo, clave).andExpect(status().isCreated());

        auditar(cuerpo, clave).andExpect(status().isOk()).andExpect(jsonPath("$.data.creada").value(false));

        assertThat(contar("auditoria_expediente")).isEqualTo(1);
        assertThat(contar("bitacora WHERE accion = 'AUDITORIA_EXPEDIENTE'")).isEqualTo(1);
        assertThat(contar("notificacion")).isEqualTo(1);
    }

    @Test
    void sinClaveDeIdempotencia_cadaPeticionCreaUnaAuditoria() throws Exception {
        String cuerpo = "{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}";
        auditar(cuerpo).andExpect(status().isCreated());
        auditar(cuerpo).andExpect(status().isCreated());

        assertThat(contar("auditoria_expediente")).isEqualTo(2);
    }

    @Test
    void laMismaClaveConOtroContenido_da409ErrExp06_yUnaClaveInvalida_da400ErrExp07() throws Exception {
        String clave = UUID.randomUUID().toString();
        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}", clave).andExpect(status().isCreated());

        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":20}", clave)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_06"));
        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":20}", "no-es-uuid")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_EXP_07"));

        assertThat(contar("auditoria_expediente")).isEqualTo(1);
    }

    @Test
    void unaSegundaAuditoria_conservaLaAnterior_yElTotalSeRecalculaConLaIa() throws Exception {
        auditar("{\"resultado\":\"OBSERVADO\",\"observaciones\":\"Falta el certificado de notas\"}")
                .andExpect(status().isCreated());
        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":20}").andExpect(status().isCreated());

        mockMvc.perform(get(base + "/expediente").with(admin))
                .andExpect(jsonPath("$.data.auditorias.length()").value(2))
                .andExpect(jsonPath("$.data.auditorias[0].resultado").value("VALIDADO"))
                .andExpect(jsonPath("$.data.auditorias[1].resultado").value("OBSERVADO"))
                .andExpect(jsonPath("$.data.puntajeTotal").value(75.5));
        assertThat(jdbc.queryForObject("""
                SELECT mensaje FROM notificacion WHERE titulo LIKE 'Tu expediente fue observado%'""",
                String.class)).contains("Falta el certificado de notas");

        // CA21: un reprocesamiento cambia el puntaje de la IA y el total se recalcula.
        jdbc.update("UPDATE puntaje_postulacion SET puntaje_total = 60.00 WHERE vigente");
        mockMvc.perform(get(base + "/expediente").with(admin))
                .andExpect(jsonPath("$.data.puntajeTotal").value(80.0));
    }

    @Test
    void siElDespachoDelAvisoFalla_laAuditoriaSeConserva() throws Exception {
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenThrow(new IllegalStateException("fcm"));
        when(correoService.enviar(any(), any(), any())).thenThrow(new IllegalStateException("smtp"));

        auditar("{\"resultado\":\"NO_VALIDO\",\"observaciones\":\"Documento adulterado\"}")
                .andExpect(status().isCreated());

        assertThat(contar("auditoria_expediente")).isEqualTo(1);
    }

    // ---- rechazos (CA04, CA05, CA07, CA08, CA15, CA18, CA19) ----

    @Test
    void rechazos400_noGuardanNada() throws Exception {
        auditar("{\"resultado\":\"OBSERVADO\",\"observaciones\":\"  \"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_EXP_03"));
        auditar("{\"resultado\":\"NO_VALIDO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_EXP_03"));
        auditar("{\"observaciones\":\"x\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        auditar("{\"resultado\":\"OBSERVADO\",\"observaciones\":\"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        auditar("{\"resultado\":\"VALIDADO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_EXP_04"));
        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":30.01}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_EXP_04"));
        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":-1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_EXP_04"));

        assertThat(contar("auditoria_expediente")).isZero();
        assertThat(contar("bitacora WHERE accion = 'AUDITORIA_EXPEDIENTE'")).isZero();
        assertThat(contar("notificacion")).isZero();
    }

    @Test
    void sinEntregaFisica_da409ErrExp01() throws Exception {
        jdbc.update("UPDATE finalista SET documentacion_recibida_en = NULL WHERE id = ?", finalistaId);

        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_01"));
        assertThat(contar("auditoria_expediente")).isZero();
    }

    @Test
    void conLaPostulacionYaNoCalificada_da409ErrExp02() throws Exception {
        jdbc.update("UPDATE postulacion SET estado = CAST('devuelta' AS estado_postulacion) WHERE id = ?",
                postulacion);

        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_EXP_02"));
        assertThat(contar("auditoria_expediente")).isZero();
    }

    @Test
    void conUnFinalistaInexistente_losTresEndpointsDan404ErrFin01() throws Exception {
        String inexistente = FINALISTAS + "/" + UUID.randomUUID();

        mockMvc.perform(get(inexistente + "/expediente").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
        mockMvc.perform(post(inexistente + "/auditorias").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
        mockMvc.perform(get(inexistente + "/documentos/" + documentoCarnet + "/acceso").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    // ---- lista de finalistas (CA16) ----

    @Test
    void laListaDeFinalistas_muestraElResultadoDeLaUltimaAuditoriaYElTotal() throws Exception {
        String lista = FINALISTAS + "?filter[convocatoriaId]=" + convocatoria;
        mockMvc.perform(get(lista).with(admin))
                .andExpect(jsonPath("$.data[0].resultadoAuditoria").value("SIN_AUDITAR"))
                .andExpect(jsonPath("$.data[0].puntajeTotal").doesNotExist());

        auditar("{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}").andExpect(status().isCreated());

        mockMvc.perform(get(lista).with(admin))
                .andExpect(jsonPath("$.data[0].resultadoAuditoria").value("VALIDADO"))
                .andExpect(jsonPath("$.data[0].puntaje").value(55.5))
                .andExpect(jsonPath("$.data[0].puntajeTotal").value(80.5));
    }

    // ---- permisos (CA14) ----

    @Test
    void sinSesionDa401_yUnPostulanteDa403() throws Exception {
        String cuerpo = "{\"resultado\":\"VALIDADO\",\"puntajeAcademico\":25}";
        mockMvc.perform(get(base + "/expediente")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(base + "/auditorias").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(base + "/documentos/" + documentoCarnet + "/acceso"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(base + "/expediente").with(postulante))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(post(base + "/auditorias").with(postulante).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)).andExpect(status().isForbidden());
        mockMvc.perform(get(base + "/documentos/" + documentoCarnet + "/acceso").with(postulante))
                .andExpect(status().isForbidden());
        assertThat(contar("auditoria_expediente")).isZero();
    }

    // ---- ayudas ----

    private ResultActions auditar(String cuerpo) throws Exception {
        return auditar(cuerpo, null);
    }

    private ResultActions auditar(String cuerpo, String claveIdempotencia) throws Exception {
        MockHttpServletRequestBuilder peticion = post(base + "/auditorias").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (claveIdempotencia != null) {
            peticion = peticion.header("Idempotency-Key", claveIdempotencia);
        }
        return mockMvc.perform(peticion);
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

    private String estados() {
        return jdbc.queryForList("SELECT estado::text || ':' || id FROM postulacion ORDER BY id", String.class)
                .toString();
    }

    private long contar(String tablaYCondicion) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tablaYCondicion, Long.class);
    }

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
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
