package com.comedoruagrm.backend.features.bitacora;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consulta de la bitacora (CU25) de punta a punta contra un Postgres real: JWT firmados de verdad, el rol
 * (`@PreAuthorize`), los filtros (usuario, accion y fechas en hora de La Paz), el orden, la paginacion y que la
 * respuesta no exponga ids internos ni admita escritura.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BitacoraIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLAVE = "secreta1";
    private static final String RUTA = "/api/v1/bitacora";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String hashClave;
    private RequestPostProcessor admin;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues sobre la misma base (un solo
    // Postgres por JVM) no deben ver lo que estos flujos commitean.
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
        hashClave = passwordEncoder.encode(CLAVE);
    }

    /** El administrador inicia sesion y se borra la bitacora que eso genera, para partir de una base conocida. */
    private long sesionDeAdministrador() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS");
        admin = bearer(login("1"));
        jdbc.update("DELETE FROM bitacora");
        return jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '1'", Long.class);
    }

    private void fila(Long usuarioId, String accion, String entidad, String creadoEn) {
        jdbc.update("""
                INSERT INTO bitacora (usuario_id, accion, entidad, entidad_id, detalle, ip, creado_en)
                VALUES (?, ?, ?, 777, CAST('{"convocatoria":"abc"}' AS jsonb), CAST('10.0.3.21' AS inet),
                        CAST(? AS timestamptz))""", usuarioId, accion, entidad, creadoEn);
    }

    // ================= permisos =================

    @Test
    void sinSesion_401_yConRolDePostulante_403ErrAuth07() throws Exception {
        cuenta("2", "POSTULANTE");
        RequestPostProcessor postulante = bearer(login("2"));

        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
        mockMvc.perform(get(RUTA).with(postulante)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get(RUTA + "/acciones").with(postulante)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }

    // ================= listado y forma de la respuesta =================

    @Test
    void listado_devuelveLaMasRecientePrimeroSinIdsInternosYConSuMeta() throws Exception {
        long adminId = sesionDeAdministrador();
        fila(adminId, "ADJUDICACION_BECA", "adjudicacion", "2026-09-29T21:00:00Z");
        fila(adminId, "AUDITORIA_EXPEDIENTE", "finalista", "2026-09-30T14:42:00Z");
        fila(null, "CIERRE_AUTOMATICO_CONVOCATORIA", "convocatoria", "2026-09-28T10:00:00Z");

        mockMvc.perform(get(RUTA).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].accion").value("AUDITORIA_EXPEDIENTE"))
                .andExpect(jsonPath("$.data[0].usuario.correo").value("u1@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data[0].ip").value("10.0.3.21"))
                .andExpect(jsonPath("$.data[0].detalle.convocatoria").value("abc"))
                .andExpect(jsonPath("$.data[0].entidadId").doesNotExist())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[2].accion").value("CIERRE_AUTOMATICO_CONVOCATORIA"))
                .andExpect(jsonPath("$.data[2].usuario").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.totalItems").value(3))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void listado_paginaYOrdenaPorElCampoPedido() throws Exception {
        long adminId = sesionDeAdministrador();
        for (int i = 1; i <= 5; i++) {
            fila(adminId, "ACCION_" + i, "entidad", "2026-09-0" + i + "T12:00:00Z");
        }

        mockMvc.perform(get(RUTA).with(admin).param("page", "2").param("pageSize", "2").param("sort", "accion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].accion").value("ACCION_3"))
                .andExpect(jsonPath("$.data[1].accion").value("ACCION_4"))
                .andExpect(jsonPath("$.meta.totalItems").value(5))
                .andExpect(jsonPath("$.meta.totalPages").value(3));
    }

    // ================= filtros =================

    @Test
    void filtros_porAccionYPorUsuarioSeCombinan() throws Exception {
        long adminId = sesionDeAdministrador();
        cuenta("2", "POSTULANTE");
        long otroId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '2'", Long.class);
        fila(adminId, "ADJUDICACION_BECA", "adjudicacion", "2026-09-30T14:00:00Z");
        fila(adminId, "AUDITORIA_EXPEDIENTE", "finalista", "2026-09-30T15:00:00Z");
        fila(otroId, "ADJUDICACION_BECA", "adjudicacion", "2026-09-30T16:00:00Z");

        mockMvc.perform(get(RUTA).with(admin).param("filter[accion]", "ADJUDICACION_BECA"))
                .andExpect(jsonPath("$.meta.totalItems").value(2));
        // El usuario se busca por nombres, apellidos o correo, sin distinguir mayusculas.
        mockMvc.perform(get(RUTA).with(admin).param("search", "APELLIDO2"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].usuario.correo").value("u2@uagrm.edu.bo"));
        mockMvc.perform(get(RUTA).with(admin).param("search", "u1@uagrm").param("filter[accion]",
                "ADJUDICACION_BECA"))
                .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get(RUTA + "/acciones").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("ADJUDICACION_BECA"))
                .andExpect(jsonPath("$.data[1]").value("AUDITORIA_EXPEDIENTE"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void filtros_porFechasIncluyenElDiaCompletoEnHoraDeLaPaz() throws Exception {
        long adminId = sesionDeAdministrador();
        // 23:30 del 30/9 en La Paz = 03:30 UTC del 1/10: sigue siendo 30/9 para el Administrador.
        fila(adminId, "A_FIN_DE_MES", "x", "2026-10-01T03:30:00Z");
        // 00:30 del 1/10 en La Paz = 04:30 UTC.
        fila(adminId, "B_INICIO_DE_MES", "x", "2026-10-01T04:30:00Z");
        // 20:00 del 31/8 en La Paz = 00:00 UTC del 1/9.
        fila(adminId, "C_ANTERIOR", "x", "2026-09-01T00:00:00Z");

        mockMvc.perform(get(RUTA).with(admin).param("filter[desde]", "2026-09-01").param("filter[hasta]",
                "2026-09-30").param("sort", "accion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].accion").value("A_FIN_DE_MES"));
        mockMvc.perform(get(RUTA).with(admin).param("filter[desde]", "2026-10-01"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].accion").value("B_INICIO_DE_MES"));
        mockMvc.perform(get(RUTA).with(admin).param("filter[hasta]", "2026-08-31"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].accion").value("C_ANTERIOR"));
    }

    @Test
    void sinCoincidencias_devuelveListaVaciaConMeta() throws Exception {
        sesionDeAdministrador();

        mockMvc.perform(get(RUTA).with(admin).param("filter[accion]", "NO_EXISTE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    // ================= errores =================

    @Test
    void parametrosInvalidos_rechazanConSuCodigo() throws Exception {
        sesionDeAdministrador();

        mockMvc.perform(get(RUTA).with(admin).param("filter[desde]", "2026-09-30").param("filter[hasta]",
                "2026-09-01")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_BIT_02"));
        mockMvc.perform(get(RUTA).with(admin).param("sort", "ip")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_BIT_01"));
        mockMvc.perform(get(RUTA).with(admin).param("filter[desde]", "30/09/2026"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(RUTA).with(admin).param("filter[usuario]", "x")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(RUTA).with(admin).param("page", "0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    // ================= solo lectura =================

    @Test
    void laBitacoraNoAdmiteEscritura() throws Exception {
        long adminId = sesionDeAdministrador();
        fila(adminId, "ADJUDICACION_BECA", "adjudicacion", "2026-09-30T14:00:00Z");
        UUID codigo = jdbc.queryForObject("SELECT codigo_bitacora FROM bitacora", UUID.class);

        mockMvc.perform(post(RUTA).with(admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(put(RUTA + "/" + codigo).with(admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().is4xxClientError());
        mockMvc.perform(delete(RUTA + "/" + codigo).with(admin)).andExpect(status().is4xxClientError());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE eliminado_en IS NULL", Long.class))
                .isEqualTo(1);
    }

    @Test
    void consultarLaBitacoraNoDejaRastroEnLaBitacora() throws Exception {
        sesionDeAdministrador();

        mockMvc.perform(get(RUTA).with(admin)).andExpect(status().isOk());
        mockMvc.perform(get(RUTA + "/acciones").with(admin)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM bitacora", Long.class)).isZero();
    }

    // ================= ayudas =================

    private void cuenta(String ci, String rol) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos, activo)
                SELECT id, ?, ?, ?, ?, ?, true FROM rol WHERE nombre = ?""",
                ci, "u" + ci + "@uagrm.edu.bo", hashClave, "Nombre" + ci, "Apellido" + ci, rol);
    }

    /** Inicia sesion de verdad (JWT firmado) y devuelve la sesion (`accessToken`, `refreshToken`...). */
    private JsonNode login(String ci) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted("u" + ci + "@uagrm.edu.bo", CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(cuerpo).get("data");
    }

    private RequestPostProcessor bearer(JsonNode sesion) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + sesion.get("accessToken").asText());
            return request;
        };
    }
}
