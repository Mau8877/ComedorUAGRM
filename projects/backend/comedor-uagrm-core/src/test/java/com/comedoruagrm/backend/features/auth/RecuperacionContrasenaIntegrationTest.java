package com.comedoruagrm.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.comedoruagrm.backend.features.auth.service.CorreoRestablecimientoService;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Recuperacion de contraseña (CU15, flujos A y B) contra un Postgres real: migraciones, JWT firmados
 * de verdad, sesiones y bitacora. Solo el envio del correo se simula, para capturar el enlace.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecuperacionContrasenaIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RECUPERAR = "/api/v1/auth/recuperar-contrasena";
    private static final String RESTABLECER = "/api/v1/auth/restablecer-contrasena";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CorreoRestablecimientoService correoService;

    private ListAppender<ILoggingEvent> logs;

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
        reset(correoService);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger("com.comedoruagrm.backend")).addAppender(logs);
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues no deben ver estas cuentas.
    @AfterEach
    void limpiarAlTerminar() {
        ((Logger) LoggerFactory.getLogger("com.comedoruagrm.backend")).detachAppender(logs);
        borrarTodo();
    }

    // ---- solicitar el enlace (flujo A) ----

    @Test
    void solicitar_conUnaCuentaActiva_guardaSoloElHashYEnviaElToken() throws Exception {
        registrarAna();

        solicitar("ana@uagrm.edu.bo").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos."));

        String token = tokenEnviado();
        String hash = jdbc.queryForObject("SELECT restablecimiento_token_hash FROM usuario", String.class);
        assertThat(hash).hasSize(64).isNotEqualTo(token);
        Boolean vigenteAprox = jdbc.queryForObject("""
                SELECT restablecimiento_expira_en BETWEEN now() + interval '59 minutes'
                                                      AND now() + interval '61 minutes' FROM usuario""", Boolean.class);
        assertThat(vigenteAprox).isTrue();
    }

    @Test
    void solicitar_conUnCorreoInexistenteOUnaCuentaDesactivada_respondeIgualYNoEnviaNada() throws Exception {
        registrarAna();
        String mensajeActiva = mensaje(solicitar("ana@uagrm.edu.bo"));
        reset(correoService);
        jdbc.update("UPDATE usuario SET restablecimiento_token_hash = NULL, restablecimiento_expira_en = NULL");
        jdbc.update("UPDATE usuario SET activo = false");

        String mensajeDesactivada = mensaje(solicitar("ana@uagrm.edu.bo"));
        String mensajeInexistente = mensaje(solicitar("nadie@uagrm.edu.bo"));

        assertThat(mensajeDesactivada).isEqualTo(mensajeActiva);
        assertThat(mensajeInexistente).isEqualTo(mensajeActiva);
        verify(correoService, never()).enviarEnlace(any(), any(), any());
        assertThat(jdbc.queryForObject("SELECT restablecimiento_token_hash FROM usuario", String.class)).isNull();
    }

    @Test
    void solicitar_noDejaNingunaFilaEnLaBitacora() throws Exception {
        registrarAna();
        long antes = contar("bitacora");

        solicitar("ana@uagrm.edu.bo").andExpect(status().isOk());
        solicitar("nadie@uagrm.edu.bo").andExpect(status().isOk());

        assertThat(contar("bitacora")).isEqualTo(antes);
    }

    @Test
    void solicitar_dosVeces_soloElUltimoEnlaceSirve() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        solicitar("ana@uagrm.edu.bo");
        ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
        verify(correoService, times(2)).enviarEnlace(eq("ana@uagrm.edu.bo"), eq("Ana"), tokens.capture());

        restablecer(tokens.getAllValues().get(0), "clave-nueva-1")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
        restablecer(tokens.getAllValues().get(1), "clave-nueva-1").andExpect(status().isOk());
    }

    // ---- restablecer (flujo B) ----

    @Test
    void restablecer_conUnEnlaceVigente_cambiaLaContrasenaCierraSesionesYDesbloquea() throws Exception {
        JsonNode sesion = registrarAna();
        String refreshViejo = sesion.get("refreshToken").asText();
        jdbc.update("UPDATE usuario SET intentos_fallidos = 3, bloqueos_consecutivos = 2,"
                + " bloqueado_hasta = now() + interval '1 hour'");
        solicitar("ana@uagrm.edu.bo");

        restablecer(tokenEnviado(), "clave-nueva-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña restablecida correctamente"));

        var cuenta = jdbc.queryForMap("SELECT intentos_fallidos, bloqueos_consecutivos, bloqueado_hasta,"
                + " restablecimiento_token_hash, restablecimiento_expira_en FROM usuario");
        assertThat(cuenta.get("intentos_fallidos")).isEqualTo(0);
        assertThat(cuenta.get("bloqueos_consecutivos")).isEqualTo(0);
        assertThat(cuenta.get("bloqueado_hasta")).isNull();
        assertThat(cuenta.get("restablecimiento_token_hash")).isNull();
        assertThat(cuenta.get("restablecimiento_expira_en")).isNull();
        login("ana@uagrm.edu.bo", "clave-nueva-1").andExpect(status().isOk());
        login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshViejo)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_06"));
    }

    @Test
    void restablecer_elMismoEnlaceDosVeces_elSegundoUsoDaErrAuth08() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        String token = tokenEnviado();

        restablecer(token, "clave-nueva-1").andExpect(status().isOk());
        restablecer(token, "otra-clave-9").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));

        login("ana@uagrm.edu.bo", "clave-nueva-1").andExpect(status().isOk());
    }

    @Test
    void restablecer_conUnEnlaceVencido_daErrAuth08SinCambiarLaContrasena() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        jdbc.update("UPDATE usuario SET restablecimiento_expira_en = now() - interval '1 minute'");

        restablecer(tokenEnviado(), "clave-nueva-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));

        login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isOk());
    }

    @Test
    void restablecer_conUnTokenInventado_daErrAuth08() throws Exception {
        registrarAna();

        restablecer("token-inventado", "clave-nueva-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }

    @Test
    void restablecer_conContrasenaCortaOSinCoincidir_daErrSys01YElEnlaceSigueSirviendo() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        String token = tokenEnviado();

        restablecer(token, "123").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"contrasena\":\"clave-nueva-1\",\"confirmacionContrasena\":\"otra-1234\"}"
                                .formatted(token)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isOk());
        restablecer(token, "clave-nueva-1").andExpect(status().isOk());
    }

    @Test
    void restablecer_laCuentaDesactivadaDespuesDePedirElEnlace_daErrAuth08() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        jdbc.update("UPDATE usuario SET activo = false");

        restablecer(tokenEnviado(), "clave-nueva-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }

    @Test
    void restablecer_enUnaCuentaHeredadaConHashInvalido_lePermiteObtenerUnaContrasena() throws Exception {
        registrarAna();
        jdbc.update("UPDATE usuario SET password_hash = '!'");
        solicitar("ana@uagrm.edu.bo");

        restablecer(tokenEnviado(), "clave-nueva-1").andExpect(status().isOk());

        login("ana@uagrm.edu.bo", "clave-nueva-1").andExpect(status().isOk());
    }

    // ---- bitacora y logs (CA19) ----

    @Test
    void restablecer_anotaUnaFilaConElDuenoDeLaCuentaComoActorYSinSecretos() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        String token = tokenEnviado();

        restablecer(token, "clave-nueva-1").andExpect(status().isOk());

        var fila = jdbc.queryForMap("""
                SELECT b.entidad, b.entidad_id = u.id AS misma_fila, b.usuario_id = u.id AS mismo_actor,
                       b.ip IS NOT NULL AS con_ip, b.detalle::text AS detalle
                FROM bitacora b JOIN usuario u ON u.id = b.usuario_id
                WHERE b.accion = 'RESTABLECIMIENTO_CONTRASENA'""");
        assertThat(fila).containsEntry("entidad", "usuario").containsEntry("misma_fila", true)
                .containsEntry("mismo_actor", true).containsEntry("con_ip", true);
        assertThat(String.valueOf(fila.get("detalle"))).doesNotContain(token).doesNotContain("clave-nueva-1");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE accion = 'RESTABLECIMIENTO_CONTRASENA'",
                Long.class)).isEqualTo(1);
    }

    @Test
    void elFlujoCompleto_nuncaEscribeElTokenNiLasContrasenasEnLosLogs() throws Exception {
        registrarAna();
        solicitar("ana@uagrm.edu.bo");
        String token = tokenEnviado();
        restablecer(token, "clave-nueva-1");
        restablecer(token, "otra-clave-9");
        login("ana@uagrm.edu.bo", "clave-nueva-1");

        String todo = String.join("\n", logs.list.stream().map(this::textoDe).toList());
        assertThat(todo).doesNotContain(token).doesNotContain("clave-nueva-1").doesNotContain("otra-clave-9")
                .doesNotContain("secreta1");
    }

    // ---- ayudas ----

    private String textoDe(ILoggingEvent evento) {
        StringBuilder texto = new StringBuilder(evento.getFormattedMessage());
        if (evento.getArgumentArray() != null) {
            for (Object argumento : evento.getArgumentArray()) {
                texto.append(' ').append(argumento);
            }
        }
        return texto.toString();
    }

    private String tokenEnviado() {
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(correoService, org.mockito.Mockito.atLeastOnce()).enviarEnlace(any(), any(), token.capture());
        List<String> todos = token.getAllValues();
        return todos.get(todos.size() - 1);
    }

    private ResultActions solicitar(String correo) throws Exception {
        return mockMvc.perform(post(RECUPERAR).contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\"}".formatted(correo)));
    }

    private ResultActions restablecer(String token, String contrasena) throws Exception {
        return mockMvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"contrasena\":\"%s\",\"confirmacionContrasena\":\"%s\"}"
                        .formatted(token, contrasena, contrasena)));
    }

    private ResultActions login(String correo, String contrasena) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)));
    }

    private String mensaje(ResultActions resultado) throws Exception {
        return JSON.readTree(resultado.andReturn().getResponse().getContentAsString()).get("message").asText();
    }

    /** Registra a Ana (contraseña `secreta1`) por la API y devuelve su sesion. */
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

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private void borrarTodo() {
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
