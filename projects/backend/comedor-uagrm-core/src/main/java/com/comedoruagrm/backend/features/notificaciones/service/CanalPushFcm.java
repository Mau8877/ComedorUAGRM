package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

/**
 * Push por Firebase Cloud Messaging.
 *
 * * Envia en lotes de hasta 500 tokens (el maximo de `sendEach`). Un token que Firebase
 * * rechaza como inexistente o mal formado se informa como invalido; un fallo pasajero
 * * (Firebase caido, cuota), como reintentable; cualquier otro error solo se cuenta.
 * ! Nunca escribe un token ni el cuerpo del mensaje en el log.
 */
public class CanalPushFcm implements CanalPush {

    static final int TAMANO_LOTE = 500;

    private static final Logger LOG = LoggerFactory.getLogger(CanalPushFcm.class);

    private final FirebaseMessaging messaging;
    private final String canalAndroid;

    public CanalPushFcm(FirebaseMessaging messaging, String canalAndroid) {
        this.messaging = messaging;
        this.canalAndroid = canalAndroid;
    }

    @Override
    public ResultadoPush enviar(List<String> tokens, MensajePush mensaje) {
        int enviados = 0;
        List<String> invalidos = new ArrayList<>();
        List<String> pasajeros = new ArrayList<>();
        for (int desde = 0; desde < tokens.size(); desde += TAMANO_LOTE) {
            List<String> lote = tokens.subList(desde, Math.min(desde + TAMANO_LOTE, tokens.size()));
            enviados += enviarLote(lote, mensaje, invalidos, pasajeros);
        }
        return new ResultadoPush(enviados, invalidos, pasajeros);
    }

    private int enviarLote(List<String> lote, MensajePush mensaje, List<String> invalidos, List<String> pasajeros) {
        List<Message> mensajes = lote.stream().map(token -> armar(token, mensaje)).toList();
        BatchResponse respuesta;
        try {
            respuesta = messaging.sendEach(mensajes);
        } catch (FirebaseMessagingException | RuntimeException e) {
            // Falla la llamada completa (red, credenciales): todo el lote es reintentable.
            LOG.warn("No se pudo llamar a Firebase", kv("module", "notificaciones"),
                    kv("telefonos", lote.size()), kv("causa", e.getClass().getSimpleName()));
            pasajeros.addAll(lote);
            return 0;
        }
        int enviados = 0;
        List<SendResponse> resultados = respuesta.getResponses();
        for (int i = 0; i < lote.size(); i++) {
            SendResponse resultado = resultados.get(i);
            if (resultado.isSuccessful()) {
                enviados++;
            } else {
                clasificar(lote.get(i), resultado.getException(), invalidos, pasajeros);
            }
        }
        return enviados;
    }

    private Message armar(String token, MensajePush mensaje) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(mensaje.titulo()).setBody(mensaje.cuerpo()).build())
                .putAllData(mensaje.datos())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder().setChannelId(canalAndroid).build())
                        .build())
                .build();
    }

    private void clasificar(String token, FirebaseMessagingException error, List<String> invalidos,
            List<String> pasajeros) {
        MessagingErrorCode codigo = error == null ? null : error.getMessagingErrorCode();
        if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
            invalidos.add(token);
        } else if (esPasajero(codigo, error)) {
            pasajeros.add(token);
        } else {
            LOG.warn("Firebase rechazo un envio", kv("module", "notificaciones"),
                    kv("errorFirebase", codigo == null ? "desconocido" : codigo.name()));
        }
    }

    private boolean esPasajero(MessagingErrorCode codigo, FirebaseMessagingException error) {
        if (codigo == MessagingErrorCode.UNAVAILABLE || codigo == MessagingErrorCode.INTERNAL
                || codigo == MessagingErrorCode.QUOTA_EXCEEDED) {
            return true;
        }
        if (codigo != null || error == null) {
            return false;
        }
        ErrorCode general = error.getErrorCode();
        return general == ErrorCode.UNAVAILABLE || general == ErrorCode.INTERNAL
                || general == ErrorCode.DEADLINE_EXCEEDED || general == ErrorCode.RESOURCE_EXHAUSTED;
    }
}
