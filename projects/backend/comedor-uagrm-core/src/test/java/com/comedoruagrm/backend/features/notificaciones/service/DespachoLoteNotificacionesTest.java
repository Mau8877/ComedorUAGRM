package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;

@ExtendWith(MockitoExtension.class)
class DespachoLoteNotificacionesTest {

    @Mock
    private CanalPush canalPush;

    @Mock
    private DispositivoFcmService dispositivoService;

    @Mock
    private CorreoNotificacionService correoService;

    @Mock
    private NotificacionService notificacionService;

    private DespachoLoteNotificaciones despacho;

    @BeforeEach
    void preparar() {
        DespachoNotificaciones individual = new DespachoNotificaciones(canalPush, dispositivoService, correoService,
                notificacionService, new Reintentos(3, new long[] {0}));
        despacho = new DespachoLoteNotificaciones(individual, dispositivoService, correoService, notificacionService);
    }

    private static NotificacionGuardada aviso(long id, long usuarioId) {
        return new NotificacionGuardada(id, UUID.randomUUID(), usuarioId, "u" + usuarioId + "@x.test", "Titulo",
                "Mensaje " + usuarioId, EventoNotificacion.BECA_ADJUDICADA);
    }

    @Test
    void despachar_resuelveLosTelefonosDeTodosEnUnaSolaConsulta_yEnviaPushYCorreoACadaUno() {
        when(dispositivoService.tokensDe(anyCollection())).thenReturn(Map.of(1L, List.of("t1"), 2L, List.of("t2a", "t2b")));
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenAnswer(i -> new ResultadoPush(((List<?>) i.getArgument(0)).size(), List.of(), List.of()));
        when(correoService.enviar(any(), any(), any())).thenReturn(true);

        despacho.despachar(new LoteNotificacionesGuardadas(List.of(aviso(10, 1), aviso(20, 2), aviso(30, 3))));

        verify(dispositivoService, times(1)).tokensDe(anyCollection());
        verify(dispositivoService, never()).tokensDe(anyLong());
        ArgumentCaptor<MensajePush> mensajes = ArgumentCaptor.forClass(MensajePush.class);
        verify(canalPush, times(2)).enviar(anyList(), mensajes.capture());
        assertThat(mensajes.getAllValues()).extracting(MensajePush::cuerpo).containsExactly("Mensaje 1", "Mensaje 2");
        assertThat(mensajes.getAllValues().get(0).datos()).containsEntry("evento", "BECA_ADJUDICADA")
                .containsKey("codigoNotificacion");
        verify(correoService, times(3)).enviar(any(), any(), any());
        ArgumentCaptor<Collection<Long>> marcadas = ArgumentCaptor.forClass(Collection.class);
        verify(notificacionService).marcarEnviadas(marcadas.capture());
        assertThat(marcadas.getValue()).containsExactlyInAnyOrder(10L, 20L, 30L);
    }

    @Test
    void despachar_siFallaElEnvioDeUno_losDemasSiSalen_yElFallidoNoSeMarcaEnviado() {
        when(dispositivoService.tokensDe(anyCollection())).thenReturn(Map.of());
        when(correoService.enviar("u1@x.test", "Titulo", "Mensaje 1")).thenReturn(false);
        when(correoService.enviar("u2@x.test", "Titulo", "Mensaje 2")).thenReturn(true);

        despacho.despachar(new LoteNotificacionesGuardadas(List.of(aviso(10, 1), aviso(20, 2))));

        ArgumentCaptor<Collection<Long>> marcadas = ArgumentCaptor.forClass(Collection.class);
        verify(notificacionService).marcarEnviadas(marcadas.capture());
        assertThat(marcadas.getValue()).containsExactly(20L);
    }

    @Test
    void despachar_siUnAvisoLanzaUnaExcepcion_seTragaYSigueConElResto() {
        when(dispositivoService.tokensDe(anyCollection())).thenReturn(Map.of());
        when(correoService.enviar("u1@x.test", "Titulo", "Mensaje 1")).thenThrow(new IllegalStateException("smtp"));
        when(correoService.enviar("u2@x.test", "Titulo", "Mensaje 2")).thenReturn(true);

        despacho.despachar(new LoteNotificacionesGuardadas(List.of(aviso(10, 1), aviso(20, 2))));

        ArgumentCaptor<Collection<Long>> marcadas = ArgumentCaptor.forClass(Collection.class);
        verify(notificacionService).marcarEnviadas(marcadas.capture());
        assertThat(marcadas.getValue()).containsExactly(20L);
    }

    @Test
    void despachar_siFallaLaConsultaDeTelefonos_noLanzaYNoMarcaNada() {
        when(dispositivoService.tokensDe(anyCollection())).thenThrow(new IllegalStateException("db"));

        despacho.despachar(new LoteNotificacionesGuardadas(List.of(aviso(10, 1))));

        verify(notificacionService, never()).marcarEnviadas(anyCollection());
        verify(correoService, never()).enviar(any(), any(), any());
    }
}
