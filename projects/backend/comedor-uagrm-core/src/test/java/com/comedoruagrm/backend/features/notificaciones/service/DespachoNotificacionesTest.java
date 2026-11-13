package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;

@ExtendWith(MockitoExtension.class)
class DespachoNotificacionesTest {

    private static final NotificacionGuardada AVISO = new NotificacionGuardada(99L, UUID.randomUUID(), 7L,
            "ana@uagrm.edu.bo", "Titulo", "Mensaje", EventoNotificacion.PRUEBA, null);

    @Mock
    private CanalPush canalPush;

    @Mock
    private DispositivoFcmService dispositivoService;

    @Mock
    private CorreoNotificacionService correoService;

    @Mock
    private NotificacionService notificacionService;

    private DespachoNotificaciones despacho() {
        return new DespachoNotificaciones(canalPush, dispositivoService, correoService, notificacionService,
                new Reintentos(3, new long[] {0}));
    }

    @Test
    void despachar_conDosTelefonos_enviaUnPushATodosYElCorreoYMarcaEnviada() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("t1", "t2"));
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(2, List.of(), List.of()));
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(true);

        despacho().despachar(AVISO);

        ArgumentCaptor<MensajePush> mensaje = ArgumentCaptor.forClass(MensajePush.class);
        verify(canalPush).enviar(eq(List.of("t1", "t2")), mensaje.capture());
        assertThat(mensaje.getValue().titulo()).isEqualTo("Titulo");
        assertThat(mensaje.getValue().datos()).containsEntry("codigoNotificacion", AVISO.notificacionCodigo().toString())
                .containsEntry("evento", "PRUEBA");
        verify(correoService).enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje");
        verify(notificacionService).marcarEnviada(99L);
    }

    @Test
    void despachar_deUnEventoSinCorreo_enviaSoloElPushYNuncaElCorreo() {
        NotificacionGuardada comunicado = new NotificacionGuardada(100L, UUID.randomUUID(), 7L, null, "Titulo",
                "Mensaje", EventoNotificacion.COMUNICADO, null);
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("t1"));
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(1, List.of(), List.of()));

        despacho().despachar(comunicado);

        verify(correoService, never()).enviar(any(), any(), any());
        verify(notificacionService).marcarEnviada(100L);
    }

    @Test
    void despachar_sinTelefonos_enviaSoloElCorreo() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of());
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(true);

        despacho().despachar(AVISO);

        verify(canalPush, never()).enviar(anyList(), any(MensajePush.class));
        verify(notificacionService).marcarEnviada(99L);
    }

    @Test
    void despachar_conTokensInvalidos_losEliminaYSigueConElCorreo() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("bueno", "desinstalado"));
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(1, List.of("desinstalado"), List.of()));
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(true);

        despacho().despachar(AVISO);

        verify(dispositivoService).eliminarInvalidos(Set.of("desinstalado"));
        verify(correoService).enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje");
    }

    @Test
    void despachar_siElPushFallaDePasoYSeRecupera_reintentaSoloLosPasajeros() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("a", "b"));
        when(canalPush.enviar(eq(List.of("a", "b")), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(1, List.of(), List.of("b")));
        when(canalPush.enviar(eq(List.of("b")), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(1, List.of(), List.of()));
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(false);

        despacho().despachar(AVISO);

        verify(canalPush, times(2)).enviar(anyList(), any(MensajePush.class));
        verify(notificacionService).marcarEnviada(99L);
    }

    @Test
    void despachar_siElPushAgotaLosReintentosYElCorreoLlega_marcaEnviadaPorElCorreo() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("a"));
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(0, List.of(), List.of("a")));
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(true);

        despacho().despachar(AVISO);

        verify(canalPush, times(3)).enviar(anyList(), any(MensajePush.class));
        verify(dispositivoService).eliminarInvalidos(anyCollection());
        verify(notificacionService).marcarEnviada(99L);
    }

    @Test
    void despachar_siNingunaViaLlega_noMarcaEnviadaYNoLanza() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("a"));
        when(canalPush.enviar(anyList(), any(MensajePush.class)))
                .thenReturn(new ResultadoPush(0, List.of(), List.of("a")));
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(false);

        despacho().despachar(AVISO);

        verify(notificacionService, never()).marcarEnviada(any(Long.class));
    }

    @Test
    void despachar_siElCanalDePushLanzaUnError_elCorreoSaleIgualYNoSePropaga() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("a"));
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenThrow(new IllegalStateException("boom"));
        when(correoService.enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje")).thenReturn(true);

        despacho().despachar(AVISO);

        verify(correoService).enviar("ana@uagrm.edu.bo", "Titulo", "Mensaje");
        verify(notificacionService).marcarEnviada(99L);
    }

    @Test
    void despachar_siFallaLaLecturaDeTelefonos_noSePropagaElError() {
        when(dispositivoService.tokensDe(7L)).thenThrow(new IllegalStateException("base caida"));

        despacho().despachar(AVISO);

        verify(notificacionService, never()).marcarEnviada(any(Long.class));
    }
}
