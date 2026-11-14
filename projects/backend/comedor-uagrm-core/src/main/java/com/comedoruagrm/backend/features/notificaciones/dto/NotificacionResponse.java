package com.comedoruagrm.backend.features.notificaciones.dto;

import java.time.Instant;
import java.util.UUID;

import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;

/**
 * Un aviso de la bandeja del usuario. `id` es el codigo publico; `detalle` es el mensaje del aviso.
 * `postulacionCodigo` es el codigo publico de la postulacion a la que apunta el aviso (CU04: tocarlo abre su
 * detalle) y es `null` en los avisos sin postulacion, como un comunicado general.
 */
public record NotificacionResponse(UUID id, TipoNotificacion tipo, String titulo, String detalle, Instant fecha,
        boolean leida, UUID postulacionCodigo) {
}
