package com.comedoruagrm.backend.features.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CorreoNotificacionService;
import com.comedoruagrm.backend.features.notificaciones.service.MensajePush;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.notificaciones.service.ResultadoPush;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servicio de notificaciones (CU-EXTRA-01, flujos A, B, C y E) contra un Postgres real. Firebase y el
 * SMTP se simulan: el canal de push y el servicio de correo son dobles; el resto (registro de
 * telefonos, bandeja, despacho asincrono, JWT, roles) es el real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class NotificacionesIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DISPOSITIVOS = "/api/v1/dispositivos-fcm";
    private static final String PRUEBA = "/api/v1/notificaciones/prueba";
    private static final String TOKEN_ANA = "token-secreto-del-telefono-de-ana";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransactionTemplate transacciones;

    @MockitoBean
    private CanalPush canalPush;

    @MockitoBean
    private CorreoNotificacionService correoService;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @BeforeEach
    void limpiar() {
        borrarTodo();
        when(correoService.enviar(any(), any(), any())).thenReturn(true);
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenAnswer(invocacion -> {
            List<?> tokens = invocacion.getArgument(0);
            return new ResultadoPush(tokens.size(), List.of(), List.of());
        });
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues no deben ver estas filas.
    @AfterEach
    void limpiarAlTerminar() {
        borrarTodo();
    }

    // ---- registro y baja de telefonos (flujos A y B) ----

    @Test
    void registrar_creaElTelefonoYRepetirLoNoDuplica() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        registrar(ana, TOKEN_ANA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plataforma").value("ANDROID"))
                .andExpect(jsonPath("$.data.token").doesNotExist());
        registrar(ana, TOKEN_ANA).andExpect(status().isOk());

        assertThat(contar("dispositivo_fcm", "eliminado_en IS NULL")).isEqualTo(1);
    }

    @Test
    void registrar_unTokenQueEraDeOtraCuenta_loReasignaAlUsuarioActual() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());
        registrar(ana, TOKEN_ANA).andExpect(status().isCreated());
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));

        registrar(admin, TOKEN_ANA).andExpect(status().isOk());

        assertThat(contar("dispositivo_fcm", "eliminado_en IS NULL")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT u.correo FROM dispositivo_fcm d JOIN usuario u ON u.id = d.usuario_id
                WHERE d.eliminado_en IS NULL""", String.class)).isEqualTo("admin@uagrm.edu.bo");
    }

    @Test
    void darDeBaja_eliminaElTelefonoYEsIdempotente() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());
        registrar(ana, TOKEN_ANA).andExpect(status().isCreated());

        baja(ana, TOKEN_ANA).andExpect(status().isOk());
        baja(ana, TOKEN_ANA).andExpect(status().isOk());
        baja(ana, "token-que-nunca-existio").andExpect(status().isOk());

        assertThat(contar("dispositivo_fcm", "eliminado_en IS NULL")).isZero();
    }

    @Test
    void darDeBaja_conElTokenDeOtraCuenta_noLoElimina() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());
        registrar(ana, TOKEN_ANA).andExpect(status().isCreated());
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));

        baja(admin, TOKEN_ANA).andExpect(status().isOk());

        assertThat(contar("dispositivo_fcm", "eliminado_en IS NULL")).isEqualTo(1);
    }

    @Test
    void endpoints_sinSesion_dan401() throws Exception {
        mockMvc.perform(post(DISPOSITIVOS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"plataforma\":\"ANDROID\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
        mockMvc.perform(post(PRUEBA)).andExpect(status().isUnauthorized());
    }

    // ---- despacho de avisos (flujos C y E) ----

    @Test
    void prueba_comoAdministradorConTelefono_guardaElAvisoYEnviaPushYCorreo(CapturedOutput salida)
            throws Exception {
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));
        registrar(admin, TOKEN_ANA).andExpect(status().isCreated());

        mockMvc.perform(post(PRUEBA).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(canalPush, timeout(5000)).enviar(eq(List.of(TOKEN_ANA)), any(MensajePush.class));
        verify(correoService, timeout(5000)).enviar(eq("admin@uagrm.edu.bo"), eq("Notificación de prueba"),
                any(String.class));
        assertThat(esperarHasta(() -> contar("notificacion", "enviada_en IS NOT NULL") == 1)).isTrue();
        assertThat(jdbc.queryForObject("SELECT tipo::text FROM notificacion", String.class)).isEqualTo("INFO");
        assertThat(jdbc.queryForObject("SELECT mensaje FROM notificacion", String.class)).contains("Hola Luis");
        assertThat(salida.getAll()).doesNotContain(TOKEN_ANA);
    }

    @Test
    void prueba_comoPostulante_da403ErrAuth07YNoGuardaNada() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        mockMvc.perform(post(PRUEBA).with(ana)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));

        assertThat(contar("notificacion", "true")).isZero();
        verify(canalPush, never()).enviar(anyList(), any(MensajePush.class));
    }

    @Test
    void prueba_sinTelefonosRegistrados_enviaSoloElCorreo() throws Exception {
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));

        mockMvc.perform(post(PRUEBA).with(admin)).andExpect(status().isOk());

        verify(correoService, timeout(5000)).enviar(eq("admin@uagrm.edu.bo"), any(String.class), any(String.class));
        assertThat(esperarHasta(() -> contar("notificacion", "enviada_en IS NOT NULL") == 1)).isTrue();
        verify(canalPush, never()).enviar(anyList(), any(MensajePush.class));
    }

    @Test
    void prueba_siFirebaseDiceQueElTokenYaNoSirve_loEliminaDeLaBase() throws Exception {
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));
        registrar(admin, TOKEN_ANA).andExpect(status().isCreated());
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(0, List.of(TOKEN_ANA), List.of()));

        mockMvc.perform(post(PRUEBA).with(admin)).andExpect(status().isOk());

        assertThat(esperarHasta(() -> contar("dispositivo_fcm", "eliminado_en IS NULL") == 0)).isTrue();
    }

    @Test
    void notificar_dentroDeUnaTransaccionQueHaceRollback_noDejaFilaNiEnviaNada() throws Exception {
        crearAdministrador();
        var admin = usuarioRepository.findByCorreo("admin@uagrm.edu.bo").orElseThrow();

        transacciones.executeWithoutResult(estado -> {
            notificacionService.notificar(admin, EventoNotificacion.PRUEBA, Map.of("nombres", "Luis"));
            estado.setRollbackOnly();
        });

        assertThat(contar("notificacion", "true")).isZero();
        Thread.sleep(300); // da tiempo a que un despacho indebido saliera
        verify(correoService, never()).enviar(any(), any(), any());
        verify(canalPush, never()).enviar(anyList(), any(MensajePush.class));
    }

    @Test
    void notificar_confirmadaLaTransaccion_guardaYDespacha() throws Exception {
        crearAdministrador();
        var admin = usuarioRepository.findByCorreo("admin@uagrm.edu.bo").orElseThrow();

        transacciones.executeWithoutResult(estado ->
                notificacionService.notificar(admin, EventoNotificacion.PRUEBA, Map.of("nombres", "Luis")));

        verify(correoService, timeout(5000)).enviar(eq("admin@uagrm.edu.bo"), any(String.class), any(String.class));
        assertThat(contar("notificacion", "true")).isEqualTo(1);
    }

    // ---- ayudas ----

    private org.springframework.test.web.servlet.ResultActions registrar(RequestPostProcessor sesion, String token)
            throws Exception {
        return mockMvc.perform(post(DISPOSITIVOS).with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"plataforma\":\"ANDROID\"}".formatted(token)));
    }

    private org.springframework.test.web.servlet.ResultActions baja(RequestPostProcessor sesion, String token)
            throws Exception {
        return mockMvc.perform(post(DISPOSITIVOS + "/baja").with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)));
    }

    private JsonNode login(String correo, String contrasena) throws Exception {
        return login(correo, contrasena, status().isOk());
    }

    private JsonNode login(String correo, String contrasena, ResultMatcher esperado) throws Exception {
        var respuesta = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)))
                .andExpect(esperado).andReturn().getResponse();
        return JSON.readTree(respuesta.getContentAsString()).get("data");
    }

    private RequestPostProcessor bearer(JsonNode sesion) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.get("accessToken").asText());
            return request;
        };
    }

    /** Registra a Ana (Postulante, contraseña `secreta1`) por la API y devuelve su sesion. */
    private JsonNode registrarAna() throws Exception {
        UUID carrera = jdbc.queryForObject("SELECT codigo_carrera FROM carrera LIMIT 1", UUID.class);
        String body = """
                {"nombres":"Ana","apellidos":"Perez","ci":"1234567","registroUniversitario":"219012345",
                 "carreraCodigo":"%s","telefono":"70000001","correo":"ana@uagrm.edu.bo",
                 "contrasena":"secreta1","confirmacionContrasena":"secreta1"}""".formatted(carrera);
        var respuesta = mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse();
        return JSON.readTree(respuesta.getContentAsString()).get("data");
    }

    private void crearAdministrador() {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '7654321', 'admin@uagrm.edu.bo', ?, 'Luis', 'Rojas' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS'""", passwordEncoder.encode("secreta1"));
    }

    private long contar(String tabla, String condicion) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + condicion, Long.class);
    }

    /** El despacho es asincrono: espera hasta 5 s a que se cumpla la condicion. */
    private boolean esperarHasta(BooleanSupplier condicion) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (condicion.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condicion.getAsBoolean();
    }

    private void borrarTodo() {
        jdbc.update("DELETE FROM notificacion");
        jdbc.update("DELETE FROM dispositivo_fcm");
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
