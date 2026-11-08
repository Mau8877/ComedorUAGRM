package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error de la devolucion de una postulacion (CU19), prefijo DEV. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Un finalista inexistente reutiliza ERR_FIN_01 y
 * un motivo vacio o de mas de 500 caracteres cae en ERR_SYS_01.
 */
public final class DevolucionErrorCodes {

    /** 409 - La postulacion ya no esta «calificada» (ya devuelta, archivada, rechazada o adjudicada). */
    public static final String ERR_DEV_01 = "ERR_DEV_01";

    /** 409 - El finalista no tiene una auditoria registrada despues de su ultima devolucion. */
    public static final String ERR_DEV_02 = "ERR_DEV_02";

    private DevolucionErrorCodes() {
    }
}
