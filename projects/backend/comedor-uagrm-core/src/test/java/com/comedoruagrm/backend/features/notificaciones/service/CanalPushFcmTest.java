package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;

class CanalPushFcmTest {

    private static final MensajePush MENSAJE = new MensajePush("Titulo", "Cuerpo", Map.of("evento", "PRUEBA"));

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final CanalPushFcm canal = new CanalPushFcm(messaging, "notificaciones");

    @Test
    void enviar_conTodosLosEnviosCorrectos_cuentaLosEnviadosYNoMarcaNada() throws Exception {
        BatchResponse respuesta = lote(ok(), ok());
        when(messaging.sendEach(anyList())).thenReturn(respuesta);

        ResultadoPush resultado = canal.enviar(List.of("t1", "t2"), MENSAJE);

        assertThat(resultado.enviados()).isEqualTo(2);
        assertThat(resultado.tokensInvalidos()).isEmpty();
        assertThat(resultado.tokensPasajeros()).isEmpty();
    }

    @Test
    void enviar_clasificaLosErroresSegunLoQueDiceFirebase() throws Exception {
        BatchResponse respuesta = lote(
                ok(),
                error(MessagingErrorCode.UNREGISTERED),
                error(MessagingErrorCode.INVALID_ARGUMENT),
                error(MessagingErrorCode.UNAVAILABLE),
                error(MessagingErrorCode.QUOTA_EXCEEDED),
                error(MessagingErrorCode.SENDER_ID_MISMATCH));
        when(messaging.sendEach(anyList())).thenReturn(respuesta);

        ResultadoPush resultado = canal.enviar(List.of("ok", "desinstalada", "malformado", "caido", "cuota", "otro"),
                MENSAJE);

        assertThat(resultado.enviados()).isEqualTo(1);
        assertThat(resultado.tokensInvalidos()).containsExactly("desinstalada", "malformado");
        assertThat(resultado.tokensPasajeros()).containsExactly("caido", "cuota");
    }

    @Test
    void enviar_conMasDe500Tokens_losParteEnLotesDeHasta500() throws Exception {
        List<String> tokens = IntStream.range(0, 600).mapToObj(i -> "token-" + i).toList();
        when(messaging.sendEach(anyList())).thenAnswer(invocacion -> {
            List<?> mensajes = invocacion.getArgument(0);
            SendResponse[] respuestas = new SendResponse[mensajes.size()];
            for (int i = 0; i < respuestas.length; i++) {
                respuestas[i] = ok();
            }
            return lote(respuestas);
        });

        ResultadoPush resultado = canal.enviar(tokens, MENSAJE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> lotes = ArgumentCaptor.forClass(List.class);
        verify(messaging, times(2)).sendEach(lotes.capture());
        assertThat(lotes.getAllValues().get(0)).hasSize(500);
        assertThat(lotes.getAllValues().get(1)).hasSize(100);
        assertThat(resultado.enviados()).isEqualTo(600);
    }

    @Test
    void enviar_siFallaLaLlamadaCompleta_todoElLoteQuedaComoPasajeroYNoLanza() throws Exception {
        when(messaging.sendEach(anyList())).thenThrow(new IllegalStateException("sin red"));

        ResultadoPush resultado = canal.enviar(new ArrayList<>(List.of("t1", "t2")), MENSAJE);

        assertThat(resultado.enviados()).isZero();
        assertThat(resultado.tokensPasajeros()).containsExactly("t1", "t2");
        assertThat(resultado.tokensInvalidos()).isEmpty();
    }

    private static SendResponse ok() {
        SendResponse respuesta = mock(SendResponse.class);
        when(respuesta.isSuccessful()).thenReturn(true);
        return respuesta;
    }

    private static SendResponse error(MessagingErrorCode codigo) {
        FirebaseMessagingException excepcion = mock(FirebaseMessagingException.class);
        when(excepcion.getMessagingErrorCode()).thenReturn(codigo);
        SendResponse respuesta = mock(SendResponse.class);
        when(respuesta.isSuccessful()).thenReturn(false);
        when(respuesta.getException()).thenReturn(excepcion);
        return respuesta;
    }

    private static BatchResponse lote(SendResponse... respuestas) {
        BatchResponse lote = mock(BatchResponse.class);
        when(lote.getResponses()).thenReturn(List.of(respuestas));
        return lote;
    }
}
