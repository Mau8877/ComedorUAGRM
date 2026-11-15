package com.comedoruagrm.backend.features.convocatorias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import com.comedoruagrm.backend.features.convocatorias.service.CierreAutomaticoConvocatoriasJob;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gestion de convocatorias y gestiones academicas (CU13) de punta a punta contra un Postgres real: JWT firmados de
 * verdad, `@PreAuthorize`, bitacora, borrado logico con la clave compuesta de los documentos, la unicidad de una
 * abierta por gestion (incluso con dos aperturas simultaneas) y el cierre automatico.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConvocatoriaIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLAVE = "secreta1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CierreAutomaticoConvocatoriasJob job;

    private String hashClave;
    private RequestPostProcessor admin;
    private UUID tipoCarnet;
    private UUID tipoNotas;

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
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria_documento");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM tipo_documento");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
        hashClave = passwordEncoder.encode(CLAVE);
    }

    private void sesionDeAdministrador() throws Exception {
        cuenta("1", "ADMINISTRADOR_DUBSS");
        admin = bearer(login("1"));
        jdbc.update("INSERT INTO tipo_documento (nombre) VALUES ('Carnet de identidad'), ('Certificado de notas')");
        tipoCarnet = jdbc.queryForObject("SELECT codigo_tipo_documento FROM tipo_documento WHERE nombre = ?",
                UUID.class, "Carnet de identidad");
        tipoNotas = jdbc.queryForObject("SELECT codigo_tipo_documento FROM tipo_documento WHERE nombre = ?",
                UUID.class, "Certificado de notas");
    }

    // ================= permisos =================

    @Test
    void sinSesion_401_yConRolDePostulante_403ErrAuth07() throws Exception {
        cuenta("2", "POSTULANTE");
        RequestPostProcessor postulante = bearer(login("2"));

        mockMvc.perform(get("/api/v1/convocatorias")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
        for (String ruta : List.of("/api/v1/convocatorias", "/api/v1/gestiones-academicas", "/api/v1/tipos-documento")) {
            mockMvc.perform(get(ruta).with(postulante)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
        mockMvc.perform(post("/api/v1/convocatorias").with(postulante).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        assertThat(contar("SELECT count(*) FROM convocatoria")).isZero();
    }

    // ================= gestiones =================

    @Test
    void gestion_seCreaSeEditaYValidaEtiquetaYFechas() throws Exception {
        sesionDeAdministrador();

        UUID gestion = crearGestion("2026-II");
        mockMvc.perform(post("/api/v1/gestiones-academicas").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoGestion("2026-II", "2026-07-01", "2026-12-20")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_12"));
        mockMvc.perform(post("/api/v1/gestiones-academicas").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoGestion("2027-I", "2027-02-01", "2027-02-01")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_CV_13"));
        mockMvc.perform(put("/api/v1/gestiones-academicas/" + gestion).with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoGestion("2026-II", "2026-07-01", "2026-12-30")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.fechaFin").value("2026-12-30"));
        mockMvc.perform(get("/api/v1/gestiones-academicas").with(admin).param("search", "2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(1));

        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'CREACION_GESTION'")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'EDICION_GESTION'")).isEqualTo(1);
    }

    // ================= crear, validar y leer =================

    @Test
    void crear_guardaEnBorradorConDocumentosYFechasEnHoraDeLaPaz() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");

        UUID codigo = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true,
                tipoNotas, false));

        mockMvc.perform(get("/api/v1/convocatorias/" + codigo).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.data.fechaApertura").value("2026-08-01"))
                .andExpect(jsonPath("$.data.fechaCierre").value("2026-08-31"))
                .andExpect(jsonPath("$.data.cantidadFinalistas").value(120))
                .andExpect(jsonPath("$.data.tienePostulaciones").value(false))
                .andExpect(jsonPath("$.data.documentos.length()").value(2));
        // 00:00 y 23:59:59 en La Paz (UTC-4).
        assertThat(jdbc.queryForObject("SELECT fecha_apertura AT TIME ZONE 'UTC' FROM convocatoria", String.class))
                .startsWith("2026-08-01 04:00:00");
        assertThat(jdbc.queryForObject("SELECT fecha_cierre AT TIME ZONE 'UTC' FROM convocatoria", String.class))
                .startsWith("2026-09-01 03:59:59");
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'CREACION_CONVOCATORIA'")).isEqualTo(1);
    }

    @Test
    void crear_conDatosInvalidos_rechazaConSuCodigoYNoGuardaNiAnotaNada() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");

        // Cierre igual a la apertura.
        pedirCrear(gestion, "2026-08-01", "2026-08-01", documentos(tipoCarnet, true))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_CV_04"));
        // Ningun documento obligatorio.
        pedirCrear(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, false))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_CV_05"));
        // Tipo repetido.
        pedirCrear(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true, tipoCarnet, false))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_CV_06"));
        // Tipo y gestion inexistentes.
        pedirCrear(gestion, "2026-08-01", "2026-08-31", documentos(UUID.randomUUID(), true))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_CV_03"));
        pedirCrear(UUID.randomUUID(), "2026-08-01", "2026-08-31", documentos(tipoCarnet, true))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_CV_02"));
        // Valores fuera de rango.
        mockMvc.perform(post("/api/v1/convocatorias").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoConvocatoria(gestion, "2026-08-01", "2026-08-31", "0", "101", "-1",
                                documentos(tipoCarnet, true))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        assertThat(contar("SELECT count(*) FROM convocatoria")).isZero();
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'CREACION_CONVOCATORIA'")).isZero();
    }

    @Test
    void listar_filtraPorEstadoYGestion_yRechazaFiltrosYOrdenesNoSoportados() throws Exception {
        sesionDeAdministrador();
        UUID g1 = crearGestion("2026-I");
        UUID g2 = crearGestion("2026-II");
        UUID a = crearConvocatoria(g1, "2026-02-01", "2026-02-28", documentos(tipoCarnet, true));
        crearConvocatoria(g2, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));
        mockMvc.perform(post("/api/v1/convocatorias/" + a + "/abrir").with(admin)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/convocatorias").with(admin).param("filter[estado]", "ABIERTA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].codigo").value(a.toString()));
        mockMvc.perform(get("/api/v1/convocatorias").with(admin).param("filter[gestion]", g2.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get("/api/v1/convocatorias").with(admin).param("sort", "cupos"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_CV_11"));
        mockMvc.perform(get("/api/v1/convocatorias").with(admin).param("filter[x]", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    // ================= abrir y cerrar =================

    @Test
    void abrir_soloUnaPorGestion_ycerrar() throws Exception {
        sesionDeAdministrador();
        UUID g1 = crearGestion("2026-I");
        UUID g2 = crearGestion("2026-II");
        UUID primera = crearConvocatoria(g1, "2026-02-01", "2026-02-28", documentos(tipoCarnet, true));
        UUID segunda = crearConvocatoria(g1, "2026-03-01", "2026-03-31", documentos(tipoCarnet, true));
        UUID otraGestion = crearConvocatoria(g2, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));

        mockMvc.perform(post("/api/v1/convocatorias/" + primera + "/abrir").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("ABIERTA"));
        mockMvc.perform(post("/api/v1/convocatorias/" + segunda + "/abrir").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_07"));
        mockMvc.perform(post("/api/v1/convocatorias/" + otraGestion + "/abrir").with(admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/convocatorias/" + primera + "/abrir").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_08"));

        mockMvc.perform(post("/api/v1/convocatorias/" + primera + "/cerrar").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("CERRADA"));
        // Con la primera cerrada, la segunda ya se puede abrir; y una cerrada no se abre ni se vuelve a cerrar.
        mockMvc.perform(post("/api/v1/convocatorias/" + segunda + "/abrir").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/convocatorias/" + primera + "/cerrar").with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_08"));

        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'APERTURA_CONVOCATORIA'")).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'CIERRE_CONVOCATORIA'")).isEqualTo(1);
        // Las operaciones rechazadas no dejan fila.
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion LIKE '%CONVOCATORIA'")).isEqualTo(3 + 3 + 1);
    }

    @Test
    void abrir_dosConvocatoriasDeLaMismaGestionAlMismoTiempo_dejaUnaSolaAbierta() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");
        UUID a = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));
        UUID b = crearConvocatoria(gestion, "2026-09-01", "2026-09-30", documentos(tipoCarnet, true));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> intentos = List.of(
                    pool.submit(() -> abrirCuandoSalga(salida, a)),
                    pool.submit(() -> abrirCuandoSalga(salida, b)));
            salida.countDown();
            List<Integer> codigos = List.of(intentos.get(0).get(), intentos.get(1).get());

            assertThat(codigos).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(contar("SELECT count(*) FROM convocatoria WHERE estado = 'ABIERTA'")).isEqualTo(1);
    }

    private int abrirCuandoSalga(CountDownLatch salida, UUID codigo) throws Exception {
        salida.await();
        return mockMvc.perform(post("/api/v1/convocatorias/" + codigo + "/abrir").with(admin)).andReturn()
                .getResponse().getStatus();
    }

    // ================= cierre automatico =================

    @Test
    void cierreAutomatico_cierraLasVencidas_anotaConActorNuloYNoTocaLasVigentes() throws Exception {
        sesionDeAdministrador();
        UUID g1 = crearGestion("2026-I");
        UUID g2 = crearGestion("2026-II");
        UUID vencida = crearConvocatoria(g1, "2026-02-01", "2026-02-28", documentos(tipoCarnet, true));
        UUID vigente = crearConvocatoria(g2, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));
        mockMvc.perform(post("/api/v1/convocatorias/" + vencida + "/abrir").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/convocatorias/" + vigente + "/abrir").with(admin)).andExpect(status().isOk());
        jdbc.update("UPDATE convocatoria SET fecha_cierre = now() - interval '1 hour' WHERE codigo_convocatoria = ?",
                vencida);
        jdbc.update("UPDATE convocatoria SET fecha_apertura = now() - interval '2 hours', "
                + "fecha_cierre = now() + interval '1 day' WHERE codigo_convocatoria = ?", vigente);

        assertThat(job.cerrarVencidas()).isEqualTo(1);
        assertThat(job.cerrarVencidas()).isZero();

        assertThat(estadoDe(vencida)).isEqualTo("CERRADA");
        assertThat(estadoDe(vigente)).isEqualTo("ABIERTA");
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'CIERRE_AUTOMATICO_CONVOCATORIA' "
                + "AND usuario_id IS NULL")).isEqualTo(1);
    }

    // ================= editar =================

    @Test
    void editar_reemplazaLosDocumentos_yUnTipoQuitadoYVueltoAAgregarReactivaLaMismaFila() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");
        UUID codigo = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true,
                tipoNotas, false));

        // Se quita el certificado de notas.
        editar(codigo, gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.documentos.length()").value(1));
        assertThat(contar("SELECT count(*) FROM convocatoria_documento WHERE eliminado_en IS NOT NULL"))
                .isEqualTo(1);
        // Se vuelve a agregar, ahora obligatorio: no viola la clave primaria, reactiva la fila.
        editar(codigo, gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true, tipoNotas, true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.documentos.length()").value(2));

        assertThat(contar("SELECT count(*) FROM convocatoria_documento")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM convocatoria_documento WHERE eliminado_en IS NOT NULL")).isZero();
        assertThat(contar("SELECT count(*) FROM convocatoria_documento WHERE obligatorio")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'EDICION_CONVOCATORIA'")).isEqualTo(2);
    }

    @Test
    void editar_unaCerrada_da409ErrCv08_yUnaEditadaSinCambiosNoSeAnota() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");
        UUID codigo = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));

        editar(codigo, gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true)).andExpect(status().isOk());
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'EDICION_CONVOCATORIA'")).isZero();

        mockMvc.perform(post("/api/v1/convocatorias/" + codigo + "/abrir").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/convocatorias/" + codigo + "/cerrar").with(admin)).andExpect(status().isOk());
        editar(codigo, gestion, "2026-08-01", "2026-09-30", documentos(tipoCarnet, true))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_08"));
    }

    // ================= con postulaciones =================

    @Test
    void conPostulaciones_noSeCambianLaAperturaNiLosDocumentos_niSeElimina_peroSiElCierre() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");
        UUID codigo = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));
        mockMvc.perform(post("/api/v1/convocatorias/" + codigo + "/abrir").with(admin)).andExpect(status().isOk());
        ExpedienteFixtures fixtures = new ExpedienteFixtures(jdbc);
        long convocatoriaId = jdbc.queryForObject(
                "SELECT id FROM convocatoria WHERE codigo_convocatoria = ?", Long.class, codigo);
        fixtures.postulacion(convocatoriaId, fixtures.postulante("9400001"), "pendiente");

        editar(codigo, gestion, "2026-07-20", "2026-08-31", documentos(tipoCarnet, true))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_09"));
        editar(codigo, gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true, tipoNotas, false))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_09"));
        editar(codigo, gestion, "2026-08-01", "2026-09-30", documentos(tipoCarnet, true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tienePostulaciones").value(true));
        mockMvc.perform(delete("/api/v1/convocatorias/" + codigo).with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_10"));
        mockMvc.perform(post("/api/v1/convocatorias/" + codigo + "/cerrar").with(admin)).andExpect(status().isOk());
    }

    // ================= eliminar =================

    @Test
    void eliminar_unaBorradorSinPostulaciones_desapareceConSusDocumentos() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");
        UUID codigo = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));

        mockMvc.perform(delete("/api/v1/convocatorias/" + codigo).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(get("/api/v1/convocatorias/" + codigo).with(admin)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_CV_01"));
        mockMvc.perform(get("/api/v1/convocatorias").with(admin)).andExpect(jsonPath("$.meta.totalItems").value(0));
        // Borrado logico: la fila sigue en la tabla, con sus documentos tambien eliminados.
        assertThat(contar("SELECT count(*) FROM convocatoria WHERE eliminado_en IS NOT NULL")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM convocatoria_documento WHERE eliminado_en IS NULL")).isZero();
        assertThat(contar("SELECT count(*) FROM bitacora WHERE accion = 'ELIMINACION_CONVOCATORIA'")).isEqualTo(1);
    }

    @Test
    void eliminar_unaAbierta_da409ErrCv08() throws Exception {
        sesionDeAdministrador();
        UUID gestion = crearGestion("2026-II");
        UUID codigo = crearConvocatoria(gestion, "2026-08-01", "2026-08-31", documentos(tipoCarnet, true));
        mockMvc.perform(post("/api/v1/convocatorias/" + codigo + "/abrir").with(admin)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/convocatorias/" + codigo).with(admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_CV_08"));
        assertThat(contar("SELECT count(*) FROM convocatoria WHERE eliminado_en IS NOT NULL")).isZero();
    }

    // ================= la cantidad de finalistas no sale por los endpoints del Postulante =================

    @Test
    void cantidadDeFinalistas_noLaDevuelveNingunEndpointDelPostulante() throws Exception {
        cuenta("3", "POSTULANTE");
        RequestPostProcessor postulante = bearer(login("3"));
        ExpedienteFixtures fixtures = new ExpedienteFixtures(jdbc);
        long convocatoria = fixtures.convocatoriaConCupos("PUB", 100, "40", 120);
        long usuario = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '3'", Long.class);
        jdbc.update("INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono) "
                + "VALUES (?, (SELECT min(id) FROM carrera), 'RU-3', '70000000')", usuario);
        fixtures.postulacion(convocatoria, usuario, "pendiente");

        String lista = mockMvc.perform(get("/api/v1/postulaciones/mias").with(postulante))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(lista).doesNotContain("cantidadFinalistas");
    }

    // ================= ayudas =================

    private UUID crearGestion(String etiqueta) throws Exception {
        ResultActions r = mockMvc.perform(post("/api/v1/gestiones-academicas").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoGestion(etiqueta, "2026-01-01", "2026-12-31")))
                .andExpect(status().isCreated());
        return UUID.fromString(datos(r).get("codigo").asText());
    }

    private String cuerpoGestion(String etiqueta, String inicio, String fin) {
        return "{\"etiqueta\":\"%s\",\"fechaInicio\":\"%s\",\"fechaFin\":\"%s\"}".formatted(etiqueta, inicio, fin);
    }

    /** `[tipo1, obligatorio1, tipo2, obligatorio2...]` como JSON de `documentos`. */
    private String documentos(Object... paresTipoYObligatorio) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < paresTipoYObligatorio.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"tipoDocumentoCodigo\":\"%s\",\"obligatorio\":%s}"
                    .formatted(paresTipoYObligatorio[i], paresTipoYObligatorio[i + 1]));
        }
        return json.append(']').toString();
    }

    private String cuerpoConvocatoria(UUID gestion, String apertura, String cierre, String cupos, String puntaje,
            String finalistas, String documentos) {
        return """
                {"nombre":"Beca Comedor","gestionCodigo":"%s","fechaApertura":"%s","fechaCierre":"%s",
                 "cupos":%s,"puntajeMinimo":%s,"cantidadFinalistas":%s,"documentos":%s}"""
                .formatted(gestion, apertura, cierre, cupos, puntaje, finalistas, documentos);
    }

    private ResultActions pedirCrear(UUID gestion, String apertura, String cierre, String documentos)
            throws Exception {
        return mockMvc.perform(post("/api/v1/convocatorias").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConvocatoria(gestion, apertura, cierre, "100", "40.5", "120", documentos)));
    }

    private UUID crearConvocatoria(UUID gestion, String apertura, String cierre, String documentos)
            throws Exception {
        return UUID.fromString(datos(pedirCrear(gestion, apertura, cierre, documentos)
                .andExpect(status().isCreated())).get("codigo").asText());
    }

    private ResultActions editar(UUID codigo, UUID gestion, String apertura, String cierre, String documentos)
            throws Exception {
        return mockMvc.perform(put("/api/v1/convocatorias/" + codigo).with(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConvocatoria(gestion, apertura, cierre, "100", "40.5", "120", documentos)));
    }

    private String estadoDe(UUID codigo) {
        return jdbc.queryForObject("SELECT estado::text FROM convocatoria WHERE codigo_convocatoria = ?",
                String.class, codigo);
    }

    private long contar(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    private UUID cuenta(String ci, String rol) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos, activo)
                SELECT id, ?, ?, ?, ?, ?, true FROM rol WHERE nombre = ?""",
                ci, "u" + ci + "@uagrm.edu.bo", hashClave, "Nombre" + ci, "Apellido" + ci, rol);
        return jdbc.queryForObject("SELECT codigo_usuario FROM usuario WHERE ci = ?", UUID.class, ci);
    }

    /** Inicia sesion de verdad (JWT firmado) y devuelve la sesion (`accessToken`, `refreshToken`...). */
    private JsonNode login(String ci) throws Exception {
        return datos(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted("u" + ci + "@uagrm.edu.bo", CLAVE)))
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
}
