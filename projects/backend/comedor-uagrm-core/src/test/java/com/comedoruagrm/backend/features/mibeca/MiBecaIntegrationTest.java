package com.comedoruagrm.backend.features.mibeca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU24 de punta a punta contra un Postgres real: el becado ve su beca (con el motivo si no esta vigente) y su
 * historial paginado con rango, cada uno ve solo lo suyo, y los rechazos por rol, rango, orden y escritura.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class MiBecaIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ExpedienteFixtures fx;
    private RequestPostProcessor ana;
    private RequestPostProcessor beto;
    private RequestPostProcessor sinBeca;
    private RequestPostProcessor admin;
    private long personalId;
    private long becaAna;
    private long becaBeto;

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
        usuarioConClave("0000001", "ADMINISTRADOR_DUBSS", "admin@dubss.test");
        usuarioConClave("0000010", "PERSONAL_COMEDOR", "comedor@dubss.test");
        personalId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000010'", Long.class);
        long convocatoria = fx.convocatoria("MBEINT");

        long anaId = postulanteConClave("9812241", "ana@uagrm.test");
        long betoId = postulanteConClave("9812242", "beto@uagrm.test");
        postulanteConClave("9812243", "sin@uagrm.test");
        becaAna = beca(convocatoria, anaId, "VIGENTE");
        becaBeto = beca(convocatoria, betoId, "VIGENTE");

        ana = bearer(login("ana@uagrm.test"));
        beto = bearer(login("beto@uagrm.test"));
        sinBeca = bearer(login("sin@uagrm.test"));
        admin = bearer(login("admin@dubss.test"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    @Test
    void miBeca_vigente_devuelveEstadoResumenYSinIdsInternos() throws Exception {
        asistencia(becaAna, "2026-09-01");
        asistencia(becaAna, "2026-09-02");

        mockMvc.perform(get("/api/v1/mi-beca").with(ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.data.totalAsistencias").value(2))
                .andExpect(jsonPath("$.data.convocatoria").exists())
                .andExpect(jsonPath("$.data.gestion").exists())
                .andExpect(jsonPath("$.data.motivo").doesNotExist())
                .andExpect(jsonPath("$.data.codigo").exists())
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void miBeca_dadaDeBaja_incluyeElMotivoYElHistorialSigueConsultable() throws Exception {
        asistencia(becaAna, "2026-09-01");
        jdbc.update("UPDATE beca SET estado = CAST('BAJA' AS estado_beca) WHERE id = ?", becaAna);
        jdbc.update("""
                INSERT INTO movimiento_beca (beca_id, registrado_por, tipo, fecha, motivo)
                VALUES (?, ?, CAST('BAJA' AS tipo_movimiento_beca), DATE '2026-09-10', 'Abandonó la universidad')""",
                becaAna, personalId);

        mockMvc.perform(get("/api/v1/mi-beca").with(ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("BAJA"))
                .andExpect(jsonPath("$.data.motivo").value("Abandonó la universidad"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias").with(ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void miBeca_sinBeca_404ErrMbe01() throws Exception {
        mockMvc.perform(get("/api/v1/mi-beca").with(sinBeca)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_MBE_01"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias").with(sinBeca)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_MBE_01"));
    }

    @Test
    void asistencias_ordenaDeLaMasRecienteYPagina() throws Exception {
        asistencia(becaAna, "2026-09-01");
        asistencia(becaAna, "2026-09-02");
        asistencia(becaAna, "2026-09-03");

        mockMvc.perform(get("/api/v1/mi-beca/asistencias?page=1&pageSize=2").with(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fecha").value("2026-09-03"))
                .andExpect(jsonPath("$.data[1].fecha").value("2026-09-02"))
                .andExpect(jsonPath("$.data[0].horaRegistro").exists())
                .andExpect(jsonPath("$.meta.totalItems").value(3))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias?sort=fecha").with(ana))
                .andExpect(jsonPath("$.data[0].fecha").value("2026-09-01"));
    }

    @Test
    void asistencias_conRango_incluyeLosExtremos() throws Exception {
        asistencia(becaAna, "2026-09-01");
        asistencia(becaAna, "2026-09-02");
        asistencia(becaAna, "2026-09-03");

        mockMvc.perform(get("/api/v1/mi-beca/asistencias?filter[desde]=2026-09-02&filter[hasta]=2026-09-03")
                .with(ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(2));
    }

    @Test
    void asistencias_sinRegistros_paginaVaciaConMeta() throws Exception {
        mockMvc.perform(get("/api/v1/mi-beca/asistencias").with(ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void asistencias_cadaBecadoVeSoloLasSuyas() throws Exception {
        asistencia(becaAna, "2026-09-01");
        asistencia(becaBeto, "2026-09-01");
        asistencia(becaBeto, "2026-09-02");

        mockMvc.perform(get("/api/v1/mi-beca/asistencias").with(ana))
                .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias").with(beto))
                .andExpect(jsonPath("$.meta.totalItems").value(2));
        mockMvc.perform(get("/api/v1/mi-beca").with(beto)).andExpect(jsonPath("$.data.totalAsistencias").value(2));
    }

    @Test
    void asistencias_conParametrosInvalidos_400() throws Exception {
        mockMvc.perform(get("/api/v1/mi-beca/asistencias?filter[desde]=2026-09-30&filter[hasta]=2026-09-01")
                .with(ana)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_MBE_02"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias?sort=-horaRegistro").with(ana))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_MBE_03"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias?filter[desde]=hoy").with(ana))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias?filter[otro]=1").with(ana))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias?page=0").with(ana)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void sinSesion_401_yConOtroRol_403ErrAuth07() throws Exception {
        mockMvc.perform(get("/api/v1/mi-beca")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/mi-beca").with(admin)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get("/api/v1/mi-beca/asistencias").with(admin)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }

    @Test
    void escrituras_405() throws Exception {
        mockMvc.perform(post("/api/v1/mi-beca").with(ana)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/v1/mi-beca").with(ana)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/v1/mi-beca/asistencias").with(ana)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/mi-beca/asistencias").with(ana)).andExpect(status().isMethodNotAllowed());
    }

    // ---- ayudas ----

    private long postulanteConClave(String ci, String correo) {
        long id = fx.postulante(ci);
        jdbc.update("UPDATE usuario SET correo = ?, password_hash = ? WHERE id = ?", correo,
                passwordEncoder.encode("secreta1"), id);
        return id;
    }

    private long beca(long convocatoria, long postulante, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), DATE '2026-09-01') RETURNING id""", Long.class, convocatoria,
                postulante, estado);
    }

    private void asistencia(long beca, String fecha) {
        jdbc.update("""
                INSERT INTO asistencia (beca_id, registrado_por, fecha, hora_registro)
                VALUES (?, ?, CAST(? AS date), now())""", beca, personalId, fecha);
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
