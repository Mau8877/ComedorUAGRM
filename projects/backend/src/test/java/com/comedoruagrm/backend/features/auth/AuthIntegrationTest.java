package com.comedoruagrm.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Flujo completo del CU01 contra un Postgres real: migraciones de Flyway,
 * indices unicos, filtro de usuario y bitacora. Solo se simula el
 * JwtDecoder (los tokens de prueba son de mentira; no se llama a Firebase).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @BeforeEach
    void limpiarYPrepararTokens() {
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
        // doThrow/doReturn (y no when(...)) porque llamar a decode() sobre el mock ya stubbeado lanzaria.
        doThrow(new BadJwtException("token desconocido")).when(jwtDecoder).decode(anyString());
        doReturn(jwt("uid-ana", "ana@uagrm.edu.bo")).when(jwtDecoder).decode("token-ana");
        doReturn(jwt("uid-luis", "luis@uagrm.edu.bo")).when(jwtDecoder).decode("token-luis");
    }

    @Test
    void registro_conUnTokenNuevo_creaUsuarioYPostulanteYResponde201() throws Exception {
        registrarAna().andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"));

        assertThat(contar("usuario")).isEqualTo(1);
        assertThat(contar("postulante")).isEqualTo(1);
        assertThat(contarBitacora("REGISTRO_USUARIO")).isEqualTo(1);
    }

    @Test
    void registro_repetidoConElMismoToken_responde200SinDuplicarFilas() throws Exception {
        registrarAna().andExpect(status().isCreated());

        registrarAna().andExpect(status().isOk());

        assertThat(contar("usuario")).isEqualTo(1);
        assertThat(contar("postulante")).isEqualTo(1);
        assertThat(contarBitacora("REGISTRO_USUARIO")).isEqualTo(1);
    }

    @Test
    void registro_conOtroTokenYElMismoCi_responde409ErrUs01() throws Exception {
        registrarAna().andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro").header(HttpHeaders.AUTHORIZATION, "Bearer token-luis")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyRegistro("1234567", "219999999")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_01"));

        assertThat(contar("usuario")).isEqualTo(1);
    }

    @Test
    void registro_conOtroTokenYElMismoRegistroUniversitario_responde409ErrUs03() throws Exception {
        registrarAna().andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro").header(HttpHeaders.AUTHORIZATION, "Bearer token-luis")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyRegistro("7654321", "219012345")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_03"));
    }

    @Test
    void sesion_conUnUsuarioRegistrado_devuelveElUsuarioYDejaUnaFilaInicioSesion() throws Exception {
        registrarAna().andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/sesion").header(HttpHeaders.AUTHORIZATION, "Bearer token-ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value("ana@uagrm.edu.bo"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"));

        assertThat(contarBitacora("INICIO_SESION")).isEqualTo(1);
    }

    @Test
    void usuarioActual_noAgregaFilasALaBitacora() throws Exception {
        registrarAna().andExpect(status().isCreated());
        long antes = contar("bitacora");

        mockMvc.perform(get("/api/v1/auth/usuario-actual").header(HttpHeaders.AUTHORIZATION, "Bearer token-ana"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/usuario-actual").header(HttpHeaders.AUTHORIZATION, "Bearer token-ana"))
                .andExpect(status().isOk());

        assertThat(contar("bitacora")).isEqualTo(antes);
    }

    @Test
    void sesion_conElUsuarioDesactivado_responde403ErrAuth03() throws Exception {
        registrarAna().andExpect(status().isCreated());
        jdbc.update("UPDATE usuario SET activo = false WHERE firebase_uid = 'uid-ana'");

        mockMvc.perform(post("/api/v1/auth/sesion").header(HttpHeaders.AUTHORIZATION, "Bearer token-ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));

        assertThat(contarBitacora("INICIO_SESION")).isZero();
    }

    @Test
    void sesion_conUnTokenSinRegistrar_responde401ErrAuth02() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sesion").header(HttpHeaders.AUTHORIZATION, "Bearer token-luis"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_02"));
    }

    @Test
    void carreras_sinToken_responde200ConLasCarrerasDeLaMigracion() throws Exception {
        mockMvc.perform(get("/api/v1/carreras").param("pageSize", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(71));
    }

    private ResultActions registrarAna() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/registro").header(HttpHeaders.AUTHORIZATION, "Bearer token-ana")
                .contentType(MediaType.APPLICATION_JSON).content(bodyRegistro("1234567", "219012345")));
    }

    private String bodyRegistro(String ci, String registroUniversitario) {
        UUID carrera = jdbc.queryForObject("SELECT codigo_carrera FROM carrera LIMIT 1", UUID.class);
        return """
                {"nombres":"Ana","apellidos":"Perez","ci":"%s","registroUniversitario":"%s",
                 "carreraCodigo":"%s","telefono":"70000001"}""".formatted(ci, registroUniversitario, carrera);
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private long contarBitacora(String accion) {
        return jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE accion = ?", Long.class, accion);
    }

    private Jwt jwt(String uid, String correo) {
        return Jwt.withTokenValue("token-de-prueba").header("alg", "RS256").subject(uid).claim("email", correo)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .audience(List.of("comedor-uagrm-test")).build();
    }
}
