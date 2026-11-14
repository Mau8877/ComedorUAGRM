package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
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
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.CierrePostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.CierrePostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.CierrePostulacionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

class CierrePostulacionServiceTest {

    private final CierrePostulacionConsultaRepository consulta = mock(CierrePostulacionConsultaRepository.class);
    private final PostulacionRepository postulaciones = mock(PostulacionRepository.class);
    private final PostulacionHistorialRepository historial = mock(PostulacionHistorialRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final CierrePostulacionService service = new CierrePostulacionService(consulta, postulaciones,
            historial, usuarios, notificaciones);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID finalistaCodigo = UUID.randomUUID();
    private final UUID postulacionCodigo = UUID.randomUUID();
    private final UUID postulanteCodigo = UUID.randomUUID();
    private final Usuario actor = usuario(7L, "Admin");
    private final Usuario postulante = usuario(8L, "Ana");
    private final Cabecera cabecera = new Cabecera(3L, postulacionCodigo, 1L, finalistaCodigo, 9L, "calificada",
            postulanteCodigo, "Ana Perez", "Conv");
    private final Postulacion postulacion = nuevaPostulacion();

    private static Usuario usuario(long id, String nombres) {
        Usuario u = new Usuario(null, "1234" + id, nombres + "@uagrm.edu.bo", "hash", nombres, "Perez");
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

    private void caminoFeliz() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(usuarios.obtenerPorCodigo(postulanteCodigo)).thenReturn(postulante);
        when(consulta.cabecera(finalistaCodigo)).thenReturn(Optional.of(cabecera));
        when(consulta.estadoActual(3L)).thenReturn("calificada");
        when(consulta.resultadosPublicados(9L)).thenReturn(false);
        when(postulaciones.findById(3L)).thenReturn(Optional.of(postulacion));
        when(historial.saveAndFlush(any(PostulacionHistorial.class))).thenAnswer(i -> {
            PostulacionHistorial h = i.getArgument(0);
            ReflectionTestUtils.setField(h, "id", 50L);
            ReflectionTestUtils.setField(h, "codigo", UUID.randomUUID());
            return h;
        });
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(true);
    }

    private static CierrePostulacionRequest motivo(String texto) {
        return new CierrePostulacionRequest(texto);
    }

    private void sinCambios() {
        verify(historial, never()).saveAndFlush(any());
        verify(notificaciones, never()).notificar(any(), any(), any(), anyLong(), any());
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.calificada);
    }

    // ---- archivar ----

