package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.List;

/**
 * Evento interno: un lote de avisos se guardo en la bandeja y falta despacharlo (push y correo). Igual que
 * {@link NotificacionGuardada} pero para varios a la vez, de modo que el despacho resuelva los telefonos de
 * todos los destinatarios en una sola consulta. Solo datos simples.
 */
public record LoteNotificacionesGuardadas(List<NotificacionGuardada> avisos) {
}
