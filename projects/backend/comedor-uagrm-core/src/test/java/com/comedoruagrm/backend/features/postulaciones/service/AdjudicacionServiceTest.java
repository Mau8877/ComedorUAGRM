package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.dto.DecisionAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.NoAdjudicarRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.PanelAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.enums.ResultadoAdjudicacion;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Adjudicacion;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CabeceraFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.UltimaAuditoria;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Panel y decision final de la adjudicacion (CU05) con los repositorios simulados. */
class AdjudicacionServiceTest {

    private final AdjudicacionConsultaRepository consulta = mock(AdjudicacionConsultaRepository.class);
    private final AdjudicacionRepository adjudicaciones = mock(AdjudicacionRepository.class);
    private final PostulacionRepository postulaciones = mock(PostulacionRepository.class);
    private final PostulacionHistorialRepository historial = mock(PostulacionHistorialRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final AdjudicacionService service = new AdjudicacionService(consulta, adjudicaciones, postulaciones,
            historial, usuarios);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID finalistaCodigo = UUID.randomUUID();
    private final UUID postulacionCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final Usuario actor = usuario(7L);
    private Postulacion postulacion = nuevaPostulacion();

    private static Usuario usuario(long id) {
        Usuario u = new Usuario(null, "1234" + id, "admin@uagrm.edu.bo", "hash", "Admin", "Perez");
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "codigo", UUID.randomUUID());
        return u;
    }

