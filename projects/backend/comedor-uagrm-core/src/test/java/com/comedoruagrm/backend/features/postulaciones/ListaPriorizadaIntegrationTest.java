package com.comedoruagrm.backend.features.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.config.DatosPruebaListaPriorizada;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lista priorizada (CU06) de punta a punta contra un Postgres real: orden, posicion, estados
 * excluidos, puntaje vigente unico, desempate, filtros con posicion real, paginacion, roles,
 * exportacion a PDF con su bitacora y el cargador de datos de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListaPriorizadaIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String LISTA = "/api/v1/lista-priorizada";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transacciones;

    private UUID convocatoria;
    private UUID convocatoriaVacia;
    private UUID carreraA;
    private UUID carreraB;
    private long modelo;
    private RequestPostProcessor admin;

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
        crearUsuario("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS", "Admin", "DUBSS");
        List<Long> carreras = jdbc.queryForList("SELECT id FROM carrera ORDER BY id LIMIT 2", Long.class);
        carreraA = jdbc.queryForObject("SELECT codigo_carrera FROM carrera WHERE id = ?", UUID.class,
                carreras.get(0));
        carreraB = jdbc.queryForObject("SELECT codigo_carrera FROM carrera WHERE id = ?", UUID.class,
                carreras.get(1));

        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES ('2026-II', DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class);
        long adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        modelo = jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (creado_por, version, activo) VALUES (?, 'v1', false)
                RETURNING id""", Long.class, adminId);
        long conv = nuevaConvocatoria(gestion, adminId, "Beca Comedor 2026-II", "CERRADA");
        long vacia = nuevaConvocatoria(gestion, adminId, "Convocatoria sin postulaciones", "BORRADOR");
        convocatoria = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?",
                UUID.class, conv);
        convocatoriaVacia = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?",
                UUID.class, vacia);

        // Orden esperado: Ana 66.5 (1), Bruno 63.0 (2), Carla 63.0 (3, empate con Bruno pero enviada
        // despues), Diego 59.5 (4, con un puntaje anterior de 69.3 no vigente), Elena 56.0 (5).
        // La IA califica de 0 a 70 (CU07): son los puntajes de antes por 0.7.
        postulacion(conv, "Ana", "Rojas", "1000001", carreras.get(0), "calificada", "2026-08-01T10:00:00Z", 66.5);
        postulacion(conv, "Bruno", "Mamani", "1000002", carreras.get(1), "calificada", "2026-08-01T10:01:00Z", 63.0);
        postulacion(conv, "Carla", "Peña", "1000003", carreras.get(0), "calificada", "2026-08-01T10:02:00Z", 63.0);
        long diego = postulacion(conv, "Diego", "Suárez", "1000004", carreras.get(0), "calificada",
                "2026-08-01T10:03:00Z", 59.5);
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, 69.3, now() - interval '30 days', false)""", modelo, diego);
        postulacion(conv, "Elena", "Flores", "1000005", carreras.get(0), "calificada", "2026-08-01T10:04:00Z", 56.0);
        // Fuera de la lista: pendiente (sin puntaje) y devuelta (con puntaje alto).
        postulacion(conv, "Fabio", "Vaca", "1000006", carreras.get(0), "pendiente", "2026-08-01T10:05:00Z", null);
        postulacion(conv, "Gina", "Egüez", "1000007", carreras.get(0), "devuelta", "2026-08-01T10:06:00Z", 70.0);
        // CU20: tampoco aparecen una postulacion archivada ni una no adjudicada (con puntaje alto).
        postulacion(conv, "Hugo", "Arce", "1000008", carreras.get(0), "archivada", "2026-08-01T10:07:00Z", 69.0);
        postulacion(conv, "Inés", "Bravo", "1000009", carreras.get(0), "no_adjudicada", "2026-08-01T10:08:00Z", 68.0);

        this.admin = bearer(login("admin@dubss.test", "secreta1"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- consulta ----

    @Test
    void lista_ordenaPorPuntajeVigenteConDesempateYDejaFueraLosOtrosEstados() throws Exception {
        JsonNode datos = listar("filter[convocatoriaId]=" + convocatoria).get("data");

        assertThat(nombres(datos)).containsExactly("Ana", "Bruno", "Carla", "Diego", "Elena");
        assertThat(posiciones(datos)).containsExactly(1L, 2L, 3L, 4L, 5L);
        // Diego aparece una sola vez y con su puntaje vigente (59.5), no con el anterior (69.3).
        assertThat(datos.get(3).get("puntaje").decimalValue()).isEqualByComparingTo("59.50");
        assertThat(datos.get(0).get("carrera").get("codigo").asText()).isEqualTo(carreraA.toString());
        assertThat(datos.get(0).has("passwordHash")).isFalse();
    }

    @Test
    void lista_paginaConMetaYLaPosicionContinuaEnLaPaginaDos() throws Exception {
        JsonNode pagina = listar("filter[convocatoriaId]=" + convocatoria + "&page=2&pageSize=2");

        assertThat(posiciones(pagina.get("data"))).containsExactly(3L, 4L);
        assertThat(pagina.get("meta").get("totalItems").asLong()).isEqualTo(5);
        assertThat(pagina.get("meta").get("totalPages").asInt()).isEqualTo(3);
        assertThat(pagina.get("meta").get("page").asInt()).isEqualTo(2);
    }

    @Test
    void lista_filtrandoPorCarreraOBuscando_conservaLaPosicionRealDeCadaUno() throws Exception {
        JsonNode porCarrera = listar("filter[convocatoriaId]=" + convocatoria + "&filter[carreraId]=" + carreraB);
        assertThat(nombres(porCarrera.get("data"))).containsExactly("Bruno");
        assertThat(posiciones(porCarrera.get("data"))).containsExactly(2L);

        JsonNode porNombre = listar("filter[convocatoriaId]=" + convocatoria + "&search=ELENA");
        assertThat(posiciones(porNombre.get("data"))).containsExactly(5L);

        JsonNode porCi = listar("filter[convocatoriaId]=" + convocatoria + "&search=1000003");
        assertThat(nombres(porCi.get("data"))).containsExactly("Carla");
        assertThat(posiciones(porCi.get("data"))).containsExactly(3L);

        JsonNode sinResultados = listar("filter[convocatoriaId]=" + convocatoria + "&search=zzz");
        assertThat(sinResultados.get("data")).isEmpty();
        assertThat(sinResultados.get("meta").get("totalItems").asLong()).isZero();
    }

    @Test
    void lista_unReprocesoCambiaElOrdenEnLaSiguienteConsulta() throws Exception {
        long elena = jdbc.queryForObject("""
                SELECT po.id FROM postulacion po JOIN usuario u ON u.id = po.postulante_id WHERE u.ci = '1000005'""",
                Long.class);
        jdbc.update("UPDATE puntaje_postulacion SET vigente = false WHERE postulacion_id = ?", elena);
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, 68.25, now(), true)""", modelo, elena);

        JsonNode datos = listar("filter[convocatoriaId]=" + convocatoria).get("data");

        assertThat(nombres(datos)).containsExactly("Elena", "Ana", "Bruno", "Carla", "Diego");
    }

    @Test
    void lista_conSortOFiltroNoSoportado_da400ErrPri02() throws Exception {
        mockMvc.perform(get(LISTA + "?filter[convocatoriaId]=" + convocatoria + "&sort=-puntaje").with(admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PRI_02"));
        mockMvc.perform(get(LISTA + "?filter[convocatoriaId]=" + convocatoria + "&filter[estado]=calificada")
                .with(admin)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PRI_02"));
    }

    @Test
    void lista_conUnaConvocatoriaInexistente_da404ErrPri01() throws Exception {
        mockMvc.perform(get(LISTA + "?filter[convocatoriaId]=" + UUID.randomUUID()).with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    @Test
    void lista_sinSesionDa401_yUnPostulanteDa403() throws Exception {
        mockMvc.perform(get(LISTA + "?filter[convocatoriaId]=" + convocatoria))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_01"));

        crearUsuario("ana@uagrm.test", "2000001", "POSTULANTE", "Ana", "Perez");
        RequestPostProcessor postulante = bearer(login("ana@uagrm.test", "secreta1"));
        mockMvc.perform(get(LISTA + "?filter[convocatoriaId]=" + convocatoria).with(postulante))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(get(LISTA + "/resumen?filter[convocatoriaId]=" + convocatoria).with(postulante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(LISTA + "/convocatorias").with(postulante)).andExpect(status().isForbidden());
    }

    @Test
    void resumen_cuentaCalificadasYPendientes_yLasConvocatoriasVienenConSuGestion() throws Exception {
        mockMvc.perform(get(LISTA + "/resumen?filter[convocatoriaId]=" + convocatoria).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCalificadas").value(5))
                .andExpect(jsonPath("$.data.totalPendientes").value(1));

        mockMvc.perform(get(LISTA + "/convocatorias").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].gestion").value("2026-II"))
                .andExpect(jsonPath("$.data[0].codigo").exists());
    }

    @Test
    void consultar_noCambiaElEstadoDeNingunaPostulacion() throws Exception {
        String antes = estados();
        listar("filter[convocatoriaId]=" + convocatoria);
        mockMvc.perform(get(LISTA + "/resumen?filter[convocatoriaId]=" + convocatoria).with(admin));

        assertThat(estados()).isEqualTo(antes);
        assertThat(contarBitacora()).isZero();
    }

    // ---- exportacion ----

    @Test
    void exportar_devuelveElPdfConLosFiltrosYAnotaLaBitacoraSinDatosPersonales() throws Exception {
        MvcResult resultado = mockMvc.perform(post(LISTA + "/exportaciones").with(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\",\"carreraId\":\"%s\",\"search\":\"ana\"}"
                        .formatted(convocatoria, carreraA)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdf = resultado.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").contains("lista-priorizada-beca-comedor-2026-ii-").contains(".pdf");

        assertThat(contarBitacora()).isEqualTo(1);
        String detalle = jdbc.queryForObject(
                "SELECT detalle::text FROM bitacora WHERE accion = 'EXPORTACION_LISTA_PRIORIZADA'", String.class);
        assertThat(detalle).contains("filas").contains("conBusqueda")
                .doesNotContain("Ana").doesNotContain("Rojas").doesNotContain("1000001");
        Long actor = jdbc.queryForObject("SELECT usuario_id FROM bitacora WHERE accion = 'EXPORTACION_LISTA_PRIORIZADA'", Long.class);
        assertThat(actor).isEqualTo(jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class));
        assertThat(jdbc.queryForObject("SELECT entidad FROM bitacora WHERE accion = 'EXPORTACION_LISTA_PRIORIZADA'", String.class)).isEqualTo("convocatoria");
    }

    @Test
    void exportar_conLaListaVacia_da400ErrPri03SinAnotarLaBitacora() throws Exception {
        mockMvc.perform(post(LISTA + "/exportaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\"}".formatted(convocatoriaVacia)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PRI_03"));
        mockMvc.perform(post(LISTA + "/exportaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\",\"search\":\"zzz\"}".formatted(convocatoria)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_PRI_03"));
        assertThat(contarBitacora()).isZero();
    }

    @Test
    void exportar_conUnaConvocatoriaInexistenteOSinCuerpo_da404Y400() throws Exception {
        mockMvc.perform(post(LISTA + "/exportaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
        mockMvc.perform(post(LISTA + "/exportaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        assertThat(contarBitacora()).isZero();
    }

    // ---- cargador de datos de prueba y rendimiento ----

    @Test
    void cargadorDeDatosDePrueba_cargaUnaVezYLaListaSeVe() throws Exception {
        DatosPruebaListaPriorizada cargador = nuevoCargador(60);

        cargador.run();
        cargador.run();

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre LIKE '[PRUEBA]%'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE correo LIKE '%@prueba.local'", Integer.class)).isEqualTo(60);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM (SELECT postulacion_id FROM puntaje_postulacion WHERE vigente
                                      GROUP BY postulacion_id HAVING count(*) > 1) t""", Integer.class)).isZero();
        UUID grande = jdbc.queryForObject(
                "SELECT codigo_convocatoria FROM convocatoria WHERE nombre = '[PRUEBA] Beca Comedor 2026-II'",
                UUID.class);
        JsonNode pagina = listar("filter[convocatoriaId]=" + grande + "&pageSize=50");
        assertThat(pagina.get("meta").get("totalItems").asLong()).isGreaterThan(20);
        List<Long> ordenadas = posiciones(pagina.get("data"));
        assertThat(ordenadas).isSorted();
        assertThat(ordenadas.get(0)).isEqualTo(1L);
    }

    @Test
    void cargadorDeDatosDePrueba_sinAdministradorNoCargaNada() {
        borrarTodo(); // sin ninguna cuenta no hay Administrador DUBSS

        nuevoCargador(60).run();

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre LIKE '[PRUEBA]%'", Integer.class)).isZero();
    }

    @Test
    @EnabledIfSystemProperty(named = "rendimiento", matches = "true")
    void rendimiento_conVeinteMilPostulaciones_cadaPaginaYElPdfResponden() throws Exception {
        long inicioCarga = System.currentTimeMillis();
        nuevoCargador(20_000).run();
        System.out.println("RENDIMIENTO> carga de 20000 postulantes: " + (System.currentTimeMillis() - inicioCarga) + " ms");
        UUID grande = jdbc.queryForObject(
                "SELECT codigo_convocatoria FROM convocatoria WHERE nombre = '[PRUEBA] Beca Comedor 2026-II'",
                UUID.class);

        for (int pagina : new int[] {1, 2, 100, 350, 700}) {
            long inicio = System.currentTimeMillis();
            JsonNode respuesta = listar("filter[convocatoriaId]=" + grande + "&page=" + pagina + "&pageSize=20");
            long tardo = System.currentTimeMillis() - inicio;
            System.out.println("RENDIMIENTO> pagina " + pagina + ": " + tardo + " ms, total="
                    + respuesta.get("meta").get("totalItems").asLong());
            assertThat(tardo).as("pagina " + pagina).isLessThan(3000);
        }
        long inicioBusqueda = System.currentTimeMillis();
        listar("filter[convocatoriaId]=" + grande + "&search=garcia");
        assertThat(System.currentTimeMillis() - inicioBusqueda).isLessThan(3000);

        long inicioPdf = System.currentTimeMillis();
        MvcResult pdf = mockMvc.perform(post(LISTA + "/exportaciones").with(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"convocatoriaId\":\"%s\"}".formatted(grande))).andExpect(status().isOk()).andReturn();
        System.out.println("RENDIMIENTO> PDF de toda la lista: " + (System.currentTimeMillis() - inicioPdf) + " ms, "
                + pdf.getResponse().getContentLength() + " bytes");
        assertThat(pdf.getResponse().getContentAsByteArray().length).isGreaterThan(100_000);
    }

    // ---- ayudas ----

    private DatosPruebaListaPriorizada nuevoCargador(int cantidad) {
        DatosPruebaListaPriorizada cargador = new DatosPruebaListaPriorizada(jdbc,
                new TransactionTemplate(transacciones));
        ReflectionTestUtils.setField(cargador, "cantidad", cantidad);
        return cargador;
    }

    private JsonNode listar(String parametros) throws Exception {
        String cuerpo = mockMvc.perform(get(LISTA + "?" + parametros).with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(cuerpo);
    }

    private List<String> nombres(JsonNode datos) {
        return datos.findValuesAsText("nombres");
    }

    private List<Long> posiciones(JsonNode datos) {
        return datos.findValues("posicion").stream().map(JsonNode::asLong).toList();
    }

    private long nuevaConvocatoria(long gestion, long creador, String nombre, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, ?, ?, '2026-07-01T00:00:00Z', '2026-08-31T00:00:00Z', CAST(? AS estado_convocatoria))
                RETURNING id""", Long.class, gestion, creador, nombre, estado);
    }

    private long postulacion(long convocatoria, String nombres, String apellidos, String ci, long carrera,
            String estado, String envio, Double puntaje) {
        long usuario = jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, '!', ?, ? FROM rol WHERE nombre = 'POSTULANTE' RETURNING id""", Long.class,
                ci, ci + "@prueba.test", nombres, apellidos);
        jdbc.update("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                VALUES (?, ?, ?, '70000000')""", usuario, carrera, "RU-" + ci);
        long postulacion = jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion), CAST(? AS timestamptz), 1) RETURNING id""",
                Long.class, convocatoria, usuario, estado, envio);
        if (puntaje != null) {
            jdbc.update("""
                    INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                    VALUES (?, ?, ?, now(), true)""", modelo, postulacion, puntaje);
        }
        return postulacion;
    }

    private void crearUsuario(String correo, String ci, String rol, String nombres, String apellidos) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, ?, ? FROM rol WHERE nombre = ?""", ci, correo,
                passwordEncoder.encode("secreta1"), nombres, apellidos, rol);
        if ("POSTULANTE".equals(rol)) {
            jdbc.update("""
                    INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                    SELECT u.id, (SELECT min(id) FROM carrera), 'RU-' || u.ci, '70000000'
                    FROM usuario u WHERE u.ci = ?""", ci);
        }
    }

    private JsonNode login(String correo, String contrasena) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(cuerpo).get("data");
    }

    private RequestPostProcessor bearer(JsonNode sesion) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.get("accessToken").asText());
            return request;
        };
    }

    private String estados() {
        return jdbc.queryForList("SELECT estado::text || ':' || id FROM postulacion ORDER BY id", String.class)
                .toString();
    }

    private long contarBitacora() {
        return jdbc.queryForObject("SELECT count(*) FROM bitacora WHERE accion = 'EXPORTACION_LISTA_PRIORIZADA'",
                Long.class);
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues no deben ver estos datos.
    private void borrarTodo() {
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM notificacion");
        jdbc.update("DELETE FROM finalista");
        jdbc.update("DELETE FROM comunicado");
        jdbc.update("DELETE FROM puntaje_postulacion");
        jdbc.update("DELETE FROM postulacion_historial");
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM modelo_preseleccion");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
