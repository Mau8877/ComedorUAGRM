package com.comedoruagrm.backend.features.asistencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * CU10 de punta a punta contra un Postgres real: buscar al becado por CI, registrar su asistencia (una sola por
 * beca y dia, con la fecha de Bolivia), los rechazos por estado de beca y por rol, y que no haya rutas de edicion.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class AsistenciaIntegrationTest {

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
    private RequestPostProcessor personal;
    private RequestPostProcessor admin;
    private long personalId;
    private long convocatoriaId;

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
        usuarioConClave("0000010", "PERSONAL_COMEDOR", "comedor@dubss.test");
        usuarioConClave("0000001", "ADMINISTRADOR_DUBSS", "admin@dubss.test");
        personalId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000010'", Long.class);
        personal = bearer(login("comedor@dubss.test"));
        admin = bearer(login("admin@dubss.test"));
        convocatoriaId = fx.convocatoria("ASIINT");
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- buscar ----

    @Test
    void buscar_conCiDeUnBecadoVigente_devuelveSusDatosSinIdsInternos() throws Exception {
        beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");

        buscar(personal, "9812241").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.nombres").value("Ana"))
                .andExpect(jsonPath("$.data.apellidos").value("Rojas Vaca"))
                .andExpect(jsonPath("$.data.ci").value("9812241"))
                .andExpect(jsonPath("$.data.estadoBeca").value("VIGENTE"))
                .andExpect(jsonPath("$.data.fecha").value("2026-11-16"))
                .andExpect(jsonPath("$.data.horaRegistro").doesNotExist())
                .andExpect(jsonPath("$.data.becaCodigo").exists())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.correo").doesNotExist());
    }

    @Test
    void buscar_conCiSinBeca_404ErrAsi01() throws Exception {
        fx.postulante("9812242");

        buscar(personal, "9812242").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_ASI_01"));
        buscar(personal, "0").andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ASI_01"));
    }

    @Test
    void buscar_conBecaSuspendida_laDevuelveConSuEstado() throws Exception {
        beca("9812243", "Beto", "Dos", "SUSPENDIDA");

        buscar(personal, "9812243").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estadoBeca").value("SUSPENDIDA"));
    }

    @Test
    void buscar_conCiVacioOLargo_400ErrSys01() throws Exception {
        buscar(personal, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        buscar(personal, "1".repeat(21)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    // ---- registrar ----

    @Test
    void registrar_conBecaVigente_creaUnaFilaConFechaDeBoliviaHoraYUsuario() throws Exception {
        UUID beca = beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");

        registrar(personal, beca).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.yaRegistrada").value(false))
                .andExpect(jsonPath("$.data.fecha").value("2026-11-16"))
                .andExpect(jsonPath("$.data.becaCodigo").value(beca.toString()))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        Map<String, Object> fila = jdbc.queryForMap("SELECT registrado_por, fecha::text AS fecha FROM asistencia");
        assertThat(fila).containsEntry("registrado_por", personalId).containsEntry("fecha", "2026-11-16");
        assertThat(contar("asistencia")).isEqualTo(1);
    }

    @Test
    void registrar_dosVeces_noDuplicaYLaSegundaDevuelve200ConLaHoraDeLaPrimera() throws Exception {
        UUID beca = beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");
        registrar(personal, beca).andExpect(status().isCreated());
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(600));

        registrar(personal, beca).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yaRegistrada").value(true))
                .andExpect(jsonPath("$.data.horaRegistro").value(AHORA.toString()));

        assertThat(contar("asistencia")).isEqualTo(1);
        buscar(personal, "9812241").andExpect(jsonPath("$.data.horaRegistro").value(AHORA.toString()));
    }

    @Test
    void registrar_aldiaSiguiente_creaOtraFila() throws Exception {
        UUID beca = beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");
        registrar(personal, beca).andExpect(status().isCreated());
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(86400));

        registrar(personal, beca).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fecha").value("2026-11-17"));

        assertThat(contar("asistencia")).isEqualTo(2);
    }

    @Test
    void registrar_conBecaSuspendidaODeBaja_409ErrAsi02() throws Exception {
        UUID suspendida = beca("9812243", "Beto", "Dos", "SUSPENDIDA");
        UUID baja = beca("9812244", "Carla", "Tres", "BAJA");

        registrar(personal, suspendida).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_ASI_02"));
        registrar(personal, baja).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ASI_02"));
        assertThat(contar("asistencia")).isZero();
    }

    @Test
    void registrar_conBecaInexistenteOSinCodigo_404ErrAsi03Y400() throws Exception {
        registrar(personal, UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_ASI_03"));
        mockMvc.perform(post("/api/v1/asistencias").with(personal).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_noCreaEntradasEnLaBitacoraNiCambiaLaBeca() throws Exception {
        UUID beca = beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");
        long antes = contar("bitacora");

        registrar(personal, beca).andExpect(status().isCreated());

        assertThat(contar("bitacora")).isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT estado::text FROM beca", String.class)).isEqualTo("VIGENTE");
    }

    // ---- rol y rutas ----

    @Test
    void conOtroRol_403ErrAuth07_ySinSesion_401() throws Exception {
        UUID beca = beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");

        buscar(admin, "9812241").andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        registrar(admin, beca).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(post("/api/v1/asistencias/buscar-becado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ci\":\"9812241\"}")).andExpect(status().isUnauthorized());
        assertThat(contar("asistencia")).isZero();
    }

    @Test
    void editarOEliminar_noTieneRutas_405() throws Exception {
        UUID beca = beca("9812241", "Ana", "Rojas Vaca", "VIGENTE");

        mockMvc.perform(put("/api/v1/asistencias").with(personal)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/v1/asistencias").with(personal)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/asistencias").with(personal)).andExpect(status().isMethodNotAllowed());
        assertThat(beca).isNotNull();
    }

    // ---- ayudas ----

    private ResultActions buscar(RequestPostProcessor sesion, String ci) throws Exception {
        return mockMvc.perform(post("/api/v1/asistencias/buscar-becado").with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"ci\":\"%s\"}".formatted(ci)));
    }

    private ResultActions registrar(RequestPostProcessor sesion, UUID beca) throws Exception {
        return mockMvc.perform(post("/api/v1/asistencias").with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"becaCodigo\":\"%s\"}".formatted(beca)));
    }

    /** Un postulante con su beca en el estado dado; devuelve el codigo publico de la beca. */
    private UUID beca(String ci, String nombres, String apellidos, String estado) {
        long usuario = fx.postulante(ci);
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        return jdbc.queryForObject("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), DATE '2026-09-01') RETURNING codigo_beca""", UUID.class,
                convocatoriaId, usuario, estado);
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

    private long contar(String tabla) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
        return total == null ? 0 : total;
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues sobre la misma base (un solo
    // Postgres por JVM) no deben ver lo que estos flujos commitean.
    private void borrarTodo() {
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
