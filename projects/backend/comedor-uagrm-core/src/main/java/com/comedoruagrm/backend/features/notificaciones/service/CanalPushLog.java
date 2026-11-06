package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Push "de mentira": no envia nada, solo deja en el log cuantos telefonos se habrian
 * avisado y el titulo. Se usa cuando no hay credenciales de Firebase.
 *
 * ! Nunca escribe los tokens ni el cuerpo del mensaje.
 */
public class CanalPushLog implements CanalPush {

    private static final Logger LOG = LoggerFactory.getLogger(CanalPushLog.class);

    @Override
    public ResultadoPush enviar(List<String> tokens, MensajePush mensaje) {
        LOG.info("Push en modo log: no se envia a Firebase",
                kv("module", "notificaciones"), kv("telefonos", tokens.size()), kv("titulo", mensaje.titulo()));
        // Se da por entregado: asi el aviso queda marcado como enviado en desarrollo.
        return new ResultadoPush(tokens.size(), List.of(), List.of());
    }
}
