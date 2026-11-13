package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.exception.PlantillaIncompletaException;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.AvisoGuardado;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.NuevoAviso;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService.AvisoParaUsuario;

@ExtendWith(MockitoExtension.class)
class NotificacionLoteServiceTest {

    @Mock
    private NotificacionLoteRepository loteRepository;

    @Mock
    private ApplicationEventPublisher publicador;

    private NotificacionLoteService service;

    @BeforeEach
    void preparar() {
        service = new NotificacionLoteService(loteRepository, new PlantillaNotificacion(), publicador);
    }

    private static AvisoParaUsuario aviso(long usuarioId, EventoNotificacion evento) {
        return new AvisoParaUsuario(usuarioId, "u" + usuarioId + "@x.test", evento,
                Map.of("nombres", "Nombre" + usuarioId, "convocatoria", "Beca"), usuarioId + 1000);
    }

    private void insertaYDevuelveFilas() {
        when(loteRepository.insertar(anyList())).thenAnswer(i -> {
            List<NuevoAviso> nuevos = i.getArgument(0);
            List<AvisoGuardado> filas = new ArrayList<>();
            for (NuevoAviso n : nuevos) {
                filas.add(new AvisoGuardado(n.usuarioId() * 10, UUID.randomUUID(), n.usuarioId()));
            }
            return filas;
        });
    }

    @Test
    void notificarLote_guardaCadaAvisoRenderizadoConSuTipo_ypublicaUnEventoConTodosParaDespachar() {
        insertaYDevuelveFilas();

        int guardados = service.notificarLote(List.of(aviso(1, EventoNotificacion.BECA_ADJUDICADA),
                aviso(2, EventoNotificacion.BECA_NO_ADJUDICADA)));

        assertThat(guardados).isEqualTo(2);
        ArgumentCaptor<List<NuevoAviso>> nuevos = ArgumentCaptor.forClass(List.class);
        verify(loteRepository, times(1)).insertar(nuevos.capture());
        assertThat(nuevos.getValue()).extracting(NuevoAviso::tipo)
                .containsExactly(TipoNotificacion.SUCCESS, TipoNotificacion.INFO);
        assertThat(nuevos.getValue().get(0).mensaje()).startsWith("Nombre1, ¡felicidades!").doesNotContain("{");
        assertThat(nuevos.getValue().get(1).mensaje()).contains("no fue adjudicada");
        assertThat(nuevos.getValue().get(0).postulacionId()).isEqualTo(1001L);

        ArgumentCaptor<LoteNotificacionesGuardadas> evento = ArgumentCaptor.forClass(LoteNotificacionesGuardadas.class);
        verify(publicador, times(1)).publishEvent(evento.capture());
        assertThat(evento.getValue().avisos()).extracting(NotificacionGuardada::notificacionId)
                .containsExactly(10L, 20L);
        assertThat(evento.getValue().avisos()).extracting(NotificacionGuardada::correo)
                .containsExactly("u1@x.test", "u2@x.test");
        assertThat(evento.getValue().avisos()).extracting(NotificacionGuardada::evento)
                .containsExactly(EventoNotificacion.BECA_ADJUDICADA, EventoNotificacion.BECA_NO_ADJUDICADA);
    }

    @Test
    void notificarLote_conMasDeUnLote_partePorLotesDeQuinientos() {
        insertaYDevuelveFilas();
        List<AvisoParaUsuario> avisos = new ArrayList<>();
        for (int i = 1; i <= 1200; i++) {
            avisos.add(aviso(i, EventoNotificacion.BECA_ADJUDICADA));
        }

        int guardados = service.notificarLote(avisos);

        assertThat(guardados).isEqualTo(1200);
        ArgumentCaptor<List<NuevoAviso>> lotes = ArgumentCaptor.forClass(List.class);
        verify(loteRepository, times(3)).insertar(lotes.capture());
        assertThat(lotes.getAllValues()).extracting(List::size).containsExactly(500, 500, 200);
        verify(publicador, times(3)).publishEvent(any(LoteNotificacionesGuardadas.class));
    }

    @Test
    void notificarLote_conCincoOQuinientos_haceUnaSolaSentencia() {
        insertaYDevuelveFilas();
        service.notificarLote(listaDe(5));
        verify(loteRepository, times(1)).insertar(anyList());
        org.mockito.Mockito.clearInvocations(loteRepository);

        service.notificarLote(listaDe(500));

        verify(loteRepository, times(1)).insertar(anyList());
    }

    private static List<AvisoParaUsuario> listaDe(int n) {
        List<AvisoParaUsuario> avisos = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            avisos.add(aviso(i, EventoNotificacion.BECA_ADJUDICADA));
        }
        return avisos;
    }

    @Test
    void notificarLote_sinAvisos_noHaceNada() {
        assertThat(service.notificarLote(List.of())).isZero();

        verify(loteRepository, never()).insertar(anyList());
        verify(publicador, never()).publishEvent(any(Object.class));
    }

    @Test
    void notificarLote_conUnaPlantillaIncompleta_lanzaYNoGuardaNiDespacha() {
        AvisoParaUsuario sinNombre = new AvisoParaUsuario(1, "a@x.test", EventoNotificacion.BECA_ADJUDICADA,
                Map.of("convocatoria", "Beca"), null);

        assertThatThrownBy(() -> service.notificarLote(List.of(sinNombre)))
                .isInstanceOf(PlantillaIncompletaException.class);

        verify(loteRepository, never()).insertar(anyList());
        verify(publicador, never()).publishEvent(any(Object.class));
    }

    @Test
    void notificarLote_siFallaGuardar_noPublicaElEventoDeDespacho() {
        when(loteRepository.insertar(anyList())).thenThrow(new IllegalStateException("fallo"));

        assertThatThrownBy(() -> service.notificarLote(listaDe(2))).isInstanceOf(IllegalStateException.class);

        verify(publicador, never()).publishEvent(any(Object.class));
    }
}
