package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.UUID;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;

/**
 * Evento interno: un aviso se guardo en la bandeja y falta despacharlo (push y correo).
 * Lleva solo datos simples (no entidades), porque lo atiende otro hilo despues del commit.
 * `postulacionCodigo` es el codigo publico de la postulacion del aviso (nulo si no tiene): viaja en el `data` del push.
 */
public record NotificacionGuardada(Long notificacionId, UUID notificacionCodigo, Long usuarioId, String correo,
        String titulo, String mensaje, EventoNotificacion evento, UUID postulacionCodigo) {
}
