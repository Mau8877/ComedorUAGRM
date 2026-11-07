package com.comedoruagrm.backend.features.notificaciones.dto;

import java.time.Instant;
import java.util.UUID;

import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;

/** Un aviso de la bandeja del usuario. `id` es el codigo publico; `detalle` es el mensaje del aviso. */
public record NotificacionResponse(UUID id, TipoNotificacion tipo, String titulo, String detalle, Instant fecha,
        boolean leida) {
}
