package com.comedoruagrm.backend.features.finalistas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.config.DatosPruebaFinalistas;
import com.comedoruagrm.backend.config.DatosPruebaListaPriorizada;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CorreoNotificacionService;
import com.comedoruagrm.backend.features.notificaciones.service.MensajePush;
import com.comedoruagrm.backend.features.notificaciones.service.ResultadoPush;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU21 de punta a punta contra un Postgres real: listado de finalistas con estados, notificar (con un
 * finalista sin telefono y el reenvio a los fallidos), registrar y corregir la entrega, comunicados, la
 * bandeja del usuario, la bitacora sin datos personales, los permisos y que ninguna postulacion cambie de
 * estado. El push y el correo estan simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinalistasComunicadosIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final String FINALISTAS = "/api/v1/finalistas";
    private static final String COMUNICADOS = "/api/v1/comunicados";
    private static final String BANDEJA = "/api/v1/notificaciones";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transacciones;

    @MockitoBean
    private CanalPush canalPush;

    @MockitoBean
    private CorreoNotificacionService correoService;

    private UUID convocatoria;
    private UUID finalistaAna;
    private UUID finalistaBeto;
    private UUID finalistaCarla;
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
        when(correoService.enviar(any(), any(), any())).thenReturn(true);
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenAnswer(i -> new ResultadoPush(((List<?>) i.getArgument(0)).size(), List.of(), List.of()));

        crearUsuario("admin@dubss.test", "0000001", "ADMINISTRADOR_DUBSS", "Admin", "DUBSS");
        long adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE ci = '0000001'", Long.class);
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES ('2026-II', DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class);
        modelo = jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (creado_por, version, activo) VALUES (?, 'v1', false)
                RETURNING id""", Long.class, adminId);
        long conv = jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, ?, 'Beca Comedor 2026-II', '2026-07-01T00:00:00Z', '2026-08-31T00:00:00Z',
                        CAST('CERRADA' AS estado_convocatoria)) RETURNING id""", Long.class, gestion, adminId);
        convocatoria = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?",
                UUID.class, conv);

        // Ana y Beto tienen telefono registrado; Carla NO (sin push, solo correo y bandeja).
        long ana = postulante(conv, "Ana", "Rojas", "1000001", "ana@uagrm.test", 95.0, "calificada");
        long beto = postulante(conv, "Beto", "Mamani", "1000002", "beto@uagrm.test", 90.0, "calificada");
        long carla = postulante(conv, "Carla", "Peña", "1000003", "carla@uagrm.test", 85.0, "calificada");
        postulante(conv, "Diego", "Suárez", "1000004", "diego@uagrm.test", 80.0, "calificada");
        postulante(conv, "Elena", "Flores", "1000005", "elena@uagrm.test", null, "pendiente");
        telefono("ana@uagrm.test", "token-ana");
        telefono("beto@uagrm.test", "token-beto");

        finalistaAna = finalista(ana, 1);
        finalistaBeto = finalista(beto, 2);
        finalistaCarla = finalista(carla, 3);
        this.admin = bearer(login("admin@dubss.test", "secreta1"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- listado ----

    @Test
    void listado_muestraPosicionNombrePuntajeYLosEstadosIniciales() throws Exception {
        JsonNode datos = listar("filter[convocatoriaId]=" + convocatoria).get("data");

        assertThat(datos.findValuesAsText("nombres")).containsExactly("Ana", "Beto", "Carla");
        assertThat(datos.findValuesAsText("estadoNotificacion")).containsOnly("SIN_ENVIAR");
        assertThat(datos.get(0).get("puntaje").decimalValue()).isEqualByComparingTo("95.00");
        assertThat(datos.get(0).get("documentacionRecibidaEn").isNull()).isTrue();
        assertThat(datos.get(0).has("id")).isFalse();
    }

    @Test
    void listado_buscaPorNombreOCiYPagina() throws Exception {
        assertThat(listar("filter[convocatoriaId]=" + convocatoria + "&search=BETO").get("data")
                .findValuesAsText("nombres")).containsExactly("Beto");
        assertThat(listar("filter[convocatoriaId]=" + convocatoria + "&search=1000003").get("data")
                .findValuesAsText("nombres")).containsExactly("Carla");
        JsonNode pagina = listar("filter[convocatoriaId]=" + convocatoria + "&page=2&pageSize=2");
        assertThat(pagina.get("data").findValuesAsText("nombres")).containsExactly("Carla");
        assertThat(pagina.get("meta").get("totalItems").asLong()).isEqualTo(3);
    }

    @Test
    void listado_deUnaConvocatoriaSinFinalistasEsVacio_yUnaInexistenteDa404() throws Exception {
        jdbc.update("DELETE FROM finalista");
        assertThat(listar("filter[convocatoriaId]=" + convocatoria).get("data")).isEmpty();

        mockMvc.perform(get(FINALISTAS + "?filter[convocatoriaId]=" + UUID.randomUUID()).with(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_02"));
    }

    // ---- notificar (flujo A) ----

    @Test
    void notificar_dejaElAvisoEnLaBandejaYManda_pushYCorreo_yUnFinalistaSinTelefonoNoDaError() throws Exception {
        String estadosAntes = estados();

        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoNotificar(List.of(finalistaAna, finalistaCarla), plazo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.solicitados").value(2))
                .andExpect(jsonPath("$.data.enviados").value(2))
                .andExpect(jsonPath("$.data.fallidos").value(0));

        // El correo sale para los dos; el push solo para Ana (Carla no tiene telefono).
        verify(correoService, timeout(5000)).enviar(eq("ana@uagrm.test"), any(String.class), any(String.class));
        verify(correoService, timeout(5000)).enviar(eq("carla@uagrm.test"), any(String.class), any(String.class));
        verify(canalPush, timeout(5000)).enviar(eq(List.of("token-ana")), any(MensajePush.class));
        assertThat(contar("notificacion", "postulacion_id IS NOT NULL")).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT mensaje FROM notificacion n JOIN usuario u ON u.id = n.usuario_id
                WHERE u.correo = 'carla@uagrm.test'""", String.class))
                .contains("Carla", "DUBSS oficina 3, 8:00 a 12:00", "Carnet de identidad");
        assertThat(contar("finalista", "notificado_en IS NOT NULL AND plazo_entrega = DATE '" + plazo() + "'"))
                .isEqualTo(2);
        assertThat(esperarHasta(() -> contar("notificacion", "enviada_en IS NOT NULL") == 2)).isTrue();

        JsonNode datos = listar("filter[convocatoriaId]=" + convocatoria).get("data");
        assertThat(datos.findValuesAsText("estadoNotificacion")).containsExactly("ENVIADA", "SIN_ENVIAR", "ENVIADA");
        // CA15 y CA16: bitacora sin datos personales, ningun estado de postulacion cambia.
        String bitacora = jdbc.queryForObject(
                "SELECT detalle::text FROM bitacora WHERE accion = 'NOTIFICACION_FINALISTAS'", String.class);
        assertThat(bitacora).contains(finalistaAna.toString(), "Carnet de identidad")
                .doesNotContain("Ana").doesNotContain("Carla").doesNotContain("1000001").doesNotContain("@uagrm");
        assertThat(estados()).isEqualTo(estadosAntes);
    }

    @Test
    void notificar_conDatosInvalidos_rechazaSinEnviarNada() throws Exception {
        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"finalistas\":[],\"lugarHorario\":\"\",\"documentos\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoNotificar(List.of(finalistaAna), LocalDate.now(ZONA))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_FIN_04"));
        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoNotificar(List.of(finalistaAna, UUID.randomUUID()), plazo())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));

        assertThat(contar("notificacion", "true")).isZero();
        assertThat(contar("finalista", "notificado_en IS NOT NULL")).isZero();
    }

    @Test
    void notificar_siElEnvioFalla_elFinalistaQuedaFallidaYSePuedeReenviarSoloAEse() throws Exception {
        when(correoService.enviar(any(), any(), any())).thenReturn(false);
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(0, List.of(), List.of()));

        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoNotificar(List.of(finalistaCarla), plazo()))).andExpect(status().isOk());

        // El aviso queda guardado en la bandeja aunque nada se haya podido entregar (CA17).
        assertThat(contar("notificacion", "enviada_en IS NULL")).isEqualTo(1);
        JsonNode datos = esperarEstado(finalistaCarla, "FALLIDA");
        assertThat(datos).isNotNull();

        // Reenvio solo a ese finalista, ahora con el correo funcionando.
        when(correoService.enviar(any(), any(), any())).thenReturn(true);
        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoNotificar(List.of(finalistaCarla), plazo()))).andExpect(status().isOk());

        assertThat(esperarEstado(finalistaCarla, "ENVIADA")).isNotNull();
        assertThat(contar("notificacion", "true")).isEqualTo(2);
    }

    // ---- entrega fisica (flujo B) ----

    @Test
    void entrega_sinFechaUsaHoy_laFuturaSeRechaza_yCorregirDejaLaAnteriorEnLaBitacora() throws Exception {
        String url = FINALISTAS + "/" + finalistaAna + "/entrega";

        mockMvc.perform(patch(url).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observaciones\":\"Documentos completos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentacionRecibidaEn").value(LocalDate.now(ZONA).toString()));

        mockMvc.perform(patch(url).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaRecepcion\":\"%s\"}".formatted(LocalDate.now(ZONA).plusDays(1))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_FIN_03"));

        LocalDate corregida = LocalDate.now(ZONA).minusDays(2);
        mockMvc.perform(patch(url).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaRecepcion\":\"%s\"}".formatted(corregida)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentacionRecibidaEn").value(corregida.toString()));

        List<String> detalles = jdbc.queryForList("""
                SELECT detalle::text FROM bitacora WHERE accion = 'ENTREGA_DOCUMENTACION_REGISTRADA'
                ORDER BY id""", String.class);
        assertThat(detalles).hasSize(2);
        assertThat(detalles.get(0)).contains("Documentos completos").doesNotContain("fechaAnterior");
        assertThat(detalles.get(1)).contains("fechaAnterior").contains(LocalDate.now(ZONA).toString());
        assertThat(detalles.toString()).doesNotContain("Ana").doesNotContain("1000001");

        mockMvc.perform(patch(FINALISTAS + "/" + UUID.randomUUID() + "/entrega").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_01"));
    }

    // ---- comunicados (flujo C) ----

    @Test
    void comunicado_aTodos_dejaUnAvisoEnLaBandejaDeCadaPostulanteYNoMandaCorreo() throws Exception {
        String estadosAntes = estados();

        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"Nuevo horario\",\"contenido\":\"El comedor abre a las 11\",\"destinatarios\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalDestinatarios").value(5))
                .andExpect(jsonPath("$.data.titulo").value("Nuevo horario"));

        assertThat(contar("notificacion", "comunicado_id IS NOT NULL")).isEqualTo(5);
        verify(canalPush, timeout(5000).atLeastOnce()).enviar(anyList(), any(MensajePush.class));
        Thread.sleep(300);
        verify(correoService, never()).enviar(any(), any(), any());
        String bitacora = jdbc.queryForObject(
                "SELECT detalle::text FROM bitacora WHERE accion = 'COMUNICADO_PUBLICADO'", String.class);
        assertThat(bitacora).contains("TODOS").doesNotContain("El comedor abre a las 11");
        assertThat(estados()).isEqualTo(estadosAntes);

        JsonNode lista = JSON.readTree(mockMvc.perform(get(COMUNICADOS).with(admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(lista.get("data").get(0).get("totalDestinatarios").asLong()).isEqualTo(5);
    }

    @Test
    void comunicado_aFinalistas_llegaSoloAEllos_yConConvocatoriaTodosSonLosQueEnviaron() throws Exception {
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"Auditoria\",\"contenido\":\"Traer originales\",\"destinatarios\":\"FINALISTAS\","
                        + "\"convocatoriaId\":\"%s\"}".formatted(convocatoria)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.totalDestinatarios").value(3));
        assertThat(jdbc.queryForList("""
                SELECT u.nombres FROM notificacion n JOIN usuario u ON u.id = n.usuario_id
                WHERE n.comunicado_id IS NOT NULL ORDER BY u.nombres""", String.class))
                .containsExactly("Ana", "Beto", "Carla");

        // Con convocatoria, TODOS son quienes tienen una postulacion enviada en ella (5 de 5, ninguna en borrador).
        jdbc.update("UPDATE postulacion SET estado = CAST('borrador' AS estado_postulacion) WHERE id = "
                + "(SELECT min(id) FROM postulacion)");
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"General\",\"contenido\":\"Aviso\",\"destinatarios\":\"TODOS\","
                        + "\"convocatoriaId\":\"%s\"}".formatted(convocatoria)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.totalDestinatarios").value(4));
    }

    @Test
    void comunicado_conDatosInvalidos_noPublicaNada() throws Exception {
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"\",\"contenido\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"ADJUDICADOS\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_COM_01"));
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"FINALISTAS\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_COM_02"));
        jdbc.update("DELETE FROM finalista");
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"FINALISTAS\",\"convocatoriaId\":\"%s\"}"
                        .formatted(convocatoria)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_COM_01"));
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"TODOS\",\"convocatoriaId\":\"%s\"}"
                        .formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_02"));

        assertThat(contar("comunicado", "true")).isZero();
        assertThat(contar("notificacion", "true")).isZero();
    }

    // ---- bandeja del usuario (flujo D) ----

    @Test
    void bandeja_cadaUsuarioVeSoloSusAvisos_losMarcaComoLeidosYLosElimina() throws Exception {
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"Aviso uno\",\"contenido\":\"a\",\"destinatarios\":\"TODOS\"}")).andExpect(status().isCreated());
        mockMvc.perform(post(COMUNICADOS).with(admin).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"Aviso dos\",\"contenido\":\"b\",\"destinatarios\":\"TODOS\"}")).andExpect(status().isCreated());
        RequestPostProcessor ana = bearer(login("ana@uagrm.test", "secreta1"));
        RequestPostProcessor beto = bearer(login("beto@uagrm.test", "secreta1"));

        JsonNode avisosAna = JSON.readTree(mockMvc.perform(get(BANDEJA + "/me?pageSize=10").with(ana))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(avisosAna.get("data").findValuesAsText("titulo")).containsExactly("Aviso dos", "Aviso uno");
        assertThat(avisosAna.get("meta").get("totalItems").asLong()).isEqualTo(2);
        mockMvc.perform(get(BANDEJA + "/me/resumen").with(ana)).andExpect(jsonPath("$.data.noLeidas").value(2));

        // Marcar leido uno propio; uno ajeno es 404 sin revelar que existe.
        String propio = avisosAna.get("data").get(0).get("id").asText();
        mockMvc.perform(post(BANDEJA + "/" + propio + "/leer").with(ana)).andExpect(status().isOk());
        mockMvc.perform(post(BANDEJA + "/" + propio + "/leer").with(ana)).andExpect(status().isOk());
        mockMvc.perform(post(BANDEJA + "/" + propio + "/leer").with(beto))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_NOT_01"));
        mockMvc.perform(get(BANDEJA + "/me/resumen").with(ana)).andExpect(jsonPath("$.data.noLeidas").value(1));
        mockMvc.perform(get(BANDEJA + "/me?sort=titulo").with(ana))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_NOT_02"));

        // Leer todo y eliminar solo tocan a Ana.
        mockMvc.perform(post(BANDEJA + "/leer-todo").with(ana)).andExpect(status().isOk());
        mockMvc.perform(get(BANDEJA + "/me/resumen").with(ana)).andExpect(jsonPath("$.data.noLeidas").value(0));
        mockMvc.perform(get(BANDEJA + "/me/resumen").with(beto)).andExpect(jsonPath("$.data.noLeidas").value(2));
        mockMvc.perform(delete(BANDEJA).with(ana)).andExpect(status().isOk());
        mockMvc.perform(get(BANDEJA + "/me").with(ana)).andExpect(jsonPath("$.meta.totalItems").value(0));
        mockMvc.perform(get(BANDEJA + "/me").with(beto)).andExpect(jsonPath("$.meta.totalItems").value(2));
    }

    // ---- permisos ----

    @Test
    void sinSesionDa401_unPostulanteDa403_yLaBandejaExigeSesion() throws Exception {
        mockMvc.perform(get(FINALISTAS + "?filter[convocatoriaId]=" + convocatoria))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
        mockMvc.perform(get(BANDEJA + "/me")).andExpect(status().isUnauthorized());

        RequestPostProcessor ana = bearer(login("ana@uagrm.test", "secreta1"));
        mockMvc.perform(get(FINALISTAS + "?filter[convocatoriaId]=" + convocatoria).with(ana))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        mockMvc.perform(post(FINALISTAS + "/notificaciones").with(ana).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoNotificar(List.of(finalistaAna), plazo()))).andExpect(status().isForbidden());
        mockMvc.perform(patch(FINALISTAS + "/" + finalistaAna + "/entrega").with(ana)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(post(COMUNICADOS).with(ana).contentType(MediaType.APPLICATION_JSON).content(
                "{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"TODOS\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get(COMUNICADOS).with(ana)).andExpect(status().isForbidden());
        assertThat(contar("notificacion", "true")).isZero();
    }

    // ---- cargadores de datos de prueba (dev) ----

    @Test
    void cargadores_creanDiezFinalistasYLaCuentaDePrueba_sinDuplicarEnLaSegundaCorrida() {
        borrarTodo();
        crearUsuario("admin@dubss.com", "0000001", "ADMINISTRADOR_DUBSS", "Administrador", "DUBSS");
        TransactionTemplate tx = new TransactionTemplate(transacciones);
        DatosPruebaListaPriorizada cargador = new DatosPruebaListaPriorizada(jdbc, tx);
        ReflectionTestUtils.setField(cargador, "cantidad", 40);
        DatosPruebaFinalistas finalistas = new DatosPruebaFinalistas(jdbc, tx, passwordEncoder);
        ReflectionTestUtils.setField(finalistas, "contrasena", "clave-de-prueba");

        cargador.run();
        assertThat(contar("finalista", "true")).isEqualTo(10);
        assertThat(contar("finalista", "notificado_en IS NOT NULL")).isEqualTo(3);
        assertThat(contar("finalista", "documentacion_recibida_en IS NOT NULL")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT posicion FROM finalista ORDER BY posicion", Integer.class))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        finalistas.run();
        assertThat(contar("usuario", "correo = 'postulante.finalista@prueba.local'")).isEqualTo(1);
        assertThat(contar("notificacion", "postulacion_id IS NOT NULL")).isEqualTo(3);
        assertThat(contar("notificacion", "postulacion_id IS NOT NULL AND enviada_en IS NULL")).isEqualTo(1);
        assertThat(contar("comunicado", "titulo LIKE '[PRUEBA]%'")).isEqualTo(1);
        assertThat(contar("notificacion", "comunicado_id IS NOT NULL")).isEqualTo(15);
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE correo = 'postulante.finalista@prueba.local'", String.class);
        assertThat(passwordEncoder.matches("clave-de-prueba", hash)).isTrue();

        finalistas.run();
        assertThat(contar("comunicado", "true")).isEqualTo(1);
        assertThat(contar("notificacion", "true")).isEqualTo(18);
    }

    // ---- ayudas ----

    private LocalDate plazo() {
        return LocalDate.now(ZONA).plusDays(7);
    }

    private String pedidoNotificar(List<UUID> finalistas, LocalDate plazo) {
        String codigos = String.join(",", finalistas.stream().map(c -> "\"" + c + "\"").toList());
        return """
                {"finalistas":[%s],"plazoEntrega":"%s","lugarHorario":"DUBSS oficina 3, 8:00 a 12:00",
                 "documentos":"Carnet de identidad y certificado"}""".formatted(codigos, plazo);
    }

    private JsonNode listar(String parametros) throws Exception {
        String cuerpo = mockMvc.perform(get(FINALISTAS + "?" + parametros).with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(cuerpo);
    }

    // El despacho del aviso es asincrono: el estado se consulta hasta que llega al esperado.
    private JsonNode esperarEstado(UUID finalista, String esperado) throws Exception {
        for (int i = 0; i < 50; i++) {
            for (JsonNode fila : listar("filter[convocatoriaId]=" + convocatoria).get("data")) {
                if (fila.get("codigo").asText().equals(finalista.toString())
                        && fila.get("estadoNotificacion").asText().equals(esperado)) {
                    return fila;
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("El finalista no llego al estado " + esperado);
    }

    private boolean esperarHasta(BooleanSupplier condicion) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (condicion.getAsBoolean()) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private long postulante(long convocatoria, String nombres, String apellidos, String ci, String correo,
            Double puntaje, String estado) {
        long usuario = jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, ?, ? FROM rol WHERE nombre = 'POSTULANTE' RETURNING id""", Long.class,
                ci, correo, passwordEncoder.encode("secreta1"), nombres, apellidos);
        // Telefono de la ficha del postulante (no es un dispositivo de push).
        jdbc.update("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                VALUES (?, (SELECT min(id) FROM carrera), ?, '70000000')""", usuario, "RU-" + ci);
        long postulacion = jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion), '2026-08-01T10:00:00Z', 1) RETURNING id""",
                Long.class, convocatoria, usuario, estado);
        if (puntaje != null) {
            jdbc.update("""
                    INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                    VALUES (?, ?, ?, now(), true)""", modelo, postulacion, puntaje);
        }
        return postulacion;
    }

    private void telefono(String correo, String token) {
        jdbc.update("""
                INSERT INTO dispositivo_fcm (usuario_id, token, plataforma)
                SELECT id, ?, 'ANDROID' FROM usuario WHERE correo = ?""", token, correo);
    }

    private UUID finalista(long postulacion, int posicion) {
        return jdbc.queryForObject("INSERT INTO finalista (postulacion_id, posicion) VALUES (?, ?) "
                + "RETURNING codigo_finalista", UUID.class, postulacion, posicion);
    }

    private void crearUsuario(String correo, String ci, String rol, String nombres, String apellidos) {
        jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, ?, ? FROM rol WHERE nombre = ?""", ci, correo,
                passwordEncoder.encode("secreta1"), nombres, apellidos, rol);
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

    private long contar(String tabla, String condicion) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + condicion, Long.class);
    }

    // Deja la base vacia al terminar: los tests de repositorio que corren despues no deben ver estos datos.
    private void borrarTodo() {
        jdbc.update("DELETE FROM bitacora");
        jdbc.update("DELETE FROM sesion");
        jdbc.update("DELETE FROM notificacion");
        jdbc.update("DELETE FROM dispositivo_fcm");
        jdbc.update("DELETE FROM finalista");
        jdbc.update("DELETE FROM comunicado");
        jdbc.update("DELETE FROM puntaje_postulacion");
        jdbc.update("DELETE FROM postulacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM modelo_preseleccion");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM postulante");
        jdbc.update("DELETE FROM usuario");
    }
}
