package com.comedoruagrm.backend.features.notificaciones.enums;

/**
 * Tipo de aviso. Mismo vocabulario que los toasts de la web (success, info, warning, error);
 * en la base es el enum de PostgreSQL `tipo_notificacion`.
 */
public enum TipoNotificacion {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}
