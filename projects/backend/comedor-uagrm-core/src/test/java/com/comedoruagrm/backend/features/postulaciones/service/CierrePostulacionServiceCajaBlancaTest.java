package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
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

/**
 * Pruebas de caja blanca de CU-20 (capitulo 9, 9.3.2): un test por camino basico de
 * `CierrePostulacionService.cerrar` (C1 a C4), invocado por `archivar`.
 */
class CierrePostulacionServiceCajaBlancaTest {

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

    private void escenario(Instant creadoEn) {
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
            ReflectionTestUtils.setField(h, "creadoEn", creadoEn);
            return h;
        });
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(true);
    }

    private void fallaSinEscribir() {
        assertThatThrownBy(() -> service.archivar(actorCodigo, finalistaCodigo, new CierrePostulacionRequest("x")))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CierrePostulacionErrorCodes.ERR_ARC_01));
        verify(historial, never()).saveAndFlush(any());
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.calificada);
    }

    @Test
    void cerrar_C1_noCalificada_lanzaErrArc01() {
        escenario(null);
        when(consulta.estadoActual(3L)).thenReturn("archivada");

        fallaSinEscribir();
    }

    @Test
    void cerrar_C2_resultadosPublicados_lanzaErrArc01() {
        escenario(null);
        when(consulta.resultadosPublicados(9L)).thenReturn(true);

        fallaSinEscribir();
    }

    @Test
    void cerrar_C3_historialSinFecha_archivaYUsaElInstanteActual() {
        escenario(null);
        Instant antes = Instant.now();

        ResultadoAuditado<CierrePostulacionResponse> r = service.archivar(actorCodigo, finalistaCodigo,
                new CierrePostulacionRequest("No entrego sus documentos"));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.archivada);
        assertThat(r.datos().estado()).isEqualTo("archivada");
        assertThat(r.datos().fecha()).isBetween(antes, Instant.now());
        verify(notificaciones).notificar(any(), any(), any(), anyLong(), any());
    }

    @Test
    void cerrar_C4_historialConFecha_archivaYUsaLaFechaDelHistorial() {
        Instant creado = Instant.parse("2026-11-20T10:00:00Z");
        escenario(creado);

        ResultadoAuditado<CierrePostulacionResponse> r = service.archivar(actorCodigo, finalistaCodigo,
                new CierrePostulacionRequest("No entrego sus documentos"));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.archivada);
        assertThat(r.datos().fecha()).isEqualTo(creado);
    }
}
