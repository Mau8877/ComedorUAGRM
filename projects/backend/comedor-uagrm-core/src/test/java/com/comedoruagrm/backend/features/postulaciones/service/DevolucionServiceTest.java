package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.postulaciones.dto.DevolucionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DevolverPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.DevolucionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.DevolucionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DevolucionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

class DevolucionServiceTest {

    private final DevolucionConsultaRepository consulta = mock(DevolucionConsultaRepository.class);
    private final PostulacionRepository postulaciones = mock(PostulacionRepository.class);
    private final PostulacionHistorialRepository historial = mock(PostulacionHistorialRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final DevolucionService service = new DevolucionService(consulta, postulaciones, historial, usuarios,
            notificaciones);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID finalistaCodigo = UUID.randomUUID();
    private final UUID postulacionCodigo = UUID.randomUUID();
    private final UUID postulanteCodigo = UUID.randomUUID();
    private final Usuario actor = usuario(7L, "Admin");
    private final Usuario postulante = usuario(8L, "Ana");
    private final Cabecera cabecera = new Cabecera(1L, finalistaCodigo, 3L, postulacionCodigo, "calificada",
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

    private void camino_feliz() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(usuarios.obtenerPorCodigo(postulanteCodigo)).thenReturn(postulante);
        when(consulta.cabecera(finalistaCodigo)).thenReturn(Optional.of(cabecera));
        when(consulta.estadoActual(3L)).thenReturn("calificada");
        when(consulta.hayAuditoriaPosteriorALaUltimaDevolucion(1L, 3L)).thenReturn(true);
        when(postulaciones.findById(3L)).thenReturn(Optional.of(postulacion));
        when(historial.saveAndFlush(any(PostulacionHistorial.class))).thenAnswer(i -> {
            PostulacionHistorial h = i.getArgument(0);
            ReflectionTestUtils.setField(h, "id", 50L);
            ReflectionTestUtils.setField(h, "codigo", UUID.randomUUID());
            return h;
        });
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(true);
    }

    @Test
    void devolver_calificadaConAuditoria_cambiaElEstadoGuardaHistorialYAvisa() {
        camino_feliz();

        ResultadoAuditado<DevolucionResponse> r = service.devolver(actorCodigo, finalistaCodigo,
                new DevolverPostulacionRequest("  Falta el carnet  "));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.devuelta);
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoAnterior()).isEqualTo(EstadoPostulacion.calificada);
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.devuelta);
        assertThat(fila.getValue().getMotivo()).isEqualTo("Falta el carnet");
        assertThat(fila.getValue().getUsuarioId()).isEqualTo(7L);
        assertThat(r.datos().estado()).isEqualTo("devuelta");
        assertThat(r.datos().postulacion()).isEqualTo(postulacionCodigo);
        assertThat(r.datos().motivo()).isEqualTo("Falta el carnet");
        assertThat(r.datos().devueltaPor()).isEqualTo("Admin Perez");
        verify(consulta).bloquearPostulacion(3L);
    }

    @Test
    void devolver_avisaConElEventoYElMotivoComoObservaciones() {
        camino_feliz();

        service.devolver(actorCodigo, finalistaCodigo, new DevolverPostulacionRequest("Falta el carnet"));

        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).notificar(eq(postulante), eq(EventoNotificacion.POSTULACION_DEVUELTA),
                datos.capture(), eq(3L), eq(null));
        assertThat(datos.getValue()).containsEntry("observaciones", "Falta el carnet").containsEntry("nombres", "Ana");
    }

    @Test
    void devolver_laBitacoraNoLlevaElMotivoNiLosNombres() {
        camino_feliz();

        ResultadoAuditado<DevolucionResponse> r = service.devolver(actorCodigo, finalistaCodigo,
                new DevolverPostulacionRequest("Falta el carnet de Ana"));

        assertThat(r.detalle().toString()).doesNotContain("carnet").doesNotContain("Ana").doesNotContain("Perez");
        assertThat(r.detalle()).containsEntry("estadoAnterior", "calificada").containsEntry("estadoNuevo", "devuelta");
        assertThat(r.entidadId()).isEqualTo(50L);
    }

    @Test
    void devolver_conFinalistaInexistente_lanzaNotFoundErrFin01() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.cabecera(finalistaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.devolver(actorCodigo, finalistaCodigo, new DevolverPostulacionRequest("x")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(FinalistaErrorCodes.ERR_FIN_01);
        verify(historial, never()).saveAndFlush(any());
    }

    @Test
    void devolver_sinEstarCalificada_lanzaConflictErrDev01SinEscribirNada() {
        camino_feliz();
        when(consulta.estadoActual(3L)).thenReturn("devuelta");

        assertThatThrownBy(() -> service.devolver(actorCodigo, finalistaCodigo, new DevolverPostulacionRequest("x")))
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(DevolucionErrorCodes.ERR_DEV_01);
        verify(historial, never()).saveAndFlush(any());
        verify(notificaciones, never()).notificar(any(), any(), any(), anyLong(), any());
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.calificada);
    }

    @Test
    void devolver_sinAuditoriaPosterior_lanzaConflictErrDev02SinEscribirNada() {
        camino_feliz();
        when(consulta.hayAuditoriaPosteriorALaUltimaDevolucion(1L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> service.devolver(actorCodigo, finalistaCodigo, new DevolverPostulacionRequest("x")))
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(DevolucionErrorCodes.ERR_DEV_02);
        verify(historial, never()).saveAndFlush(any());
        verify(notificaciones, never()).notificar(any(), any(), any(), anyLong(), any());
    }

    @Test
    void devolver_siElAvisoFalla_laDevolucionIgualSeCompleta() {
        camino_feliz();
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenThrow(new RuntimeException("smtp"));

        ResultadoAuditado<DevolucionResponse> r = service.devolver(actorCodigo, finalistaCodigo,
                new DevolverPostulacionRequest("x"));

        assertThat(r.datos().estado()).isEqualTo("devuelta");
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.devuelta);
    }

    @Test
    void devolver_siElAvisoNoSeRegistra_laDevolucionIgualSeCompleta() {
        camino_feliz();
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(false);

        assertThat(service.devolver(actorCodigo, finalistaCodigo, new DevolverPostulacionRequest("x"))
                .datos().estado()).isEqualTo("devuelta");
    }
}