    private static Postulacion nuevaPostulacion() {
        try {
            var ctor = Postulacion.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Postulacion p = ctor.newInstance();
            ReflectionTestUtils.setField(p, "estado", EstadoPostulacion.calificada);
            return p;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DatosConvocatoria convocatoria(String estado, Integer cupos, String minimo, Integer cantidad) {
        return new DatosConvocatoria(9L, convocatoriaCodigo, "Conv", estado, cupos,
                minimo == null ? null : new BigDecimal(minimo), cantidad);
    }

    private CabeceraFinalista cabecera(String ia) {
        return new CabeceraFinalista(1L, finalistaCodigo, 3L, postulacionCodigo, 9L, convocatoriaCodigo,
                "calificada", ia == null ? null : new BigDecimal(ia));
    }

    /** Camino feliz de adjudicar: cerrada, 5 cupos y minimo 60, IA 45, ultima auditoria validada con 20. */
    private void escenario(DatosConvocatoria conv, String ia, UltimaAuditoria auditoria, long adjudicadas) {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.cabeceraFinalista(finalistaCodigo)).thenReturn(Optional.of(cabecera(ia)));
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(conv));
        when(consulta.estadoActual(3L)).thenReturn("calificada");
        when(consulta.resultadosPublicados(9L)).thenReturn(false);
        when(consulta.ultimaAuditoria(1L)).thenReturn(Optional.ofNullable(auditoria));
        when(consulta.contarAdjudicadas(9L)).thenReturn(adjudicadas);
        when(postulaciones.findById(3L)).thenReturn(Optional.of(postulacion));
        when(adjudicaciones.saveAndFlush(any(Adjudicacion.class))).thenAnswer(i -> {
            Adjudicacion a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 60L);
            ReflectionTestUtils.setField(a, "codigo", UUID.randomUUID());
            return a;
        });
        when(historial.saveAndFlush(any(PostulacionHistorial.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void feliz() {
        escenario(convocatoria("CERRADA", 5, "60", 10), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                2);
    }

    private static NoAdjudicarRequest motivo(String texto) {
        return new NoAdjudicarRequest(texto);
    }

    private void sinCambios() {
        verify(adjudicaciones, never()).saveAndFlush(any());
        verify(historial, never()).saveAndFlush(any());
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.calificada);
    }

    private void falla(Runnable accion, Class<? extends BusinessException> tipo, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(tipo).satisfies(e -> assertThat(((BusinessException) e)
                .getErrorCode()).isEqualTo(codigo));
        sinCambios();
    }

    // ---- panel ----

    @Test
    void panel_conConvocatoriaInexistente_lanzaNotFound() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.panel(convocatoriaCodigo)).isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(PriorizacionErrorCodes.ERR_PRI_01));
    }

    @Test
    void panel_conCuposYMinimoDefinidos_entregaLasCuentasYHabilitaLasDecisiones() {
        when(consulta.convocatoria(convocatoriaCodigo))
                .thenReturn(Optional.of(convocatoria("CERRADA", 10, "55.5", 12)));
        when(consulta.contarAdjudicadas(9L)).thenReturn(4L);
        when(consulta.contarNoAdjudicadas(9L)).thenReturn(2L);
        when(consulta.contarFinalistas(9L)).thenReturn(20L);
        when(consulta.contarCalificadasSinFinalista(9L)).thenReturn(30L);

        PanelAdjudicacionResponse p = service.panel(convocatoriaCodigo);

        assertThat(p.convocatoria().codigo()).isEqualTo(convocatoriaCodigo);
        assertThat(p.convocatoria().estado()).isEqualTo("CERRADA");
        assertThat(p.cupos()).isEqualTo(10);
        assertThat(p.puntajeMinimo()).isEqualByComparingTo("55.5");
        assertThat(p.adjudicados()).isEqualTo(4);
        assertThat(p.noAdjudicados()).isEqualTo(2);
        assertThat(p.cuposDisponibles()).isEqualTo(6);
        assertThat(p.cantidadFinalistas()).isEqualTo(12);
        assertThat(p.totalFinalistas()).isEqualTo(20);
        assertThat(p.calificadasSinSeleccionar()).isEqualTo(30);
        assertThat(p.resultadosPublicados()).isFalse();
        assertThat(p.decisionesHabilitadas()).isTrue();
    }

    @Test
    void panel_sinCupos_noHabilitaLasDecisionesNiInventaCuposDisponibles() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria("CERRADA", null, "50", null)));

        PanelAdjudicacionResponse p = service.panel(convocatoriaCodigo);

        assertThat(p.cupos()).isNull();
        assertThat(p.cuposDisponibles()).isNull();
        assertThat(p.cantidadFinalistas()).isNull();
        assertThat(p.decisionesHabilitadas()).isFalse();
    }

    @Test
    void panel_sinPuntajeMinimo_noHabilitaLasDecisiones() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria("CERRADA", 10, null, null)));

        PanelAdjudicacionResponse p = service.panel(convocatoriaCodigo);

        assertThat(p.puntajeMinimo()).isNull();
        assertThat(p.decisionesHabilitadas()).isFalse();
    }

    @Test
    void panel_conConvocatoriaAbierta_seConsultaPeroNoHabilitaLasDecisiones() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria("ABIERTA", 10, "50", null)));

        PanelAdjudicacionResponse p = service.panel(convocatoriaCodigo);

        assertThat(p.convocatoria().estado()).isEqualTo("ABIERTA");
        assertThat(p.decisionesHabilitadas()).isFalse();
    }

    @Test
    void panel_conCuposAgotados_cuposDisponiblesEsCeroYNuncaNegativo() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria("CERRADA", 3, "50", null)));
        when(consulta.contarAdjudicadas(9L)).thenReturn(3L);

        assertThat(service.panel(convocatoriaCodigo).cuposDisponibles()).isZero();

        when(consulta.contarAdjudicadas(9L)).thenReturn(5L);
        assertThat(service.panel(convocatoriaCodigo).cuposDisponibles()).isZero();
    }

    @Test
    void panel_conResultadosPublicados_noHabilitaLasDecisiones() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria("CERRADA", 10, "50", null)));
        when(consulta.resultadosPublicados(9L)).thenReturn(true);

        PanelAdjudicacionResponse p = service.panel(convocatoriaCodigo);

        assertThat(p.resultadosPublicados()).isTrue();
        assertThat(p.decisionesHabilitadas()).isFalse();
    }

    // ---- adjudicar ----

    @Test
    void adjudicar_feliz_cambiaElEstadoGuardaAdjudicacionHistorialYResponde() {
        feliz();

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.adjudicar(actorCodigo, finalistaCodigo);

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.adjudicada);
        ArgumentCaptor<Adjudicacion> adj = ArgumentCaptor.forClass(Adjudicacion.class);
        verify(adjudicaciones).saveAndFlush(adj.capture());
        assertThat(adj.getValue().getResultado()).isEqualTo(ResultadoAdjudicacion.ADJUDICADA);
        assertThat(adj.getValue().getPuntajeFinal()).isEqualByComparingTo("65");
        assertThat(adj.getValue().getDecididoPor()).isEqualTo(7L);
        assertThat(adj.getValue().getPostulacionId()).isEqualTo(3L);
        assertThat(adj.getValue().getFechaAdjudicacion()).isNotNull();
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoAnterior()).isEqualTo(EstadoPostulacion.calificada);
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.adjudicada);
        assertThat(fila.getValue().getMotivo()).isNull();
        assertThat(fila.getValue().getUsuarioId()).isEqualTo(7L);
        assertThat(r.datos().resultado()).isEqualTo("ADJUDICADA");
        assertThat(r.datos().estado()).isEqualTo("adjudicada");
        assertThat(r.datos().puntajeFinal()).isEqualByComparingTo("65");
        assertThat(r.datos().decididaPor()).isEqualTo("Admin Perez");
        assertThat(r.datos().finalista()).isEqualTo(finalistaCodigo);
        assertThat(r.datos().postulacion()).isEqualTo(postulacionCodigo);
        assertThat(r.actorId()).isEqualTo(7L);
        assertThat(r.entidadId()).isEqualTo(60L);
        assertThat(r.auditar()).isTrue();
    }

    @Test
    void adjudicar_bloqueaPrimeroLaConvocatoriaYDespuesLaPostulacion() {
        feliz();

        service.adjudicar(actorCodigo, finalistaCodigo);

        var orden = org.mockito.Mockito.inOrder(consulta);
        orden.verify(consulta).bloquearConvocatoria(9L);
        orden.verify(consulta).bloquearPostulacion(3L);
    }

    @Test
    void adjudicar_losCuposDisponiblesDeLaRespuestaYaVienenActualizados() {
        feliz();
        // La validacion cuenta 2 adjudicadas (3 disponibles); la consulta final ya cuenta la nueva (2 disponibles).
        when(consulta.contarAdjudicadas(9L)).thenReturn(2L, 3L);

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.adjudicar(actorCodigo, finalistaCodigo);

        assertThat(r.datos().cuposDisponibles()).isEqualTo(2);
    }

    @Test
    void adjudicar_conFinalistaInexistente_lanzaNotFoundYNoBloqueaNada() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.cabeceraFinalista(finalistaCodigo)).thenReturn(Optional.empty());

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), NotFoundException.class,
                FinalistaErrorCodes.ERR_FIN_01);
        verify(consulta, never()).bloquearConvocatoria(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void adjudicar_conConvocatoriaNoCerrada_lanzaErrAdj05() {
        escenario(convocatoria("ABIERTA", 5, "60", null), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_05);
    }

    @Test
    void adjudicar_sinCuposDefinidos_lanzaErrAdj05() {
        escenario(convocatoria("CERRADA", null, "60", null), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_05);
    }

    @Test
    void adjudicar_sinPuntajeMinimoDefinido_lanzaErrAdj05() {
        escenario(convocatoria("CERRADA", 5, null, null), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_05);
    }

    @Test
    void decidir_conPostulacionQueYaNoEstaCalificada_lanzaErrAdj01ConElEstadoActual() {
        for (String estado : new String[] {"devuelta", "archivada", "no_adjudicada", "adjudicada"}) {
            feliz();
            when(consulta.estadoActual(3L)).thenReturn(estado);

            assertThatThrownBy(() -> service.adjudicar(actorCodigo, finalistaCodigo))
                    .isInstanceOf(ConflictException.class).hasMessageContaining(estado)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(AdjudicacionErrorCodes.ERR_ADJ_01));
            assertThatThrownBy(() -> service.noAdjudicar(actorCodigo, finalistaCodigo, motivo("No cumple")))
                    .isInstanceOf(ConflictException.class).satisfies(e -> assertThat(((BusinessException) e)
                            .getErrorCode()).isEqualTo(AdjudicacionErrorCodes.ERR_ADJ_01));
            sinCambios();
        }
    }

    @Test
    void decidir_conResultadosPublicados_lanzaErrAdj01() {
        feliz();
        when(consulta.resultadosPublicados(9L)).thenReturn(true);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_01);
        falla(() -> service.noAdjudicar(actorCodigo, finalistaCodigo, motivo("No cumple")),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_01);
    }

    @Test
    void decidir_conLaConvocatoriaYaPublicada_lanzaErrAdj01EnVezDeErrAdj05() {
        // Al publicar, la convocatoria pasa a PUBLICADA (ya no es «cerrada»): el error correcto sigue siendo ERR_ADJ_01.
        escenario(convocatoria("PUBLICADA", 5, "60", 10), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                2);
        when(consulta.resultadosPublicados(9L)).thenReturn(true);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_01);
        falla(() -> service.noAdjudicar(actorCodigo, finalistaCodigo, motivo("No cumple")),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_01);
        sinCambios();
    }

    @Test
    void panel_conLaConvocatoriaPublicada_noHabilitaLasDecisiones() {
        when(consulta.convocatoria(convocatoriaCodigo))
                .thenReturn(Optional.of(convocatoria("PUBLICADA", 5, "60", 10)));
        when(consulta.resultadosPublicados(9L)).thenReturn(true);

        var p = service.panel(convocatoriaCodigo);

        assertThat(p.resultadosPublicados()).isTrue();
        assertThat(p.decisionesHabilitadas()).isFalse();
    }

    @Test
    void decidir_sinNingunaAuditoria_lanzaErrAdj02EnLasDosDecisiones() {
        escenario(convocatoria("CERRADA", 5, "60", null), "45", null, 0);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_02);
        falla(() -> service.noAdjudicar(actorCodigo, finalistaCodigo, motivo("No cumple")),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_02);
    }

    @Test
    void adjudicar_conUltimaAuditoriaObservadaONoValida_lanzaErrAdj02() {
        for (String resultado : new String[] {"OBSERVADO", "NO_VALIDO"}) {
            escenario(convocatoria("CERRADA", 5, "60", null), "45", new UltimaAuditoria(resultado, null), 0);

            falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                    AdjudicacionErrorCodes.ERR_ADJ_02);
        }
    }

    @Test
    void adjudicar_conPuntajeTotalPorDebajoDelMinimo_lanzaErrAdj03ConLosDosValores() {
        escenario(convocatoria("CERRADA", 5, "60", null), "30", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        assertThatThrownBy(() -> service.adjudicar(actorCodigo, finalistaCodigo))
                .isInstanceOf(ConflictException.class).hasMessageContaining("50").hasMessageContaining("60")
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(AdjudicacionErrorCodes.ERR_ADJ_03));
        sinCambios();
    }

    @Test
    void adjudicar_sinPuntajeDeLaIaVigente_lanzaErrAdj03ConPuntajeNoDisponible() {
        escenario(convocatoria("CERRADA", 5, "60", null), null, new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        assertThatThrownBy(() -> service.adjudicar(actorCodigo, finalistaCodigo))
                .isInstanceOf(ConflictException.class).hasMessageContaining("no disponible")
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(AdjudicacionErrorCodes.ERR_ADJ_03));
    }

    @Test
    void adjudicar_conPuntajeIgualAlMinimo_esPermitido() {
        escenario(convocatoria("CERRADA", 5, "60", null), "40", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.adjudicar(actorCodigo, finalistaCodigo);

        assertThat(r.datos().puntajeFinal()).isEqualByComparingTo("60");
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.adjudicada);
    }

    @Test
    void adjudicar_conCuposAgotados_lanzaErrAdj04() {
        escenario(convocatoria("CERRADA", 3, "60", null), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                3);

        falla(() -> service.adjudicar(actorCodigo, finalistaCodigo), ConflictException.class,
                AdjudicacionErrorCodes.ERR_ADJ_04);
    }

    @Test
    void adjudicar_dosDecisionesSeguidas_laSegundaRecibeErrAdj01() {
        feliz();
        service.adjudicar(actorCodigo, finalistaCodigo);
        // El estado ya cambio: la consulta tras el bloqueo lo ve como adjudicada.
        when(consulta.estadoActual(3L)).thenReturn("adjudicada");

        assertThatThrownBy(() -> service.adjudicar(actorCodigo, finalistaCodigo))
                .isInstanceOf(ConflictException.class).satisfies(e -> assertThat(((BusinessException) e)
                        .getErrorCode()).isEqualTo(AdjudicacionErrorCodes.ERR_ADJ_01));
        verify(adjudicaciones, org.mockito.Mockito.times(1)).saveAndFlush(any());
    }

    @Test
    void adjudicar_dosAdjudicacionesConUnSoloCupo_simuladasEnSerie_laSegundaRecibeErrAdj04() {
        escenario(convocatoria("CERRADA", 1, "60", null), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);
        service.adjudicar(actorCodigo, finalistaCodigo);

        // Otro finalista, otra postulacion, mismo cupo ya usado.
        UUID otroFinalista = UUID.randomUUID();
        Postulacion otra = nuevaPostulacion();
        when(consulta.cabeceraFinalista(otroFinalista)).thenReturn(Optional.of(new CabeceraFinalista(2L,
                otroFinalista, 4L, UUID.randomUUID(), 9L, convocatoriaCodigo, "calificada", new BigDecimal("45"))));
        when(consulta.estadoActual(4L)).thenReturn("calificada");
        when(consulta.ultimaAuditoria(2L)).thenReturn(Optional.of(new UltimaAuditoria("VALIDADO",
                new BigDecimal("20"))));
        when(consulta.contarAdjudicadas(9L)).thenReturn(1L);
        when(postulaciones.findById(4L)).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> service.adjudicar(actorCodigo, otroFinalista))
                .isInstanceOf(ConflictException.class).satisfies(e -> assertThat(((BusinessException) e)
                        .getErrorCode()).isEqualTo(AdjudicacionErrorCodes.ERR_ADJ_04));
        assertThat(otra.getEstado()).isEqualTo(EstadoPostulacion.calificada);
        verify(adjudicaciones, org.mockito.Mockito.times(1)).saveAndFlush(any());
    }

    @Test
    void adjudicar_elPuntajeFinalNoCambiaSiDespuesCambiaElPuntajeDeLaIa() {
        feliz();
        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.adjudicar(actorCodigo, finalistaCodigo);
        ArgumentCaptor<Adjudicacion> adj = ArgumentCaptor.forClass(Adjudicacion.class);
        verify(adjudicaciones).saveAndFlush(adj.capture());

        // La IA se reprocesa: la cabecera ahora trae otro puntaje; lo guardado no se toca.
        when(consulta.cabeceraFinalista(finalistaCodigo)).thenReturn(Optional.of(cabecera("10")));

        assertThat(adj.getValue().getPuntajeFinal()).isEqualByComparingTo("65");
        assertThat(r.datos().puntajeFinal()).isEqualByComparingTo("65");
    }

    // ---- no adjudicar ----

    @Test
    void noAdjudicar_conAuditoriaValidada_guardaLaDecisionConMotivoYSinTocarLosCupos() {
        feliz();

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.noAdjudicar(actorCodigo, finalistaCodigo,
                motivo("  Documento falso  "));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
        ArgumentCaptor<Adjudicacion> adj = ArgumentCaptor.forClass(Adjudicacion.class);
        verify(adjudicaciones).saveAndFlush(adj.capture());
        assertThat(adj.getValue().getResultado()).isEqualTo(ResultadoAdjudicacion.NO_ADJUDICADA);
        assertThat(adj.getValue().getPuntajeFinal()).isEqualByComparingTo("65");
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.no_adjudicada);
        assertThat(fila.getValue().getMotivo()).isEqualTo("Documento falso");
        assertThat(r.datos().resultado()).isEqualTo("NO_ADJUDICADA");
        assertThat(r.datos().estado()).isEqualTo("no_adjudicada");
        assertThat(r.datos().cuposDisponibles()).isEqualTo(3);
    }

    @Test
    void noAdjudicar_conAuditoriaObservadaONoValida_loPermiteYElPuntajeFinalPuedeSerNulo() {
        for (String resultado : new String[] {"OBSERVADO", "NO_VALIDO"}) {
            postulacion = nuevaPostulacion();
            escenario(convocatoria("CERRADA", 5, "60", null), "45", new UltimaAuditoria(resultado, null), 0);

            ResultadoAuditado<DecisionAdjudicacionResponse> r = service.noAdjudicar(actorCodigo, finalistaCodigo,
                    motivo("No procede"));

            assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
            assertThat(r.datos().puntajeFinal()).isNull();
        }
    }

    @Test
    void noAdjudicar_conPuntajeBajoOSinCupos_esPermitido() {
        escenario(convocatoria("CERRADA", 3, "90", null), "20", new UltimaAuditoria("VALIDADO", new BigDecimal("5")),
                3);

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.noAdjudicar(actorCodigo, finalistaCodigo,
                motivo("Puntaje insuficiente"));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
        assertThat(r.datos().puntajeFinal()).isEqualByComparingTo("25");
    }

    @Test
    void noAdjudicar_conConvocatoriaNoCerrada_lanzaErrAdj05() {
        escenario(convocatoria("ABIERTA", 5, "60", null), "45", new UltimaAuditoria("VALIDADO", new BigDecimal("20")),
                0);

        falla(() -> service.noAdjudicar(actorCodigo, finalistaCodigo, motivo("No cumple")),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_05);
    }

    // ---- bitacora, avisos ----

    @Test
    void decidir_laBitacoraLlevaCodigosYEstadosPeroNuncaElMotivoNiLosNombres() {
        feliz();

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.noAdjudicar(actorCodigo, finalistaCodigo,
                motivo("Documentos de Ana Rojas no válidos"));

        assertThat(r.detalle()).containsKeys("adjudicacion", "postulacion", "finalista", "convocatoria")
                .containsEntry("resultado", "NO_ADJUDICADA").containsEntry("estadoAnterior", "calificada")
                .containsEntry("estadoNuevo", "no_adjudicada");
        assertThat(r.detalle().toString()).doesNotContain("Documentos").doesNotContain("Ana").doesNotContain("Rojas")
                .doesNotContain("Perez");
    }

    @Test
    void decidir_noUsaNingunServicioDeNotificacion() {
        // El service ni siquiera recibe un NotificacionService: la decision no avisa al postulante (CU22).
        for (var campo : AdjudicacionService.class.getDeclaredFields()) {
            assertThat(campo.getType().getSimpleName()).doesNotContain("Notificacion");
        }
    }
}
