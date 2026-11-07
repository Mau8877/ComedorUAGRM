package com.comedoruagrm.backend.features.notificaciones.exception;

/**
 * Codigos de error de la bandeja de avisos (CU21 / NOTIFICACIONES_WEB), prefijo NOT. Catalogo vivo
 * en docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md.
 */
public final class NotificacionErrorCodes {

    /** 404 - El aviso no existe o no es del usuario autenticado (no se distingue, para no filtrar datos). */
    public static final String ERR_NOT_01 = "ERR_NOT_01";

    /** 400 - Se mando un `sort` que la bandeja no soporta. */
    public static final String ERR_NOT_02 = "ERR_NOT_02";

    private NotificacionErrorCodes() {
    }
}
