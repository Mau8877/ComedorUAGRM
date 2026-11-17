package com.comedoruagrm.backend.features.becas;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * El padron de becados (CU23) de punta a punta contra un Postgres real: JWT firmados de verdad, `@PreAuthorize`, el
 * alta de los adjudicados, el listado, la baja, la suspension y el reemplazo, con sus movimientos y su bitacora.
 *
 * * La convocatoria de cada prueba tiene 6 finalistas: 1 y 2 adjudicados; 3 y 6 disponibles para reemplazar; 4 con
 * * auditoria «Observado» y 5 no adjudicado (no deben proponerse nunca).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PadronIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLAVE = "secreta1";

    // En orden de dependencia (primero las que apuntan a otras).
    private static final List<String> TABLAS = List.of("movimiento_beca", "beca", "publicacion_resultado",
            "adjudicacion", "auditoria_expediente", "finalista", "puntaje_postulacion", "postulacion",
            "parametro_modelo", "modelo_preseleccion", "bitacora", "sesion", "postulante", "usuario", "convocatoria",
            "gestion_academica");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ExpedienteFixtures fx;
    private String hashClave;
    private RequestPostProcessor admin;
    private long adminId;
    private long convocatoria;
    private UUID convocatoriaCodigo;
    private final long[] postulantes = new long[7];

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
        for (String tabla : TABLAS) {
            jdbc.update("DELETE FROM " + tabla);
        }
        fx = new ExpedienteFixtures(jdbc);
        hashClave = passwordEncoder.encode(CLAVE);
    }

    // ================= permisos y errores de convocatoria =================

    @Test
    void sinSesion_401_yConRolDePostulante_403ErrAuth07() throws Exception {
        escenario(true);
        cuenta("901", "POSTULANTE");
        RequestPostProcessor postulante = bearer(login("901"));
        String ruta = "/api/v1/convocatorias/" + convocatoriaCodigo + "/padron";

        mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        for (ResultActions r : new ResultActions[] {mockMvc.perform(get(ruta).with(postulante)),
                mockMvc.perform(get(ruta + "/resumen").with(postulante)),
                mockMvc.perform(post(ruta + "/alta").with(postulante))}) {
            r.andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
        mockMvc.perform(post("/api/v1/becas/" + UUID.randomUUID() + "/dar-de-baja").with(postulante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        assertThat(contar("SELECT count(*) FROM beca")).isZero();
    }

    @Test
    void convocatoriaInexistente_404ErrBec02() throws Exception {
        escenario(true);
        UUID otra = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/convocatorias/" + otra + "/padron/resumen").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_BEC_02"));
        mockMvc.perform(get("/api/v1/convocatorias/" + otra + "/padron").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_BEC_02"));
        mockMvc.perform(post("/api/v1/convocatorias/" + otra + "/padron/alta").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_BEC_02"));
    }

    // ================= resultados sin publicar =================

    @Test
    void sinPublicar_elResumenLoDice_yElListadoYElAltaResponden409ErrBec03() throws Exception {
        escenario(false);

        mockMvc.perform(get(padron() + "/resumen").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultadosPublicados").value(false))
                .andExpect(jsonPath("$.data.adjudicados").value(2))
                .andExpect(jsonPath("$.data.pendientesDeAlta").value(0));
        mockMvc.perform(get(padron()).with(admin)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_BEC_03"));
        mockMvc.perform(post(padron() + "/alta").with(admin)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_BEC_03"));
        assertThat(contar("SELECT count(*) FROM beca")).isZero();
    }

    // ================= alta =================

    @Test
    void alta_creaUnaBecaVigentePorAdjudicadaSinDuplicarYAnotaLaBitacora() throws Exception {
        escenario(true);

        mockMvc.perform(get(padron() + "/resumen").with(admin))
                .andExpect(jsonPath("$.data.resultadosPublicados").value(true))
                .andExpect(jsonPath("$.data.adjudicados").value(2))
                .andExpect(jsonPath("$.data.pendientesDeAlta").value(2))
                .andExpect(jsonPath("$.data.vigentes").value(0));
        mockMvc.perform(post(padron() + "/alta").with(admin)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.creadas").value(2));

        // Solo las ADJUDICADA (no la NO_ADJUDICADA), vigentes, con su adjudicacion y la fecha de publicacion.
        assertThat(contar("SELECT count(*) FROM beca WHERE estado = 'VIGENTE' AND adjudicacion_id IS NOT NULL"))
                .isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM beca WHERE fecha_inicio = DATE '2026-11-10'")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM beca WHERE postulante_id = " + postulantes[5])).isZero();
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'ALTA_BECAS'")).isEqualTo(1);
        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = 'ALTA_BECAS'",
                String.class);
        assertThat(detalle).contains("\"creadas\": 2").doesNotContain("101").doesNotContain("Rojas");

        // Repetir el alta no duplica ni vuelve a anotar la bitacora.
        mockMvc.perform(post(padron() + "/alta").with(admin)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_BEC_04"));
        assertThat(contar("SELECT count(*) FROM beca")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'ALTA_BECAS'")).isEqualTo(1);
    }

    // ================= listado =================

    @Test
    void listado_muestraNombreCiCarreraYEstado_yFiltraBuscaYOrdena() throws Exception {
        escenario(true);
        alta();
        jdbc.update("UPDATE usuario SET nombres = 'Zoila', apellidos = 'Vaca' WHERE id = ?", postulantes[1]);
        long beca2 = jdbc.queryForObject("SELECT id FROM beca WHERE postulante_id = ?", Long.class, postulantes[2]);
        jdbc.update("UPDATE beca SET estado = 'SUSPENDIDA' WHERE id = ?", beca2);

        mockMvc.perform(get(padron()).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                // Por defecto, por apellido: «Rojas» (2) antes que «Vaca» (1).
                .andExpect(jsonPath("$.data[0].becado.ci").value("102"))
                .andExpect(jsonPath("$.data[0].becado.carrera").isString())
                .andExpect(jsonPath("$.data[0].estado").value("SUSPENDIDA"))
                .andExpect(jsonPath("$.data[0].becado.codigo").isString())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].becado.id").doesNotExist())
                .andExpect(jsonPath("$.data[1].becado.nombres").value("Zoila"))
                .andExpect(jsonPath("$.data[1].fechaInicio").value("2026-11-10"))
                .andExpect(jsonPath("$.meta.totalItems").value(2));
        mockMvc.perform(get(padron()).with(admin).param("search", "zoi"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].becado.ci").value("101"));
        mockMvc.perform(get(padron()).with(admin).param("search", "102"))
                .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get(padron()).with(admin).param("filter[estado]", "VIGENTE"))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.data[0].becado.ci").value("101"));
        mockMvc.perform(get(padron()).with(admin).param("sort", "-ci"))
                .andExpect(jsonPath("$.data[0].becado.ci").value("102"));
        mockMvc.perform(get(padron()).with(admin).param("page", "2").param("pageSize", "1"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
        mockMvc.perform(get(padron() + "/resumen").with(admin))
                .andExpect(jsonPath("$.data.vigentes").value(1))
                .andExpect(jsonPath("$.data.suspendidas").value(1))
                .andExpect(jsonPath("$.data.pendientesDeAlta").value(0));
    }

    @Test
    void listado_conParametrosInvalidos_rechazaConSuCodigo() throws Exception {
        escenario(true);
        alta();

        mockMvc.perform(get(padron()).with(admin).param("sort", "ip")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_BEC_10"));
        mockMvc.perform(get(padron()).with(admin).param("filter[estado]", "X")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(padron()).with(admin).param("filter[carrera]", "x")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(padron()).with(admin).param("page", "0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    // ================= baja y suspension =================

    @Test
    void baja_cambiaElEstadoYDejaMovimientoYBitacoraSinElMotivo() throws Exception {
        escenario(true);
        alta();
        UUID beca = becaDe(1);

        mockMvc.perform(post("/api/v1/becas/" + beca + "/dar-de-baja").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"  Dejó la universidad  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("BAJA"))
                .andExpect(jsonPath("$.data.becado.ci").value("101"));

        assertThat(jdbc.queryForObject("SELECT estado::text FROM beca WHERE codigo_beca = ?", String.class, beca))
                .isEqualTo("BAJA");
        assertThat(jdbc.queryForObject(
                "SELECT tipo::text || '|' || motivo FROM movimiento_beca WHERE beca_id = (SELECT id FROM beca WHERE codigo_beca = ?)",
                String.class, beca)).isEqualTo("BAJA|Dejó la universidad");
        assertThat(jdbc.queryForObject("SELECT fecha FROM movimiento_beca", java.time.LocalDate.class)).isNotNull();
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'BAJA_BECA'")).isEqualTo(1);
        String detalle = jdbc.queryForObject("SELECT detalle::text FROM bitacora WHERE accion = 'BAJA_BECA'",
                String.class);
        assertThat(detalle).contains("VIGENTE").contains("BAJA").doesNotContain("universidad");

        // Una beca que ya no esta vigente no se vuelve a dar de baja ni se suspende.
        mockMvc.perform(post("/api/v1/becas/" + beca + "/dar-de-baja").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"otra vez\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_05"));
        mockMvc.perform(post("/api/v1/becas/" + beca + "/suspender").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_05"));
        assertThat(contar("SELECT count(*) FROM movimiento_beca")).isEqualTo(1);
    }

    @Test
    void suspension_cambiaElEstado_yUnaSuspendidaNoSePuedeDarDeBaja() throws Exception {
        escenario(true);
        alta();
        UUID beca = becaDe(2);

        mockMvc.perform(post("/api/v1/becas/" + beca + "/suspender").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Sin asistencia\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("SUSPENDIDA"));

        assertThat(contar("SELECT count(*) FROM movimiento_beca WHERE tipo = 'SUSPENSION'")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'SUSPENSION_BECA'")).isEqualTo(1);
        mockMvc.perform(post("/api/v1/becas/" + beca + "/dar-de-baja").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_05"));
    }

    @Test
    void sinMotivoOConBecaInexistente_noCambiaNada() throws Exception {
        escenario(true);
        alta();
        UUID beca = becaDe(1);

        for (String cuerpo : new String[] {"{}", "{\"motivo\":\"\"}", "{\"motivo\":\"   \"}"}) {
            mockMvc.perform(post("/api/v1/becas/" + beca + "/dar-de-baja").with(admin)
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        mockMvc.perform(post("/api/v1/becas/" + beca + "/suspender").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post("/api/v1/becas/" + UUID.randomUUID() + "/dar-de-baja").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_BEC_01"));

        assertThat(contar("SELECT count(*) FROM beca WHERE estado = 'VIGENTE'")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM movimiento_beca")).isZero();
    }

    // ================= reemplazo =================

    @Test
    void reemplazo_proponeAlSiguienteFinalistaValidadoYCreaSuBecaVinculada() throws Exception {
        escenario(true);
        alta();
        UUID liberada = becaDe(1);
        baja(liberada);

        // El tercero de la lista cumple; el cuarto esta «Observado» y el quinto no fue adjudicado.
        mockMvc.perform(get("/api/v1/becas/" + liberada + "/reemplazo").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ci").value("103")).andExpect(jsonPath("$.data.posicion").value(3))
                .andExpect(jsonPath("$.data.puntajeTotal").value(75.0))
                .andExpect(jsonPath("$.data.carrera").isString())
                .andExpect(jsonPath("$.data.postulanteCodigo").isString());
        UUID candidato = codigoPostulante(3);

        mockMvc.perform(post("/api/v1/becas/" + liberada + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(candidato)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.becaLiberada.estado").value("BAJA"))
                .andExpect(jsonPath("$.data.becaReemplazante.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.data.becaReemplazante.becado.ci").value("103"));

        assertThat(contar("SELECT count(*) FROM beca")).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM beca WHERE postulante_id = " + postulantes[3]
                + " AND estado = 'VIGENTE' AND adjudicacion_id IS NULL")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM movimiento_beca m JOIN beca b ON b.id = m.beca_id "
                + "WHERE m.tipo = 'REEMPLAZO' AND b.codigo_beca = '" + liberada + "' "
                + "AND m.beca_reemplazante_id = (SELECT id FROM beca WHERE postulante_id = " + postulantes[3]
                + ")")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'REEMPLAZO_BECA'")).isEqualTo(1);
        // La postulacion del reemplazante no cambia de estado ni recibe adjudicacion (decision pendiente del CU).
        assertThat(jdbc.queryForObject("SELECT estado::text FROM postulacion WHERE postulante_id = ?", String.class,
                postulantes[3])).isEqualTo("calificada");
        assertThat(contar("SELECT count(*) FROM adjudicacion")).isEqualTo(3);

        // Una beca se reemplaza una sola vez.
        mockMvc.perform(get("/api/v1/becas/" + liberada + "/reemplazo").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_09"));
        mockMvc.perform(post("/api/v1/becas/" + liberada + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(codigoPostulante(6))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_09"));
        assertThat(contar("SELECT count(*) FROM beca")).isEqualTo(3);
    }

    @Test
    void reemplazo_conElCandidatoCambiado_409ErrBec08_yLuegoConElNuevoFunciona() throws Exception {
        escenario(true);
        alta();
        UUID primera = becaDe(1);
        baja(primera);
        UUID vistoAntes = codigoPostulante(3);
        UUID segunda = becaDe(2);
        baja(segunda);
        mockMvc.perform(post("/api/v1/becas/" + primera + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(vistoAntes)))
                .andExpect(status().isOk());

        // El administrador vio al tercero, pero ya ocupo el cupo de la otra baja: ahora toca al sexto.
        mockMvc.perform(post("/api/v1/becas/" + segunda + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(vistoAntes)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_08"));
        mockMvc.perform(get("/api/v1/becas/" + segunda + "/reemplazo").with(admin))
                .andExpect(jsonPath("$.data.ci").value("106")).andExpect(jsonPath("$.data.posicion").value(6));
        mockMvc.perform(post("/api/v1/becas/" + segunda + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(codigoPostulante(6))))
                .andExpect(status().isOk());
        assertThat(contar("SELECT count(*) FROM beca WHERE estado = 'VIGENTE'")).isEqualTo(2);
    }

    @Test
    void reemplazo_sinBajaOSinCandidato_409() throws Exception {
        escenario(true);
        alta();
        UUID vigente = becaDe(1);

        mockMvc.perform(get("/api/v1/becas/" + vigente + "/reemplazo").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_06"));
        mockMvc.perform(post("/api/v1/becas/" + vigente + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(codigoPostulante(3))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_06"));
        mockMvc.perform(get("/api/v1/becas/" + UUID.randomUUID() + "/reemplazo").with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_BEC_01"));
        mockMvc.perform(post("/api/v1/becas/" + vigente + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        // Se agotan los candidatos (3 y 6): con dos bajas mas, la tercera baja ya no tiene reemplazo.
        baja(vigente);
        UUID otra = becaDe(2);
        baja(otra);
        mockMvc.perform(post("/api/v1/becas/" + vigente + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(codigoPostulante(3))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/becas/" + otra + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(codigoPostulante(6))))
                .andExpect(status().isOk());
        UUID nueva = becaDe(3);
        baja(nueva);
        mockMvc.perform(get("/api/v1/becas/" + nueva + "/reemplazo").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_07"));
        mockMvc.perform(post("/api/v1/becas/" + nueva + "/reemplazar").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReemplazo(codigoPostulante(3))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_BEC_07"));
    }

    // ================= solo lectura =================

    @Test
    void lasConsultasNoEscribenNada() throws Exception {
        escenario(true);
        alta();
        UUID beca = becaDe(1);
        baja(beca);
        long becas = contar("SELECT count(*) FROM beca");
        long movimientos = contar("SELECT count(*) FROM movimiento_beca");
        long bitacora = contar("SELECT count(*) FROM bitacora");

        mockMvc.perform(get(padron() + "/resumen").with(admin)).andExpect(status().isOk());
        mockMvc.perform(get(padron()).with(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/becas/" + beca + "/reemplazo").with(admin)).andExpect(status().isOk());

        assertThat(contar("SELECT count(*) FROM beca")).isEqualTo(becas);
        assertThat(contar("SELECT count(*) FROM movimiento_beca")).isEqualTo(movimientos);
        assertThat(contar("SELECT count(*) FROM bitacora")).isEqualTo(bitacora);
    }

    // ================= escenario y ayudas =================

    /**
     * Una convocatoria cerrada con cupos 2, minimo 70 y seis finalistas; con `publicada`, tambien su publicacion
     * (10/11/2026 14:00 UTC, que son las 10:00 en La Paz). Deja al administrador con sesion iniciada.
     */
    private void escenario(boolean publicada) throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS");
        admin = bearer(login("1"));
        jdbc.update("DELETE FROM bitacora");
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '1'", Long.class);

        convocatoria = fx.convocatoriaConCupos("padron", 2, "70", 6);
        convocatoriaCodigo = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?",
                UUID.class, convocatoria);
        // posicion, puntaje IA, puntaje academico, resultado de la auditoria
        Object[][] filas = {
                {1, "50", "30", "VALIDADO"}, {2, "48", "30", "VALIDADO"}, {3, "45", "30", "VALIDADO"},
                {4, "40", "30", "OBSERVADO"}, {5, "38", "20", "VALIDADO"}, {6, "60", "30", "VALIDADO"}};
        for (Object[] f : filas) {
            int n = (int) f[0];
            postulantes[n] = fx.postulante("10" + n);
            long postulacion = fx.postulacion(convocatoria, postulantes[n], "calificada");
            fx.puntajeIa(postulacion, (String) f[1], "{}");
            long finalista = fx.finalista(postulacion, n);
            fx.auditoria(finalista, adminId, (String) f[3], null, (String) f[2], "2026-11-06T10:00:00Z");
            if (n <= 2) {
                fx.adjudicacion(postulacion, "ADJUDICADA", "80.00", adminId);
                jdbc.update("UPDATE postulacion SET estado = CAST('adjudicada' AS estado_postulacion) WHERE id = ?",
                        postulacion);
            }
            if (n == 5) {
                fx.adjudicacion(postulacion, "NO_ADJUDICADA", null, adminId);
                jdbc.update("UPDATE postulacion SET estado = CAST('no_adjudicada' AS estado_postulacion) "
                        + "WHERE id = ?", postulacion);
            }
        }
        if (publicada) {
            jdbc.update("""
                    INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en)
                    VALUES (?, ?, CAST('2026-11-10T14:00:00Z' AS timestamptz))""", convocatoria, adminId);
        }
    }

    private String padron() {
        return "/api/v1/convocatorias/" + convocatoriaCodigo + "/padron";
    }

    private void alta() throws Exception {
        mockMvc.perform(post(padron() + "/alta").with(admin)).andExpect(status().isCreated());
    }

    private void baja(UUID beca) throws Exception {
        mockMvc.perform(post("/api/v1/becas/" + beca + "/dar-de-baja").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Baja de prueba\"}"))
                .andExpect(status().isOk());
    }

    private UUID becaDe(int n) {
        return jdbc.queryForObject("SELECT codigo_beca FROM beca WHERE postulante_id = ?", UUID.class,
                postulantes[n]);
    }

    private UUID codigoPostulante(int n) {
        return jdbc.queryForObject("SELECT codigo_postulante FROM postulante WHERE usuario_id = ?", UUID.class,
                postulantes[n]);
    }

    private String cuerpoReemplazo(UUID postulante) {
        return "{\"postulanteCodigo\":\"" + postulante + "\"}";
    }

    private long contar(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

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
