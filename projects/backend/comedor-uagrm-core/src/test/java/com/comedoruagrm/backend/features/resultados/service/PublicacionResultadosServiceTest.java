package com.comedoruagrm.backend.features.resultados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService.AvisoParaUsuario;
import com.comedoruagrm.backend.features.resultados.dto.PublicacionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.model.PublicacionResultado;
import com.comedoruagrm.backend.features.resultados.repository.PublicacionResultadoRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Conteos;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosPublicacion;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Destinatario;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class PublicacionResultadosServiceTest {

    private static final long CONVOCATORIA_ID = 7L;

    @Mock
    private ResultadosConsultaRepository consulta;

    @Mock
    private PublicacionResultadoRepository publicacionRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private NotificacionLoteService notificacionLoteService;

    private PublicacionResultadosService service;
    private Usuario admin;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new PublicacionResultadosService(consulta, publicacionRepository, usuarioService,
                notificacionLoteService);
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
    }

    private void convocatoria(String estado) {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        when(consulta.convocatoria(convocatoria)).thenReturn(
                Optional.of(new DatosConvocatoria(CONVOCATORIA_ID, convocatoria, "Beca 2026-II", estado)));
    }

    private void guardaLaPublicacion() {
        when(publicacionRepository.saveAndFlush(any(PublicacionResultado.class))).thenAnswer(i -> {
            PublicacionResultado p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 50L);
            ReflectionTestUtils.setField(p, "codigo", UUID.randomUUID());
            return p;
        });
    }

    private void guardaLosAvisos() {
        when(notificacionLoteService.notificarLote(any())).thenAnswer(i -> ((List<?>) i.getArgument(0)).size());
    }

    private static Destinatario destinatario(long usuarioId, String nombres, String resultado) {
        return new Destinatario(usuarioId, nombres, nombres.toLowerCase() + "@prueba.test", 100 + usuarioId,
                UUID.randomUUID(), resultado);
    }

    /** Deja el escenario feliz listo: cerrada, sin publicar, nada pendiente y estos destinatarios. */
    private void escenarioFeliz(Conteos conteos, List<Destinatario> destinatarios) {
        convocatoria("CERRADA");
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());
        when(consulta.contarPorEstado(CONVOCATORIA_ID)).thenReturn(conteos);
        when(consulta.destinatariosDelAviso(CONVOCATORIA_ID)).thenReturn(destinatarios);
        guardaLaPublicacion();
    }

    // ---- camino feliz ----

    @Test
    void publicar_conTodoDecidido_guardaLaPublicacion_cambiaElEstado_yAvisaACadaFinalista() {
        escenarioFeliz(new Conteos(1, 1, 0, 4), List.of(destinatario(11, "Ana", "ADJUDICADA"),
                destinatario(12, "Beto", "NO_ADJUDICADA")));
        guardaLosAvisos();

        ResultadoAuditado<PublicacionResultadoResponse> resultado = service.publicar(actorCodigo, convocatoria);

        ArgumentCaptor<PublicacionResultado> guardada = ArgumentCaptor.forClass(PublicacionResultado.class);
        verify(publicacionRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getConvocatoriaId()).isEqualTo(CONVOCATORIA_ID);
        assertThat(guardada.getValue().getPublicadoPor()).isEqualTo(1L);
        assertThat(guardada.getValue().getPublicadoEn()).isNotNull();
        verify(consulta).bloquearConvocatoria(CONVOCATORIA_ID);
        verify(consulta).marcarConvocatoriaPublicada(CONVOCATORIA_ID);

        PublicacionResultadoResponse respuesta = resultado.datos();
        assertThat(respuesta.convocatoria().codigo()).isEqualTo(convocatoria);
        assertThat(respuesta.convocatoria().estado()).isEqualTo("PUBLICADA");
        assertThat(respuesta.publicadoPor()).isEqualTo("Admin DUBSS");
        assertThat(respuesta.adjudicados()).isEqualTo(1);
        assertThat(respuesta.noAdjudicados()).isEqualTo(1);
        assertThat(respuesta.avisados()).isEqualTo(2);
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(50L);
        assertThat(resultado.auditar()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicar_avisaConElEventoDeCadaResultado_yConSusDatos_enUnaSolaLlamadaPorLotes() {
        escenarioFeliz(new Conteos(1, 1, 0, 2), List.of(destinatario(11, "Ana", "ADJUDICADA"),
                destinatario(12, "Beto", "NO_ADJUDICADA")));
        guardaLosAvisos();

        service.publicar(actorCodigo, convocatoria);

        ArgumentCaptor<List<AvisoParaUsuario>> avisos = ArgumentCaptor.forClass(List.class);
        verify(notificacionLoteService, times(1)).notificarLote(avisos.capture());
        assertThat(avisos.getValue()).hasSize(2);
        AvisoParaUsuario ana = avisos.getValue().get(0);
        assertThat(ana.evento()).isEqualTo(EventoNotificacion.BECA_ADJUDICADA);
        assertThat(ana.usuarioId()).isEqualTo(11L);
        assertThat(ana.correo()).isEqualTo("ana@prueba.test");
        assertThat(ana.datos()).isEqualTo(Map.of("nombres", "Ana", "convocatoria", "Beca 2026-II"));
        assertThat(ana.postulacionId()).isEqualTo(111L);
        AvisoParaUsuario beto = avisos.getValue().get(1);
        assertThat(beto.evento()).isEqualTo(EventoNotificacion.BECA_NO_ADJUDICADA);
        assertThat(beto.datos()).containsEntry("nombres", "Beto");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicar_sinNingunAdjudicado_funciona_yCadaFinalistaRecibeNoAdjudicada() {
        escenarioFeliz(new Conteos(0, 3, 0, 3), List.of(destinatario(11, "Ana", "NO_ADJUDICADA"),
                destinatario(12, "Beto", "NO_ADJUDICADA"), destinatario(13, "Carla", "NO_ADJUDICADA")));
        guardaLosAvisos();

        PublicacionResultadoResponse respuesta = service.publicar(actorCodigo, convocatoria).datos();

        ArgumentCaptor<List<AvisoParaUsuario>> avisos = ArgumentCaptor.forClass(List.class);
        verify(notificacionLoteService).notificarLote(avisos.capture());
        assertThat(avisos.getValue()).extracting(AvisoParaUsuario::evento)
                .containsOnly(EventoNotificacion.BECA_NO_ADJUDICADA).hasSize(3);
        assertThat(respuesta.adjudicados()).isZero();
        assertThat(respuesta.avisados()).isEqualTo(3);
    }

    @Test
    void publicar_sinFinalistasConDecision_publicaIgualYNoAvisaANadie() {
        escenarioFeliz(new Conteos(0, 0, 0, 0), List.of());
        guardaLosAvisos();

        PublicacionResultadoResponse respuesta = service.publicar(actorCodigo, convocatoria).datos();

        verify(consulta).marcarConvocatoriaPublicada(CONVOCATORIA_ID);
        assertThat(respuesta.avisados()).isZero();
    }

    // ---- errores de negocio ----

    @Test
    void publicar_conFinalistasEnCalificada_lanzaErrPub01ConLaCantidad_yNoGuardaNada() {
        convocatoria("CERRADA");
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());
        when(consulta.contarPorEstado(CONVOCATORIA_ID)).thenReturn(new Conteos(2, 1, 3, 6));

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_01");
                    assertThat(e.getMessage()).contains("Faltan 3 decisiones");
                });

        sinEfectos();
    }

    @Test
    void publicar_siYaEstabaPublicada_lanzaErrPub02_yNoGuardaNiAvisa() {
        convocatoria("PUBLICADA");
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.of(
                new DatosPublicacion(java.time.Instant.now(), "Admin DUBSS")));

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_02"));

        sinEfectos();
    }

    @Test
    void publicar_dosVeces_laPrimeraPublica_laSegundaRecibeErrPub02_yHayUnSoloEnvio() {
        escenarioFeliz(new Conteos(1, 0, 0, 1), List.of(destinatario(11, "Ana", "ADJUDICADA")));
        guardaLosAvisos();

        service.publicar(actorCodigo, convocatoria);
        // La segunda peticion, ya con el bloqueo tomado, ve la publicacion de la primera.
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.of(
                new DatosPublicacion(java.time.Instant.now(), "Admin DUBSS")));

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_02"));

        verify(publicacionRepository, times(1)).saveAndFlush(any(PublicacionResultado.class));
        verify(notificacionLoteService, times(1)).notificarLote(any());
        verify(consulta, times(1)).marcarConvocatoriaPublicada(CONVOCATORIA_ID);
    }

    @Test
    void publicar_siLaUnicidadDeLaBaseGanaLaCarrera_loConvierteEnErrPub02() {
        convocatoria("CERRADA");
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());
        when(consulta.contarPorEstado(CONVOCATORIA_ID)).thenReturn(new Conteos(1, 0, 0, 1));
        when(publicacionRepository.saveAndFlush(any(PublicacionResultado.class)))
                .thenThrow(new DataIntegrityViolationException("uq_publicacion_resultado_convocatoria"));

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_02"));

        verify(consulta, never()).marcarConvocatoriaPublicada(anyLong());
        verify(notificacionLoteService, never()).notificarLote(any());
    }

    @Test
    void publicar_convocatoriaNoCerrada_lanzaErrPub03() {
        convocatoria("ABIERTA");
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_03"));

        sinEfectos();
    }

    @Test
    void publicar_convocatoriaInexistente_lanzaNotFoundConErrPri01() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));

        verify(consulta, never()).bloquearConvocatoria(anyLong());
    }

    private void sinEfectos() {
        verify(publicacionRepository, never()).saveAndFlush(any(PublicacionResultado.class));
        verify(consulta, never()).marcarConvocatoriaPublicada(anyLong());
        verify(notificacionLoteService, never()).notificarLote(any());
    }

    // ---- atomicidad ----

    @Test
    void publicar_siFallaGuardarLosAvisos_laExcepcionSubeParaQueLaTransaccionSeDeshaga() {
        escenarioFeliz(new Conteos(1, 0, 0, 1), List.of(destinatario(11, "Ana", "ADJUDICADA")));
        org.mockito.Mockito.doThrow(new IllegalStateException("fallo al guardar")).when(notificacionLoteService)
                .notificarLote(any());

        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publicar_esTransaccional_conRollbackPorCualquierRuntimeException() throws NoSuchMethodException {
        Transactional tx = PublicacionResultadosService.class.getMethod("publicar", UUID.class, UUID.class)
                .getAnnotation(Transactional.class);

        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
        assertThat(tx.rollbackFor()).isEmpty();
        assertThat(tx.noRollbackFor()).isEmpty();
    }

    // ---- sin N+1, bitacora y permisos ----

    @Test
    void publicar_conCincoOQuinientosFinalistas_hacelasMismasLlamadasAlRepository() {
        verificarLlamadasConFinalistas(5);
        org.mockito.Mockito.clearInvocations(consulta, publicacionRepository, notificacionLoteService);
        verificarLlamadasConFinalistas(500);
    }

    private void verificarLlamadasConFinalistas(int cantidad) {
        List<Destinatario> destinatarios = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            destinatarios.add(destinatario(1000L + i, "Nombre" + i, i % 2 == 0 ? "ADJUDICADA" : "NO_ADJUDICADA"));
        }
        org.mockito.Mockito.reset(consulta, publicacionRepository, usuarioService, notificacionLoteService);
        escenarioFeliz(new Conteos(cantidad / 2, cantidad - cantidad / 2, 0, cantidad), destinatarios);
        guardaLosAvisos();

        service.publicar(actorCodigo, convocatoria);

        verify(consulta, times(1)).destinatariosDelAviso(CONVOCATORIA_ID);
        verify(consulta, times(1)).contarPorEstado(CONVOCATORIA_ID);
        verify(consulta, times(1)).marcarConvocatoriaPublicada(CONVOCATORIA_ID);
        verify(publicacionRepository, times(1)).saveAndFlush(any(PublicacionResultado.class));
        verify(notificacionLoteService, times(1)).notificarLote(any());
    }

    @Test
    void publicar_laBitacoraLlevaCodigosYCantidades_peroNingunDatoPersonal() {
        escenarioFeliz(new Conteos(1, 1, 0, 2), List.of(destinatario(11, "Ana", "ADJUDICADA"),
                destinatario(12, "Beto", "NO_ADJUDICADA")));
        guardaLosAvisos();

        Map<String, Object> detalle = service.publicar(actorCodigo, convocatoria).detalle();

        assertThat(detalle.keySet()).containsExactly("publicacion", "convocatoria", "adjudicados", "noAdjudicados",
                "avisados");
        assertThat(detalle).containsEntry("convocatoria", convocatoria.toString())
                .containsEntry("adjudicados", 1L).containsEntry("noAdjudicados", 1L).containsEntry("avisados", 2);
        assertThat(detalle.toString()).doesNotContain("Ana", "Beto", "@", "admin", "DUBSS");
    }

    @Test
    void publicar_noTocaPostulacionesNiActaNiPadron() {
        Constructor<?>[] constructores = PublicacionResultadosService.class.getDeclaredConstructors();

        assertThat(constructores).hasSize(1);
        assertThat(constructores[0].getParameterTypes()).extracting(Class::getSimpleName)
                .containsExactly("ResultadosConsultaRepository", "PublicacionResultadoRepository", "UsuarioService",
                        "NotificacionLoteService");
    }

    @Test
    void publicar_exigeElRolAdministrador_yQuedaAuditadoConLaAccionDeLaPublicacion() throws NoSuchMethodException {
        var metodo = PublicacionResultadosService.class.getMethod("publicar", UUID.class, UUID.class);

        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        assertThat(metodo.getAnnotation(Auditar.class).accion()).isEqualTo(BitacoraAcciones.PUBLICACION_RESULTADOS);
        assertThat(BusinessException.class).isAssignableFrom(ConflictException.class);
    }
}
