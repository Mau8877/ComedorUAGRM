package com.comedoruagrm.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

/**
 * Perfil y cambio de contraseña (CU15, flujos C, D y E) contra un Postgres real: JWT firmados de
 * verdad, sesiones por dispositivo y bitacora.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PerfilIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PERFIL = "/api/v1/perfil";
    private static final String CAMBIAR = "/api/v1/auth/cambiar-contrasena";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues no deben ver estas cuentas.
    @AfterEach
    void limpiarAlTerminar() {
        borrarTodo();
    }

    // ---- consultar (flujo C) ----

    @Test
    void obtener_comoPostulante_traeRegistroCarreraYTelefonoYNuncaElHash() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        mockMvc.perform(get(PERFIL).with(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ci").value("1234567"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"))
                .andExpect(jsonPath("$.data.postulante.registroUniversitario").value("219012345"))
                .andExpect(jsonPath("$.data.postulante.telefono").value("70000001"))
                .andExpect(jsonPath("$.data.postulante.carrera.nombre").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void obtener_comoAdministrador_noTraeDatosDePostulante() throws Exception {
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));

        mockMvc.perform(get(PERFIL).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rol").value("ADMINISTRADOR_DUBSS"))
                .andExpect(jsonPath("$.data.postulante").doesNotExist());
    }

    @Test
    void obtener_sinSesion_da401() throws Exception {
        mockMvc.perform(get(PERFIL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    // ---- actualizar (flujo D) ----

    @Test
    void actualizar_elTelefono_loGuardaYAnotaSoloElCampoSinElValor() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        actualizar(ana, "{\"telefono\":\"71111111\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postulante.telefono").value("71111111"));

        assertThat(jdbc.queryForObject("SELECT telefono FROM postulante", String.class)).isEqualTo("71111111");
        var detalle = jdbc.queryForObject(
                "SELECT detalle::text FROM bitacora WHERE accion = 'ACTUALIZACION_PERFIL'", String.class);
        assertThat(detalle).contains("telefono").doesNotContain("71111111");
    }

    @Test
    void actualizar_elCorreoConLaContrasenaActual_loUsaParaIniciarSesion() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        actualizar(ana, "{\"correo\":\"Nuevo@UAGRM.edu.bo\",\"contrasenaActual\":\"secreta1\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.correo").value("nuevo@uagrm.edu.bo"));

        login("nuevo@uagrm.edu.bo", "secreta1");
        login("ana@uagrm.edu.bo", "secreta1", status().isUnauthorized());
        assertThat(contarBitacora("ACTUALIZACION_PERFIL")).isEqualTo(1);
    }

    @Test
    void actualizar_elCorreoSinLaContrasenaOConUnaIncorrecta_da400ErrAuth09SinCambiarNada() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        actualizar(ana, "{\"correo\":\"nuevo@uagrm.edu.bo\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_09"));
        actualizar(ana, "{\"correo\":\"nuevo@uagrm.edu.bo\",\"contrasenaActual\":\"mala\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_AUTH_09"));

        assertThat(jdbc.queryForObject("SELECT correo FROM usuario", String.class)).isEqualTo("ana@uagrm.edu.bo");
        assertThat(contarBitacora("ACTUALIZACION_PERFIL")).isZero();
        // Seguir con la sesion: un 400 no la cierra.
        mockMvc.perform(get(PERFIL).with(ana)).andExpect(status().isOk());
    }

    @Test
    void actualizar_conElCorreoDeOtraCuenta_da409ErrUs02YNoGuardaNada() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());
        crearAdministrador();

        actualizar(ana, "{\"correo\":\"admin@uagrm.edu.bo\",\"telefono\":\"72222222\","
                + "\"contrasenaActual\":\"secreta1\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_02"));

        assertThat(jdbc.queryForObject("SELECT telefono FROM postulante", String.class)).isEqualTo("70000001");
    }

    @Test
    void actualizar_conElCiEnElCuerpo_noLoCambia() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        actualizar(ana, "{\"ci\":\"999\",\"telefono\":\"71111111\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ci").value("1234567"));

        assertThat(jdbc.queryForObject("SELECT ci FROM usuario", String.class)).isEqualTo("1234567");
    }

    @Test
    void actualizar_elTelefonoDeUnAdministrador_da400ErrUs13() throws Exception {
        crearAdministrador();
        RequestPostProcessor admin = bearer(login("admin@uagrm.edu.bo", "secreta1"));

        actualizar(admin, "{\"telefono\":\"71111111\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_US_13"));
    }

    @Test
    void actualizar_sinCambiosReales_responde200YNoAnotaLaBitacora() throws Exception {
        RequestPostProcessor ana = bearer(registrarAna());

        actualizar(ana, "{\"telefono\":\"70000001\"}").andExpect(status().isOk());
        actualizar(ana, "{}").andExpect(status().isOk());

        assertThat(contarBitacora("ACTUALIZACION_PERFIL")).isZero();
    }

    // ---- cambiar la contraseña (flujo E) ----

    @Test
    void cambiarContrasena_conLaCookieDeLaWeb_cierraLasOtrasSesionesYConservaLaActual() throws Exception {
        JsonNode primera = registrarAna();
        JsonNode segunda = login("ana@uagrm.edu.bo", "secreta1");
        RequestPostProcessor actual = bearer(segunda);

        cambiar(actual, "secreta1", "clave-nueva-1")
                .cookie(new Cookie("refresh_token", segunda.get("refreshToken").asText()))
                .andExpect(status().isOk());

        refrescar(segunda.get("refreshToken").asText()).andExpect(status().isOk());
        refrescar(primera.get("refreshToken").asText()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_06"));
        login("ana@uagrm.edu.bo", "clave-nueva-1");
        login("ana@uagrm.edu.bo", "secreta1", status().isUnauthorized());
        assertThat(contarBitacora("CAMBIO_CONTRASENA")).isEqualTo(1);
    }

    @Test
    void cambiarContrasena_conElRefreshTokenEnElCuerpo_comoMobile_conservaEsaSesion() throws Exception {
        JsonNode primera = registrarAna();
        JsonNode segunda = login("ana@uagrm.edu.bo", "secreta1");

        mockMvc.perform(post(CAMBIAR).with(bearer(segunda)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"secreta1\",\"contrasenaNueva\":\"clave-nueva-1\","
                                + "\"confirmacionContrasena\":\"clave-nueva-1\",\"refreshToken\":\"%s\"}"
                                .formatted(segunda.get("refreshToken").asText())))
                .andExpect(status().isOk());

        refrescar(segunda.get("refreshToken").asText()).andExpect(status().isOk());
        refrescar(primera.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarContrasena_conLaActualIncorrecta_da400ErrAuth09SinCambiarNiCerrarSesiones() throws Exception {
        JsonNode primera = registrarAna();
        JsonNode segunda = login("ana@uagrm.edu.bo", "secreta1");

        cambiar(bearer(segunda), "mala", "clave-nueva-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_09"));

        refrescar(primera.get("refreshToken").asText()).andExpect(status().isOk());
        login("ana@uagrm.edu.bo", "secreta1");
        assertThat(contarBitacora("CAMBIO_CONTRASENA")).isZero();
    }

    @Test
    void cambiarContrasena_sinSesionOConLaNuevaCorta_da401Y400() throws Exception {
        mockMvc.perform(post(CAMBIAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"a\",\"contrasenaNueva\":\"clave-nueva-1\","
                                + "\"confirmacionContrasena\":\"clave-nueva-1\"}"))
                .andExpect(status().isUnauthorized());

        RequestPostProcessor ana = bearer(registrarAna());
        cambiar(ana, "secreta1", "123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void cambiarContrasena_laBitacoraNoGuardaContrasenasNiTokens() throws Exception {
        JsonNode sesion = registrarAna();

        cambiar(bearer(sesion), "secreta1", "clave-nueva-1")
                .andExpect(status().isOk());

        var fila = jdbc.queryForMap("""
                SELECT b.usuario_id = u.id AS mismo_actor, b.detalle::text AS detalle
                FROM bitacora b JOIN usuario u ON u.id = b.usuario_id WHERE b.accion = 'CAMBIO_CONTRASENA'""");
        assertThat(fila).containsEntry("mismo_actor", true);
        assertThat(String.valueOf(fila.get("detalle"))).doesNotContain("secreta1").doesNotContain("clave-nueva-1")
                .doesNotContain(sesion.get("refreshToken").asText());
    }

    // ---- ayudas ----

    private ResultActions actualizar(RequestPostProcessor sesion, String cuerpo) throws Exception {
        return mockMvc.perform(patch(PERFIL).with(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    /** Devuelve el builder ya enviado: se le agregan cookies antes con `cambiarConCookie` si hace falta. */
    private CambioEnCurso cambiar(RequestPostProcessor sesion, String actual, String nueva) {
        return new CambioEnCurso(sesion, actual, nueva);
    }

    private final class CambioEnCurso {
        private final RequestPostProcessor sesion;
        private final String actual;
        private final String nueva;
        private Cookie cookie;

        private CambioEnCurso(RequestPostProcessor sesion, String actual, String nueva) {
            this.sesion = sesion;
            this.actual = actual;
            this.nueva = nueva;
        }

        CambioEnCurso cookie(Cookie cookie) {
            this.cookie = cookie;
            return this;
        }

        ResultActions andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            var peticion = post(CAMBIAR).with(sesion).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"contrasenaActual\":\"%s\",\"contrasenaNueva\":\"%s\",\"confirmacionContrasena\":\"%s\"}"
                            .formatted(actual, nueva, nueva));
            if (cookie != null) {
                peticion.cookie(cookie);
            }
            return mockMvc.perform(peticion).andExpect(matcher);
        }
    }

    private ResultActions refrescar(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private JsonNode login(String correo, String contrasena) throws Exception {
        return login(correo, contrasena, status().isOk());
    }

    private JsonNode login(String correo, String contrasena, org.springframework.test.web.servlet.ResultMatcher esperado)
            throws Exception {
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

    private long contarBitacora(String accion) {
        return jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE accion = ?", Long.class, accion);
    }

    private void borrarTodo() {
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
