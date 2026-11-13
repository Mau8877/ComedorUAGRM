package com.comedoruagrm.backend.features.comunicados.exception;

/**
 * Codigos de error de comunicados (CU21), prefijo COM. Catalogo vivo en
 * docs/errors/comunicados/ERRORES_COMUNICADOS.md.
 */
public final class ComunicadoErrorCodes {

    /** 400 - El grupo de destinatarios elegido no tiene a nadie. */
    public static final String ERR_COM_01 = "ERR_COM_01";

    /** 400 - Un comunicado a `FINALISTAS` o a `ADJUDICADOS` necesita la convocatoria. */
    public static final String ERR_COM_02 = "ERR_COM_02";

    private ComunicadoErrorCodes() {
    }
}
