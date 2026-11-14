package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error de «Mis postulaciones» (CU04), prefijo POS. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Un `sort` o `filter` no soportado reutiliza ERR_SYS_05.
 */
public final class MisPostulacionesErrorCodes {

    /** 404 - La postulacion no existe, es de otra persona o todavia es un borrador. */
    public static final String ERR_POS_01 = "ERR_POS_01";

    private MisPostulacionesErrorCodes() {
    }
}
