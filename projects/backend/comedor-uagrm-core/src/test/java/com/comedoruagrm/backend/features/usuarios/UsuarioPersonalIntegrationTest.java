package com.comedoruagrm.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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

/**
 * Gestion de cuentas del personal (CU14) de punta a punta contra un Postgres real: JWT firmados de verdad,
 * `@PreAuthorize`, sesiones, bitacora y las protecciones del CU. Los flujos A a F y los criterios de
 * aceptacion que solo se pueden demostrar con la base real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioPersonalIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLAVE = "secreta1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String hashClave;

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
        hashClave = passwordEncoder.encode(CLAVE);
    }

    // ================= permisos (A7, CA19, CA20) =================

    @Test
    void sinElRolDeAdministrador_cadaEndpointDa403ErrAuth07() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("9", "PERSONAL_COMEDOR", true);
        for (String ci : List.of("2", "3")) {
            String rol = ci.equals("2") ? "DIRECTIVO" : "POSTULANTE";
            cuenta(ci, rol, true);
            RequestPostProcessor sesion = bearer(login(ci));

            mockMvc.perform(get("/api/v1/usuarios").with(sesion)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
            mockMvc.perform(post("/api/v1/usuarios").with(sesion).contentType(MediaType.APPLICATION_JSON)
                            .content(nuevaCuenta("500", "x500@uagrm.edu.bo", "DIRECTIVO")))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
            mockMvc.perform(patch("/api/v1/usuarios/" + objetivo).with(sesion)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"X\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/cambiar-rol").with(sesion)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"DIRECTIVO\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/desactivar").with(sesion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/reactivar").with(sesion))
                    .andExpect(status().isForbidden());
        }
        assertThat(contarUsuarios()).isEqualTo(4);
        assertThat(contarBitacora("%")).isEqualTo(contarBitacoraDeLogin());
    }

    @Test
    void sinSesion_responde401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    // ================= alta (flujo B) =================

    @Test
    void alta_conCadaRolDelPersonal_creaLaCuentaActivaYDevuelveLaContrasenaTemporalSinCache() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        RequestPostProcessor admin = bearer(login("1"));
        int numero = 100;
        for (String rol : List.of("ADMINISTRADOR_DUBSS", "DIRECTIVO", "PERSONAL_COMEDOR")) {
            String ci = String.valueOf(numero++);
            String correo = "n" + ci + "@uagrm.edu.bo";

            ResultActions respuesta = mockMvc.perform(post("/api/v1/usuarios").with(admin)
                            .contentType(MediaType.APPLICATION_JSON).content(nuevaCuenta(ci, correo, rol)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.data.rol").value(rol))
                    .andExpect(jsonPath("$.data.activo").value(true))
                    .andExpect(jsonPath("$.data.contrasenaTemporal").isNotEmpty());
            String temporal = datos(respuesta).get("contrasenaTemporal").asText();

            // La persona inicia sesion con esa contraseña temporal.
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, temporal)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.usuario.rol").value(rol));
            String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE ci = ?", String.class, ci);
            assertThat(hash).isNotEqualTo(temporal).startsWith("$2");
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM bitacora WHERE detalle::text LIKE ?", Long.class, "%" + temporal + "%"))
                    .isZero();
        }
        assertThat(contarBitacora("ALTA_PERSONAL")).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM postulante", Long.class)).isZero();
    }

    @Test
    void alta_conRolPostulanteOInexistente_da400ErrUs10() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        RequestPostProcessor admin = bearer(login("1"));

        for (String rol : List.of("POSTULANTE", "NO_EXISTE")) {
            mockMvc.perform(post("/api/v1/usuarios").with(admin).contentType(MediaType.APPLICATION_JSON)
                            .content(nuevaCuenta("200", "n200@uagrm.edu.bo", rol)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_US_10"));
        }
        assertThat(contarUsuarios()).isEqualTo(1);
        assertThat(contarBitacora("ALTA_PERSONAL")).isZero();
    }

    @Test
    void alta_conCiOCorreoRepetidos_da409ErrUs01YErrUs02YElReintentoNoDuplica() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        cuenta("2", "DIRECTIVO", true);
        RequestPostProcessor admin = bearer(login("1"));

        mockMvc.perform(post("/api/v1/usuarios").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(nuevaCuenta("2", "otro@uagrm.edu.bo", "DIRECTIVO")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_01"));
        mockMvc.perform(post("/api/v1/usuarios").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(nuevaCuenta("300", correoDe("2"), "DIRECTIVO")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_02"));

        String cuerpo = nuevaCuenta("301", "n301@uagrm.edu.bo", "DIRECTIVO");
        mockMvc.perform(post("/api/v1/usuarios").with(admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/usuarios").with(admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
        assertThat(contarUsuarios()).isEqualTo(3);
        assertThat(contarBitacora("ALTA_PERSONAL")).isEqualTo(1);
    }

    @Test
    void alta_conDatosInvalidos_da400ErrSys01() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);

        mockMvc.perform(post("/api/v1/usuarios").with(bearer(login("1"))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"\",\"apellidos\":\"P\",\"ci\":\"9\",\"correo\":\"mal\",\"rol\":\"DIRECTIVO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    // ================= lista (flujo A, CA06, CA07) =================

    @Test
    void lista_soloMuestraPersonalConFiltrosBusquedaOrdenYPaginacion() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        cuenta("2", "DIRECTIVO", true);
        cuenta("3", "DIRECTIVO", false);
        cuenta("4", "PERSONAL_COMEDOR", true);
        cuenta("5", "POSTULANTE", true);
        UUID eliminada = cuenta("6", "DIRECTIVO", true);
        jdbc.update("UPDATE usuario SET eliminado_en = now() WHERE codigo_usuario = ?", eliminada);
        RequestPostProcessor admin = bearer(login("1"));

        mockMvc.perform(get("/api/v1/usuarios").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(4))
                .andExpect(jsonPath("$.data[?(@.rol=='POSTULANTE')]").isEmpty())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("filter[rol]", "DIRECTIVO"))
                .andExpect(jsonPath("$.meta.totalItems").value(2));
        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("filter[activo]", "false"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].ci").value("3"));
        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("search", "apellido4"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].ci").value("4"));
        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("pageSize", "2").param("page", "2").param("sort", "ci"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].ci").value("3"))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void lista_conPostulanteComoRolOSortInvalido_da400() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        RequestPostProcessor admin = bearer(login("1"));

        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("filter[rol]", "POSTULANTE"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_US_10"));
        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("sort", "passwordHash"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_US_11"));
        mockMvc.perform(get("/api/v1/usuarios").with(admin).param("filter[otro]", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_US_11"));
    }

    // ================= edicion (flujo C) =================

    @Test
    void edicion_cambiaLosDatosNoElCiYAnotaAlAdministradorComoActor() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("2", "DIRECTIVO", true);
        cuenta("3", "DIRECTIVO", true);
        RequestPostProcessor admin = bearer(login("1"));

        mockMvc.perform(patch("/api/v1/usuarios/" + objetivo).with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Beatriz\",\"correo\":\"BEA@uagrm.edu.bo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombres").value("Beatriz"))
                .andExpect(jsonPath("$.data.correo").value("bea@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data.ci").value("2"));
        mockMvc.perform(patch("/api/v1/usuarios/" + objetivo).with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correoDe("3") + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_02"));

        assertThat(contarBitacora("EDICION_USUARIO")).isEqualTo(1);
        Map<String, Object> fila = jdbc.queryForMap("""
                SELECT b.usuario_id = (SELECT id FROM usuario WHERE ci = '1') AS actor_admin,
                       b.entidad_id = (SELECT id FROM usuario WHERE ci = '2') AS entidad_cuenta,
                       b.detalle::text AS detalle
                FROM bitacora b WHERE b.accion = 'EDICION_USUARIO'""");
        assertThat(fila).containsEntry("actor_admin", true).containsEntry("entidad_cuenta", true);
        assertThat(fila.get("detalle").toString()).contains("nombres", "correo").doesNotContain("Beatriz");
    }

    @Test
    void edicion_deUnPostulanteOInexistente_da404ErrUs06() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID postulante = cuenta("2", "POSTULANTE", true);
        RequestPostProcessor admin = bearer(login("1"));

        for (UUID codigo : List.of(postulante, UUID.randomUUID())) {
            mockMvc.perform(patch("/api/v1/usuarios/" + codigo).with(admin)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"X\"}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_US_06"));
        }
        mockMvc.perform(post("/api/v1/usuarios/" + postulante + "/desactivar").with(admin))
                .andExpect(status().isNotFound());
        assertThat(contarBitacora("EDICION_USUARIO")).isZero();
    }

    // ================= cambio de rol (flujo D) =================

    @Test
    void cambioDeRol_cambiaElRolCierraLasSesionesYAnotaRolAnteriorYNuevo() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("2", "DIRECTIVO", true);
        RequestPostProcessor admin = bearer(login("1"));
        String refreshDelObjetivo = login("2").get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/cambiar-rol").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"PERSONAL_COMEDOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rol").value("PERSONAL_COMEDOR"));

        // Sus sesiones se cerraron: tiene que iniciar sesion de nuevo y aterriza con el rol nuevo.
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshDelObjetivo)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_06"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correoDe("2"), CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.usuario.rol").value("PERSONAL_COMEDOR"));

        Map<String, Object> fila = jdbc.queryForMap("""
                SELECT b.usuario_id = (SELECT id FROM usuario WHERE ci = '1') AS actor_admin,
                       b.entidad_id = (SELECT id FROM usuario WHERE ci = '2') AS entidad_cuenta,
                       b.detalle ->> 'rolAnterior' AS anterior, b.detalle ->> 'rolNuevo' AS nuevo,
                       b.ip IS NOT NULL AS con_ip
                FROM bitacora b WHERE b.accion = 'CAMBIO_ROL'""");
        assertThat(fila).containsEntry("actor_admin", true).containsEntry("entidad_cuenta", true)
                .containsEntry("anterior", "DIRECTIVO").containsEntry("nuevo", "PERSONAL_COMEDOR")
                .containsEntry("con_ip", true);
    }

    @Test
    void cambioDeRol_rechazos_noCambianNadaNiAnotan() throws Exception {
        UUID admin1 = cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("2", "DIRECTIVO", true);
        RequestPostProcessor admin = bearer(login("1"));

        // Propio rol, mismo rol, rol de Postulante y cuenta inexistente.
        mockMvc.perform(post("/api/v1/usuarios/" + admin1 + "/cambiar-rol").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"DIRECTIVO\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_US_08"));
        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/cambiar-rol").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"DIRECTIVO\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_12"));
        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/cambiar-rol").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"POSTULANTE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_US_10"));
        mockMvc.perform(post("/api/v1/usuarios/" + UUID.randomUUID() + "/cambiar-rol").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"DIRECTIVO\"}"))
                .andExpect(status().isNotFound());

        assertThat(rolDe("1")).isEqualTo("ADMINISTRADOR_DUBSS");
        assertThat(rolDe("2")).isEqualTo("DIRECTIVO");
        assertThat(contarBitacora("CAMBIO_ROL")).isZero();
    }

    // ================= desactivar y reactivar (flujos E y F) =================

    @Test
    void desactivar_quitaElAccesoCierraLasSesionesYConservaElRegistro() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("2", "DIRECTIVO", true);
        RequestPostProcessor admin = bearer(login("1"));
        JsonNode sesionObjetivo = login("2");

        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/desactivar").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correoDe("2"), CLAVE)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(sesionObjetivo.get("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/usuario-actual").with(bearer(sesionObjetivo)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE ci = '2' AND eliminado_en IS NULL AND NOT activo", Long.class))
                .isEqualTo(1);
        assertThat(contarBitacora("DESACTIVACION_USUARIO")).isEqualTo(1);
    }

    @Test
    void desactivar_laPropiaCuentaOUnaYaDesactivada_dan403Y409() throws Exception {
        UUID admin1 = cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("2", "DIRECTIVO", true);
        RequestPostProcessor admin = bearer(login("1"));

        mockMvc.perform(post("/api/v1/usuarios/" + admin1 + "/desactivar").with(admin))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_US_08"));
        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/desactivar").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/desactivar").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_07"));

        assertThat(contarBitacora("DESACTIVACION_USUARIO")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE ci = '1'", Boolean.class)).isTrue();
    }

    @Test
    void reactivar_recuperaElAccesoConElMismoRolYRepetirDa409() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID objetivo = cuenta("2", "PERSONAL_COMEDOR", false);
        RequestPostProcessor admin = bearer(login("1"));

        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/reactivar").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.rol").value("PERSONAL_COMEDOR"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correoDe("2"), CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.usuario.rol").value("PERSONAL_COMEDOR"));
        mockMvc.perform(post("/api/v1/usuarios/" + objetivo + "/reactivar").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_07"));
        assertThat(contarBitacora("REACTIVACION_USUARIO")).isEqualTo(1);
    }

    // ================= concurrencia (A5) =================

    @Test
    void dosAdministradoresQueSeDesactivanALaVez_nuncaDejanAlSistemaSinAdministradorActivo() throws Exception {
        UUID a = cuenta("1", "ADMINISTRADOR_DUBSS", true);
        UUID b = cuenta("2", "ADMINISTRADOR_DUBSS", true);
        RequestPostProcessor sesionA = bearer(login("1"));
        RequestPostProcessor sesionB = bearer(login("2"));
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> aDesactivaB = hilos.submit(() -> {
                salida.await();
                return mockMvc.perform(post("/api/v1/usuarios/" + b + "/desactivar").with(sesionA))
                        .andReturn().getResponse().getStatus();
            });
            Future<Integer> bDesactivaA = hilos.submit(() -> {
                salida.await();
                return mockMvc.perform(post("/api/v1/usuarios/" + a + "/desactivar").with(sesionB))
                        .andReturn().getResponse().getStatus();
            });
            salida.countDown();
            List<Integer> estados = List.of(aDesactivaB.get(), bDesactivaA.get());

            assertThat(estados).doesNotContain(500);
            assertThat(estados).contains(200);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM usuario u JOIN rol r ON r.id = u.rol_id
                    WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.activo""", Long.class)).isGreaterThanOrEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }
    }

    // ---- ayudas ----

    private String nuevaCuenta(String ci, String correo, String rol) {
        return "{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"ci\":\"%s\",\"correo\":\"%s\",\"rol\":\"%s\"}"
                .formatted(ci, correo, rol);
    }

    private String correoDe(String ci) {
        return "u" + ci + "@uagrm.edu.bo";
    }

    /** Inserta una cuenta directo en la base (con la contraseña `secreta1`) y devuelve su codigo publico. */
    private UUID cuenta(String ci, String rol, boolean activo) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos, activo)
                SELECT id, ?, ?, ?, ?, ?, ? FROM rol WHERE nombre = ?""",
                ci, correoDe(ci), hashClave, "Nombre" + ci, "Apellido" + ci, activo, rol);
        return jdbc.queryForObject("SELECT codigo_usuario FROM usuario WHERE ci = ?", UUID.class, ci);
    }

    /** Inicia sesion de verdad (JWT firmado) y devuelve la sesion (`accessToken`, `refreshToken`...). */
    private JsonNode login(String ci) throws Exception {
        return datos(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correoDe(ci), CLAVE)))
                .andExpect(status().isOk()));
    }

    private RequestPostProcessor bearer(JsonNode sesion) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + sesion.get("accessToken").asText());
            return request;
        };
    }

    private JsonNode datos(ResultActions resultado) throws Exception {
        return JSON.readTree(resultado.andReturn().getResponse().getContentAsString()).get("data");
    }

    private long contarUsuarios() {
        return jdbc.queryForObject("SELECT count(*) FROM usuario", Long.class);
    }

    private long contarBitacora(String accion) {
        return jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE accion LIKE ?", Long.class, accion);
    }

    private long contarBitacoraDeLogin() {
        return contarBitacora("INICIO_SESION");
    }

    private String rolDe(String ci) {
        return jdbc.queryForObject(
                "SELECT r.nombre FROM usuario u JOIN rol r ON r.id = u.rol_id WHERE u.ci = ?", String.class, ci);
    }
}