    @Test
    void archivar_calificada_cambiaElEstadoGuardaHistorialYResponde() {
        caminoFeliz();

        ResultadoAuditado<CierrePostulacionResponse> r = service.archivar(actorCodigo, finalistaCodigo,
                motivo("  No entregó sus documentos  "));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.archivada);
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoAnterior()).isEqualTo(EstadoPostulacion.calificada);
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.archivada);
        assertThat(fila.getValue().getMotivo()).isEqualTo("No entregó sus documentos");
        assertThat(fila.getValue().getUsuarioId()).isEqualTo(7L);
        assertThat(r.datos().estado()).isEqualTo("archivada");
        assertThat(r.datos().postulacion()).isEqualTo(postulacionCodigo);
        assertThat(r.datos().motivo()).isEqualTo("No entregó sus documentos");
        assertThat(r.datos().decididaPor()).isEqualTo("Admin Perez");
        verify(consulta).bloquearPostulacion(3L);
    }

    @Test
    void archivar_avisaConElEventoDeArchivoYElMotivo() {
        caminoFeliz();

        service.archivar(actorCodigo, finalistaCodigo, motivo("No entregó sus documentos"));

        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).notificar(eq(postulante), eq(EventoNotificacion.POSTULACION_ARCHIVADA),
                datos.capture(), eq(3L), eq(null));
        assertThat(datos.getValue()).containsEntry("motivo", "No entregó sus documentos")
                .containsEntry("nombres", "Ana");
    }

    @Test
    void archivar_laBitacoraNoLlevaElMotivoNiLosNombres() {
        caminoFeliz();

        ResultadoAuditado<CierrePostulacionResponse> r = service.archivar(actorCodigo, finalistaCodigo,
                motivo("Documentos de Ana no válidos"));

        assertThat(r.detalle().toString()).doesNotContain("Documentos").doesNotContain("Ana")
                .doesNotContain("Perez");
        assertThat(r.detalle()).containsEntry("estadoAnterior", "calificada").containsEntry("estadoNuevo", "archivada")
                .containsEntry("postulacion", postulacionCodigo.toString())
                .containsEntry("finalista", finalistaCodigo.toString());
        assertThat(r.entidadId()).isEqualTo(50L);
    }

    // ---- rechazar ----

    @Test
    void rechazar_calificada_dejaNoAdjudicadaConSuHistorial() {
        caminoFeliz();

        ResultadoAuditado<CierrePostulacionResponse> r = service.rechazar(actorCodigo, finalistaCodigo,
                motivo("Documentos falsos"));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoAnterior()).isEqualTo(EstadoPostulacion.calificada);
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.no_adjudicada);
        assertThat(fila.getValue().getMotivo()).isEqualTo("Documentos falsos");
        assertThat(r.datos().estado()).isEqualTo("no_adjudicada");
        assertThat(r.detalle()).containsEntry("estadoNuevo", "no_adjudicada");
    }

    @Test
    void rechazar_avisaConElEventoDeRechazoYElMotivo() {
        caminoFeliz();

        service.rechazar(actorCodigo, finalistaCodigo, motivo("Documentos falsos"));

        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).notificar(eq(postulante), eq(EventoNotificacion.POSTULACION_RECHAZADA),
                datos.capture(), eq(3L), eq(null));
        assertThat(datos.getValue()).containsEntry("motivo", "Documentos falsos").containsEntry("nombres", "Ana");
    }

    // ---- validaciones ----

    @Test
    void conFinalistaInexistente_lanzaNotFoundErrFin01SinEscribirNada() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.cabecera(finalistaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archivar(actorCodigo, finalistaCodigo, motivo("x")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(FinalistaErrorCodes.ERR_FIN_01);
        assertThatThrownBy(() -> service.rechazar(actorCodigo, finalistaCodigo, motivo("x")))
                .isInstanceOf(NotFoundException.class);
        verify(historial, never()).saveAndFlush(any());
        verify(consulta, never()).bloquearPostulacion(anyLong());
    }

    @Test
    void sinEstarCalificada_lanzaConflictErrArc01ConElEstadoActual_enAmbasAcciones() {
        for (String estado : new String[] {"pendiente", "devuelta", "archivada", "no_adjudicada", "adjudicada"}) {
            caminoFeliz();
            when(consulta.estadoActual(3L)).thenReturn(estado);

            assertThatThrownBy(() -> service.archivar(actorCodigo, finalistaCodigo, motivo("x")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(estado)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CierrePostulacionErrorCodes.ERR_ARC_01);
            assertThatThrownBy(() -> service.rechazar(actorCodigo, finalistaCodigo, motivo("x")))
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CierrePostulacionErrorCodes.ERR_ARC_01);
            sinCambios();
        }
    }

    @Test
    void conLosResultadosPublicados_lanzaConflictErrArc01SinEscribirNada() {
        caminoFeliz();
        when(consulta.resultadosPublicados(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.archivar(actorCodigo, finalistaCodigo, motivo("x")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("publicados")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CierrePostulacionErrorCodes.ERR_ARC_01);
        assertThatThrownBy(() -> service.rechazar(actorCodigo, finalistaCodigo, motivo("x")))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CierrePostulacionErrorCodes.ERR_ARC_01);
        sinCambios();
    }

    @Test
    void dosDecisionesSeguidas_laSegundaRecibeErrArc01YSoloHayUnaFila() {
        caminoFeliz();
        service.archivar(actorCodigo, finalistaCodigo, motivo("x"));
        // La segunda peticion ya ve el estado cambiado tras el bloqueo.
        when(consulta.estadoActual(3L)).thenReturn("archivada");

        assertThatThrownBy(() -> service.rechazar(actorCodigo, finalistaCodigo, motivo("y")))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CierrePostulacionErrorCodes.ERR_ARC_01);

        verify(historial, times(1)).saveAndFlush(any());
        verify(notificaciones, times(1)).notificar(any(), any(), any(), anyLong(), any());
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.archivada);
    }

    @Test
    void noExigeAuditoriaNiEntregaFisica() {
        caminoFeliz();

        assertThat(service.archivar(actorCodigo, finalistaCodigo, motivo("x")).datos().estado())
                .isEqualTo("archivada");
    }

    // ---- aviso ----

    @Test
    void siElAvisoFalla_laDecisionIgualSeCompleta() {
        caminoFeliz();
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenThrow(new RuntimeException("smtp"));

        ResultadoAuditado<CierrePostulacionResponse> r = service.rechazar(actorCodigo, finalistaCodigo, motivo("x"));

        assertThat(r.datos().estado()).isEqualTo("no_adjudicada");
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
    }

    @Test
    void siElAvisoNoSeRegistra_laDecisionIgualSeCompleta() {
        caminoFeliz();
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(false);

        assertThat(service.archivar(actorCodigo, finalistaCodigo, motivo("x")).datos().estado())
                .isEqualTo("archivada");
    }
}
