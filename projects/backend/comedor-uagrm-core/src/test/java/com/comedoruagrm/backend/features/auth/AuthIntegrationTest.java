package com.comedoruagrm.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Flujo completo del CU01 contra un Postgres real: migraciones de Flyway,
 * indices unicos, JWT firmados de verdad (sin simular nada), sesiones,
 * bloqueo por intentos fallidos y bitacora.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues sobre la
    // misma base (un solo Postgres por JVM) no deben ver las cuentas de estos flujos.
    @AfterEach
    void limpiarAlTerminar() {
        limpiar();
    }

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }

    // ---- registro ----

    @Test
    void registro_conDatosNuevos_creaLaCuentaAbreLaSesionYResponde201() throws Exception {
        registrarAna().andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usuario.correo").value("ana@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data.usuario.rol").value("POSTULANTE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        assertThat(contar("usuario")).isEqualTo(1);
        assertThat(contar("postulante")).isEqualTo(1);
        assertThat(contar("sesion")).isEqualTo(1);
        assertThat(contarBitacora("REGISTRO_USUARIO")).isEqualTo(1);
        assertThat(contarBitacora("INICIO_SESION")).isEqualTo(1);
    }

    @Test
    void registro_anotaEnLaBitacoraElActorLaEntidadElRolYLaIpDelRequest() throws Exception {
        registrarAna().andExpect(status().isCreated());

        var fila = jdbc.queryForMap("""
                SELECT b.entidad, b.entidad_id = u.id AS misma_fila, b.usuario_id = u.id AS mismo_actor,
                       b.detalle ->> 'rol' AS rol, b.ip IS NOT NULL AS con_ip
                FROM bitacora b JOIN usuario u ON u.id = b.usuario_id
                WHERE b.accion = 'REGISTRO_USUARIO'""");
        assertThat(fila).containsEntry("entidad", "usuario").containsEntry("misma_fila", true)
                .containsEntry("mismo_actor", true).containsEntry("rol", "POSTULANTE")
                .containsEntry("con_ip", true);
    }

    @Test
    void registro_guardaLaContrasenaSoloComoHash() throws Exception {
        registrarAna().andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario", String.class);
        assertThat(hash).isNotEqualTo("secreta1").startsWith("$2");
    }

    @Test
    void registro_repetidoConLosMismosDatos_responde200SinDuplicarFilas() throws Exception {
        registrarAna().andExpect(status().isCreated());

        registrarAna().andExpect(status().isOk());

        assertThat(contar("usuario")).isEqualTo(1);
        assertThat(contar("postulante")).isEqualTo(1);
        assertThat(contarBitacora("REGISTRO_USUARIO")).isEqualTo(1);
    }

    @Test
    void registro_conOtroCorreoYElMismoCi_responde409ErrUs01() throws Exception {
        registrarAna().andExpect(status().isCreated());

        registrar("1234567", "219999999", "luis@uagrm.edu.bo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_01"));

        assertThat(contar("usuario")).isEqualTo(1);
    }

    @Test
    void registro_conElMismoCorreoEnOtraCapitalizacion_responde409ErrUs02() throws Exception {
        registrarAna().andExpect(status().isCreated());

        registrar("7654321", "219999999", "ANA@uagrm.edu.bo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_02"));
    }

    @Test
    void registro_conOtroCorreoYElMismoRegistroUniversitario_responde409ErrUs03() throws Exception {
        registrarAna().andExpect(status().isCreated());

        registrar("7654321", "219012345", "luis@uagrm.edu.bo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_03"));
    }

    // ---- login ----

    @Test
    void login_conCredencialesCorrectas_devuelveUnTokenQueAbreLasRutasPrivadas() throws Exception {
        registrarAna().andExpect(status().isCreated());

        JsonNode sesion = datos(login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isOk()));

        mockMvc.perform(get("/api/v1/auth/usuario-actual")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"));
        assertThat(contarBitacora("INICIO_SESION")).isEqualTo(2);
    }

    @Test
    void login_conUnTokenVencidoEnElHeader_loIgnora() throws Exception {
        registrarAna().andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, "Bearer token-viejo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@uagrm.edu.bo\",\"contrasena\":\"secreta1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void login_conContrasenaIncorrectaOCorreoInexistente_dan401ConElMismoMensaje() throws Exception {
        registrarAna().andExpect(status().isCreated());

        String conContrasenaMala = login("ana@uagrm.edu.bo", "mala").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04")).andReturn().getResponse().getContentAsString();
        String sinCorreo = login("nadie@uagrm.edu.bo", "mala").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04")).andReturn().getResponse().getContentAsString();

        assertThat(JSON.readTree(conContrasenaMala).get("message")).isEqualTo(JSON.readTree(sinCorreo).get("message"));
    }

    @Test
    void login_conLaCuentaDesactivada_responde403ErrAuth03SinAbrirSesion() throws Exception {
        registrarAna().andExpect(status().isCreated());
        jdbc.update("UPDATE usuario SET activo = false");
        long sesionesAntes = contar("sesion");

        login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));

        assertThat(contar("sesion")).isEqualTo(sesionesAntes);
    }

    @Test
    void login_tras5IntentosFallidos_bloqueaLaCuentaAunqueLaContrasenaSeaCorrecta() throws Exception {
        registrarAna().andExpect(status().isCreated());

        for (int i = 0; i < 5; i++) {
            login("ana@uagrm.edu.bo", "mala").andExpect(status().isUnauthorized());
        }

        login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_05"));
        assertThat(jdbc.queryForObject("SELECT bloqueado_hasta IS NOT NULL FROM usuario", Boolean.class)).isTrue();
    }

    @Test
    void login_conMenosDe5FallosSeguidosDeUnAcierto_reiniciaElContador() throws Exception {
        registrarAna().andExpect(status().isCreated());
        for (int i = 0; i < 4; i++) {
            login("ana@uagrm.edu.bo", "mala").andExpect(status().isUnauthorized());
        }
        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuario", Integer.class)).isEqualTo(4);

        login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuario", Integer.class)).isZero();
    }

    // ---- rutas privadas ----

    @Test
    void usuarioActual_sinToken_responde401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/auth/usuario-actual"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void usuarioActual_conUnTokenInventado_responde401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/auth/usuario-actual").header(HttpHeaders.AUTHORIZATION, "Bearer inventado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void usuarioActual_conLaCuentaDesactivadaDespuesDeEmitirElToken_responde403ErrAuth03() throws Exception {
        String acceso = datos(registrarAna().andExpect(status().isCreated())).get("accessToken").asText();
        jdbc.update("UPDATE usuario SET activo = false");

        mockMvc.perform(get("/api/v1/auth/usuario-actual").header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
    }

    @Test
    void carreras_sinToken_responde200ConLasCarrerasDeLaMigracion() throws Exception {
        mockMvc.perform(get("/api/v1/carreras").param("pageSize", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(71));
    }

    // ---- refresh y logout ----

    @Test
    void refresh_conUnRefreshTokenVigente_entregaUnParNuevoYRevocaElUsado() throws Exception {
        String refresh = datos(registrarAna().andExpect(status().isCreated())).get("refreshToken").asText();

        JsonNode nuevo = datos(refresh(refresh).andExpect(status().isOk()));

        assertThat(nuevo.get("refreshToken").asText()).isNotEqualTo(refresh);
        refresh(refresh).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_06"));
        refresh(nuevo.get("refreshToken").asText()).andExpect(status().isOk());
    }

    @Test
    void refresh_noAnotaUnInicioDeSesionNuevoEnLaBitacora() throws Exception {
        String refresh = datos(registrarAna().andExpect(status().isCreated())).get("refreshToken").asText();

        refresh(refresh).andExpect(status().isOk());

        assertThat(contarBitacora("INICIO_SESION")).isEqualTo(1);
    }

    @Test
    void refresh_conUnTokenDesconocido_responde401ErrAuth06() throws Exception {
        refresh("desconocido").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_06"));
    }

    @Test
    void refresh_conLaCuentaDesactivada_responde403ErrAuth03() throws Exception {
        String refresh = datos(registrarAna().andExpect(status().isCreated())).get("refreshToken").asText();
        jdbc.update("UPDATE usuario SET activo = false");

        refresh(refresh).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
    }

    @Test
    void logout_revocaSoloLaSesionDeEseDispositivo() throws Exception {
        registrarAna().andExpect(status().isCreated());
        String celular = datos(login("ana@uagrm.edu.bo", "secreta1")).get("refreshToken").asText();
        String web = datos(login("ana@uagrm.edu.bo", "secreta1")).get("refreshToken").asText();

        logout(celular).andExpect(status().isOk());

        refresh(celular).andExpect(status().isUnauthorized());
        refresh(web).andExpect(status().isOk());
    }

    @Test
    void logout_conUnaSesionYaCerradaODesconocida_responde200SinError() throws Exception {
        String refresh = datos(registrarAna().andExpect(status().isCreated())).get("refreshToken").asText();
        logout(refresh).andExpect(status().isOk());

        logout(refresh).andExpect(status().isOk());
        logout("desconocido").andExpect(status().isOk());
    }

    @Test
    void sesion_noGuardaElRefreshTokenEnTextoPlano() throws Exception {
        String refresh = datos(registrarAna().andExpect(status().isCreated())).get("refreshToken").asText();

        String guardado = jdbc.queryForObject("SELECT token_hash FROM sesion", String.class);

        assertThat(guardado).isNotEqualTo(refresh).hasSize(64);
    }

    // ---- cookies HttpOnly (web) ----

    @Test
    void web_conSoloLasCookies_inicia_consulta_renueva_y_cierraSesionSinHeaderAuthorization() throws Exception {
        registrarAna().andExpect(status().isCreated());
        var login = login("ana@uagrm.edu.bo", "secreta1").andExpect(status().isOk()).andReturn().getResponse();
        var acceso = login.getCookie("access_token");
        var refresh = login.getCookie("refresh_token");
        assertThat(acceso).isNotNull();
        assertThat(acceso.isHttpOnly()).isTrue();
        assertThat(refresh.isHttpOnly()).isTrue();

        mockMvc.perform(get("/api/v1/auth/usuario-actual").cookie(acceso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"));

        var renovada = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(renovada.getCookie("refresh_token").getValue()).isNotEqualTo(refresh.getValue());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(renovada.getCookie("refresh_token")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(renovada.getCookie("refresh_token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conUnaCookieDeAccesoVencidaOInvalida_laIgnoraPorSerRutaPublica() throws Exception {
        registrarAna().andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "vencido"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@uagrm.edu.bo\",\"contrasena\":\"secreta1\"}"))
                .andExpect(status().isOk());
    }

    // ---- ayudas ----

    private ResultActions registrarAna() throws Exception {
        return registrar("1234567", "219012345", "ana@uagrm.edu.bo");
    }

    private ResultActions registrar(String ci, String registroUniversitario, String correo) throws Exception {
        UUID carrera = jdbc.queryForObject("SELECT codigo_carrera FROM carrera LIMIT 1", UUID.class);
        String body = """
                {"nombres":"Ana","apellidos":"Perez","ci":"%s","registroUniversitario":"%s",
                 "carreraCodigo":"%s","telefono":"70000001","correo":"%s",
                 "contrasena":"secreta1","confirmacionContrasena":"secreta1"}"""
                .formatted(ci, registroUniversitario, carrera, correo);
        return mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String correo, String contrasena) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private JsonNode datos(ResultActions resultado) throws Exception {
        return JSON.readTree(resultado.andReturn().getResponse().getContentAsString()).get("data");
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private long contarBitacora(String accion) {
        return jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE accion = ?", Long.class, accion);
    }
}
