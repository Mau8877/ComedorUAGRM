package com.comedoruagrm.backend.features.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CorreoNotificacionService;
import com.comedoruagrm.backend.features.notificaciones.service.MensajePush;
import com.comedoruagrm.backend.features.notificaciones.service.ResultadoPush;
import com.comedoruagrm.backend.support.ContadorDeSentenciasDataSource;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CU04 de punta a punta contra un Postgres real: el listado ordenado y filtrado por gestion, el detalle con su
 * linea de tiempo sin autor, las observaciones, el motivo, el bloque de finalista, el estado visible antes y
 * despues de publicar, el aviso de la bandeja que apunta a la postulacion, los rechazos (404/401/403) y que
 * consultar no modifique nada ni haga N+1.
 */
// Muchas peticiones desde una misma IP: se sube el limite para que el rate limit no interfiera con el test.
@SpringBootTest(properties = {"app.ratelimit.capacity=100000", "app.ratelimit.refill-per-minute=100000"})
@AutoConfigureMockMvc
class MisPostulacionesIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TITULO_FINALISTA = "Entrega de documentación física - Beca Comedor";

    /** Cuenta las sentencias SQL del hilo de la peticion para probar que el listado no hace N+1. */
    @TestConfiguration
    static class ContadorConfig {

        @Bean
        static BeanPostProcessor contadorDeSentencias() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) throws BeansException {
                    if (bean instanceof DataSource ds && !(bean instanceof ContadorDeSentenciasDataSource)) {
                        return new ContadorDeSentenciasDataSource(ds);
                    }
                    return bean;
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CanalPush canalPush;

    @MockitoBean
    private CorreoNotificacionService correoService;

    private ExpedienteFixtures fx;
    private RequestPostProcessor yo;
    private RequestPostProcessor otra;
    private RequestPostProcessor admin;
    private long yoId;
    private long otraId;
    private long adminId;

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
        reset(canalPush, correoService);
        when(correoService.enviar(any(), any(), any())).thenReturn(true);
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenAnswer(i -> new ResultadoPush(((List<?>) i.getArgument(0)).size(), List.of(), List.of()));

        fx = new ExpedienteFixtures(jdbc);
        adminId = fx.usuario("0000001", "ADMINISTRADOR_DUBSS");
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE id = ?", passwordEncoder.encode("secreta1"), adminId);
        yoId = fx.postulante("2200001");
        otraId = fx.postulante("2200002");
        conClave("2200001");
        conClave("2200002");
        yo = bearer(login("2200001@prueba.test"));
        otra = bearer(login("2200002@prueba.test"));
        admin = bearer(login("0000001@prueba.test"));
    }

    @AfterEach
    void limpiar() {
        borrarTodo();
    }

    // ---- listado (CA01, CA02, CA03, CA12) ----

    @Test
    void mias_listaLasEnviadasDeLaMasRecienteALaMasAntigua_sinBorradoresNiAjenas() throws Exception {
        long antigua = convocatoria("MIA");
        long reciente = convocatoria("MIB");
        postulacion(yoId, antigua, "pendiente", "2026-11-01T10:00:00Z");
        postulacion(yoId, reciente, "calificada", "2026-11-05T10:00:00Z");
        postulacion(yoId, convocatoria("MIC"), "borrador", null);
        postulacion(otraId, reciente, "pendiente", "2026-11-06T10:00:00Z");

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].convocatoria.nombre").value("Conv MIB"))
                .andExpect(jsonPath("$.data[0].estado").value("calificada"))
                .andExpect(jsonPath("$.data[0].gestionAcademica.nombre").value("G-MIB"))
                .andExpect(jsonPath("$.data[0].numeroEnvios").value(1))
                .andExpect(jsonPath("$.data[1].convocatoria.nombre").value("Conv MIA"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(20));
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo).param("sort", "fechaEnvio"))
                .andExpect(jsonPath("$.data[0].convocatoria.nombre").value("Conv MIA"));
    }

    @Test
    void mias_sinPostulaciones_devuelveUnaListaVacia() throws Exception {
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void mias_filtraPorGestionAcademica_yRechazaLoNoSoportado() throws Exception {
        long a = convocatoria("MIFA");
        long b = convocatoria("MIFB");
        postulacion(yoId, a, "pendiente", "2026-11-01T10:00:00Z");
        postulacion(yoId, b, "pendiente", "2026-11-02T10:00:00Z");
        UUID gestionA = jdbc.queryForObject("SELECT g.codigo_gestion_academica FROM convocatoria cv JOIN "
                + "gestion_academica g ON g.id = cv.gestion_id WHERE cv.id = ?", UUID.class, a);

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo).param("filter[gestionAcademica]", gestionA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].convocatoria.nombre").value("Conv MIFA"));
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo).param("filter[estado]", "pendiente"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo).param("filter[gestionAcademica]", "no-uuid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo).param("sort", "estado"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void mias_elEstadoVisibleEsCalificadaHastaPublicar_yElRechazoDelCu20SeVeDeInmediato() throws Exception {
        long sinPublicar = convocatoria("MIV1");
        long publicada = convocatoria("MIV2");
        long rechazo = convocatoria("MIV3");
        long p1 = postulacion(yoId, sinPublicar, "adjudicada", "2026-11-01T10:00:00Z");
        long p2 = postulacion(yoId, publicada, "no_adjudicada", "2026-11-02T10:00:00Z");
        postulacion(yoId, rechazo, "no_adjudicada", "2026-11-03T10:00:00Z");
        fx.adjudicacion(p1, "ADJUDICADA", "80.00", adminId);
        fx.adjudicacion(p2, "NO_ADJUDICADA", "40.00", adminId);
        publicar(publicada);

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].convocatoria.nombre").value("Conv MIV3"))
                .andExpect(jsonPath("$.data[0].estado").value("no_adjudicada"))
                .andExpect(jsonPath("$.data[1].convocatoria.nombre").value("Conv MIV2"))
                .andExpect(jsonPath("$.data[1].estado").value("no_adjudicada"))
                .andExpect(jsonPath("$.data[2].convocatoria.nombre").value("Conv MIV1"))
                .andExpect(jsonPath("$.data[2].estado").value("calificada"));
    }

    @Test
    void mias_haceElMismoNumeroDeConsultasConPocasOMuchasPostulaciones() throws Exception {
        for (int i = 0; i < 3; i++) {
            long c = convocatoria("MIN" + i);
            postulacion(yoId, c, "adjudicada", "2026-11-0" + (i + 1) + "T10:00:00Z");
            publicar(c);
        }
        int conTres = sentenciasDelListado();
        for (int i = 3; i < 12; i++) {
            long c = convocatoria("MIN" + i);
            long p = postulacion(yoId, c, "adjudicada", "2026-11-" + (i + 10) + "T10:00:00Z");
            fx.adjudicacion(p, "ADJUDICADA", "80.00", adminId);
            publicar(c);
        }

        assertThat(sentenciasDelListado()).isEqualTo(conTres);
    }

    private int sentenciasDelListado() throws Exception {
        ContadorDeSentenciasDataSource.reiniciar();
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo)).andExpect(status().isOk());
        return ContadorDeSentenciasDataSource.sentencias();
    }

    // ---- detalle (CA04, CA05, CA06, CA09) ----

    @Test
    void detalle_muestraElHistorialConFechaYHora_sinElAutorDeLosCambios() throws Exception {
        long c = convocatoria("MID1");
        long p = postulacion(yoId, c, "calificada", "2026-11-01T10:00:00Z");
        historial(p, "borrador", "pendiente", null, "2026-11-01T10:00:00Z");
        historial(p, "pendiente", "calificada", null, "2026-11-02T11:30:00Z");

        MvcResult resultado = mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(yo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value(codigo(p).toString()))
                .andExpect(jsonPath("$.data.estado").value("calificada"))
                .andExpect(jsonPath("$.data.convocatoria.nombre").value("Conv MID1"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[0].estado").value("pendiente"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[0].situacion").value("COMPLETADO"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[1].estado").value("calificada"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[1].fecha").value("2026-11-02T11:30:00Z"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[2].estado").value("resultado"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[2].situacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.puedeCorregir").value(false))
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("usuario", "autor", "0000001", "@prueba.test");
    }

    @Test
    void detalle_deUnaDevuelta_muestraLasObservacionesYPuedeCorregir() throws Exception {
        long c = convocatoria("MID2");
        long p = postulacion(yoId, c, "devuelta", "2026-11-01T10:00:00Z");
        historial(p, "pendiente", "calificada", null, "2026-11-02T10:00:00Z");
        historial(p, "calificada", "devuelta", "Falta el certificado de notas", "2026-11-03T10:00:00Z");

        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(yo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("devuelta"))
                .andExpect(jsonPath("$.data.observaciones.texto").value("Falta el certificado de notas"))
                .andExpect(jsonPath("$.data.observaciones.fecha").value("2026-11-03T10:00:00Z"))
                .andExpect(jsonPath("$.data.puedeCorregir").value(true));
    }

    @Test
    void detalle_deUnaArchivadaYDeUnRechazo_muestraElMotivoRegistrado() throws Exception {
        long archivada = postulacion(yoId, convocatoria("MID3"), "archivada", "2026-11-01T10:00:00Z");
        historial(archivada, "calificada", "archivada", "No entregó su documentación", "2026-11-03T10:00:00Z");
        long rechazada = postulacion(yoId, convocatoria("MID4"), "no_adjudicada", "2026-11-01T10:00:00Z");
        historial(rechazada, "calificada", "no_adjudicada", "Documentos que no coinciden", "2026-11-03T10:00:00Z");

        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(archivada)).with(yo))
                .andExpect(jsonPath("$.data.estado").value("archivada"))
                .andExpect(jsonPath("$.data.motivo").value("No entregó su documentación"));
        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(rechazada)).with(yo))
                .andExpect(jsonPath("$.data.estado").value("no_adjudicada"))
                .andExpect(jsonPath("$.data.motivo").value("Documentos que no coinciden"))
                .andExpect(jsonPath("$.data.resultado").doesNotExist());
    }

    @Test
    void detalle_deUnFinalista_muestraElPlazoLosDocumentosYSiLaDocumentacionFueRecibida() throws Exception {
        long c = convocatoria("MID5");
        long p = postulacion(yoId, c, "calificada", "2026-11-01T10:00:00Z");
        long finalista = fx.finalista(p, 1);
        jdbc.update("UPDATE finalista SET plazo_entrega = DATE '2026-11-20' WHERE id = ?", finalista);
        aviso(yoId, p, TITULO_FINALISTA, "Hola. Debes entregar tu documentación física hasta el 20/11/2026. "
                + "Lugar y horario: DUBSS. Documentos a presentar: carnet de identidad y certificado de notas.");

        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(yo))
                .andExpect(jsonPath("$.data.finalista.plazoEntrega").value("2026-11-20"))
                .andExpect(jsonPath("$.data.finalista.documentacionRecibida").value(false))
                .andExpect(jsonPath("$.data.finalista.documentosFisicos")
                        .value("carnet de identidad y certificado de notas"));

        fx.entregaRecibida(finalista);
        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(yo))
                .andExpect(jsonPath("$.data.finalista.documentacionRecibida").value(true));
    }

    // ---- estado visible y resultado (CA07, CA08) ----

    @Test
    void detalle_deUnaDecisionFinalSinPublicar_seVeCalificada_conLaFilaOculta_ySinResultado() throws Exception {
        long c = convocatoria("MID6");
        long p = postulacion(yoId, c, "adjudicada", "2026-11-01T10:00:00Z");
        historial(p, "pendiente", "calificada", null, "2026-11-02T10:00:00Z");
        historial(p, "calificada", "adjudicada", null, "2026-11-03T10:00:00Z");
        fx.adjudicacion(p, "ADJUDICADA", "85.00", adminId);

        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(yo))
                .andExpect(jsonPath("$.data.estado").value("calificada"))
                .andExpect(jsonPath("$.data.lineaDeTiempo.length()").value(2))
                .andExpect(jsonPath("$.data.lineaDeTiempo[0].estado").value("calificada"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[0].situacion").value("COMPLETADO"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[1].estado").value("resultado"))
                .andExpect(jsonPath("$.data.lineaDeTiempo[1].situacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.resultado").doesNotExist());
    }

    @Test
    void detalle_conResultadosPublicados_muestraElResultadoYElCodigoDeLaConvocatoria_sinElMotivoDeLaDecision()
            throws Exception {
        long adj = convocatoria("MID7");
        long no = convocatoria("MID8");
        long p1 = postulacion(yoId, adj, "adjudicada", "2026-11-01T10:00:00Z");
        long p2 = postulacion(yoId, no, "no_adjudicada", "2026-11-01T10:00:00Z");
        historial(p1, "calificada", "adjudicada", null, "2026-11-03T10:00:00Z");
        historial(p2, "calificada", "no_adjudicada", "Motivo reservado a la DUBSS", "2026-11-03T10:00:00Z");
        fx.adjudicacion(p1, "ADJUDICADA", "85.00", adminId);
        fx.adjudicacion(p2, "NO_ADJUDICADA", "40.00", adminId);
        publicar(adj);
        publicar(no);

        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p1)).with(yo))
                .andExpect(jsonPath("$.data.estado").value("adjudicada"))
                .andExpect(jsonPath("$.data.resultado").value("ADJUDICADA"))
                .andExpect(jsonPath("$.data.convocatoria.codigo").value(codigoConvocatoria(adj).toString()));
        MvcResult noAdjudicada = mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p2)).with(yo))
                .andExpect(jsonPath("$.data.estado").value("no_adjudicada"))
                .andExpect(jsonPath("$.data.resultado").value("NO_ADJUDICADA"))
                .andExpect(jsonPath("$.data.motivo").doesNotExist())
                .andReturn();
        assertThat(noAdjudicada.getResponse().getContentAsString()).doesNotContain("Motivo reservado");
    }

    // ---- el aviso abre el detalle (CA10) ----

    @Test
    void bandeja_elAvisoDeUnaPostulacionTraeSuCodigo_yUnoSinPostulacionTraeNulo() throws Exception {
        long p = postulacion(yoId, convocatoria("MIA10"), "devuelta", "2026-11-01T10:00:00Z");
        aviso(yoId, p, "Tu postulación fue devuelta - Beca Comedor", "Observaciones: falta algo");
        jdbc.update("INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo) VALUES (?, 'Comunicado', "
                + "'General', CAST('INFO' AS tipo_notificacion))", yoId);

        mockMvc.perform(get("/api/v1/notificaciones/me?page=1&pageSize=10&sort=fecha").with(yo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].postulacionCodigo").value(codigo(p).toString()))
                .andExpect(jsonPath("$.data[1].postulacionCodigo").doesNotExist());
    }

    // ---- 404, 401 y 403 (CA11) ----

    @Test
    void detalle_deUnaAjenaUnaInexistenteOUnBorrador_dan404ErrPos01ConElMismoMensaje() throws Exception {
        long ajena = postulacion(otraId, convocatoria("MIE1"), "pendiente", "2026-11-01T10:00:00Z");
        long borrador = postulacion(yoId, convocatoria("MIE2"), "borrador", null);

        String delAjeno = cuerpo404("/api/v1/postulaciones/" + codigo(ajena));
        String delBorrador = cuerpo404("/api/v1/postulaciones/" + codigo(borrador));
        String delInexistente = cuerpo404("/api/v1/postulaciones/" + UUID.randomUUID());

        for (String cuerpo : List.of(delAjeno, delBorrador, delInexistente)) {
            assertThat(JSON.readTree(cuerpo).get("error").asText()).isEqualTo("ERR_POS_01");
        }
        assertThat(JSON.readTree(delAjeno).get("message")).isEqualTo(JSON.readTree(delInexistente).get("message"));
        assertThat(JSON.readTree(delBorrador).get("message")).isEqualTo(JSON.readTree(delInexistente).get("message"));
    }

    private String cuerpo404(String ruta) throws Exception {
        return mockMvc.perform(get(ruta).with(yo)).andExpect(status().isNotFound()).andReturn().getResponse()
                .getContentAsString();
    }

    @Test
    void sinSesionDa401_yUnNoPostulanteDa403ErrAuth07() throws Exception {
        long p = postulacion(yoId, convocatoria("MIS"), "pendiente", "2026-11-01T10:00:00Z");

        mockMvc.perform(get("/api/v1/postulaciones/mias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p))).andExpect(status().isUnauthorized());
        for (String ruta : List.of("/api/v1/postulaciones/mias", "/api/v1/postulaciones/" + codigo(p))) {
            ResultActions r = mockMvc.perform(get(ruta).with(admin));
            r.andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
        }
    }

    // ---- sin efectos secundarios (CA13) ----

    @Test
    void consultar_noModificaPostulacionesHistorialAvisosNiBitacora() throws Exception {
        long p = postulacion(yoId, convocatoria("MIX"), "devuelta", "2026-11-01T10:00:00Z");
        historial(p, "calificada", "devuelta", "Falta algo", "2026-11-03T10:00:00Z");
        aviso(yoId, p, "Tu postulación fue devuelta - Beca Comedor", "Observaciones: falta algo");
        String antes = instantanea();

        mockMvc.perform(get("/api/v1/postulaciones/mias").with(yo)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(yo)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/postulaciones/" + codigo(p)).with(otra)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/postulaciones/mias").with(admin)).andExpect(status().isForbidden());

        assertThat(instantanea()).isEqualTo(antes);
        assertThat(contar("notificacion WHERE leida")).isZero();
        assertThat(contar("bitacora WHERE accion NOT IN ('INICIO_SESION')")).isZero();
    }

    private String instantanea() {
        return jdbc.queryForList("SELECT 'p' || id || estado::text || numero_envios || actualizado_en::text "
                + "FROM postulacion UNION ALL SELECT 'h' || id || actualizado_en::text FROM postulacion_historial "
                + "UNION ALL SELECT 'n' || id || leida::text || actualizado_en::text FROM notificacion "
                + "UNION ALL SELECT 'a' || id FROM adjudicacion ORDER BY 1", String.class).toString();
    }

    // ---- ayudas ----

    private long convocatoria(String etiqueta) {
        return fx.convocatoria(etiqueta);
    }

    private long postulacion(long usuario, long convocatoria, String estado, String envio) {
        long id = fx.postulacion(convocatoria, usuario, estado);
        if (envio != null) {
            fx.fechaEnvio(id, envio);
        }
        return id;
    }

    private UUID codigo(long postulacion) {
        return jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class, postulacion);
    }

    private UUID codigoConvocatoria(long id) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class, id);
    }

    private void publicar(long convocatoria) {
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)", convocatoria,
                adminId);
    }

    private void historial(long postulacion, String anterior, String nuevo, String motivo, String creadoEn) {
        jdbc.update("INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo, "
                + "motivo, creado_en) VALUES (?, ?, CAST(? AS estado_postulacion), CAST(? AS estado_postulacion), ?, "
                + "CAST(? AS timestamptz))", postulacion, adminId, anterior, nuevo, motivo, creadoEn);
    }

    private void aviso(long usuario, long postulacion, String titulo, String mensaje) {
        jdbc.update("INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo, postulacion_id) VALUES "
                + "(?, ?, ?, CAST('INFO' AS tipo_notificacion), ?)", usuario, titulo, mensaje, postulacion);
    }

    private long contar(String tablaYCondicion) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tablaYCondicion, Long.class);
    }

    /** Le pone una contrasena conocida a un usuario de prueba para poder iniciar sesion con el. */
    private void conClave(String ci) {
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE ci = ?", passwordEncoder.encode("secreta1"), ci);
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

    // Deja la base vacia al terminar: los demas tests de integracion no deben ver estos datos.
    private void borrarTodo() {
        jdbc.update("DELETE FROM publicacion_resultado");
        jdbc.update("DELETE FROM adjudicacion");
        jdbc.update("DELETE FROM postulacion_historial");
        jdbc.update("DELETE FROM auditoria_expediente");
        jdbc.update("DELETE FROM documento_adjunto");
        jdbc.update("DELETE FROM respuesta_socioeconomica");
        jdbc.update("DELETE FROM convocatoria_documento");
        jdbc.update("DELETE FROM tipo_documento");
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
