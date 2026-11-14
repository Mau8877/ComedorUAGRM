package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error del cierre de una postulacion, archivar o rechazar (CU20), prefijo ARC. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Un finalista inexistente reutiliza ERR_FIN_01 y
 * un motivo vacio o de mas de 500 caracteres cae en ERR_SYS_01.
 */
public final class CierrePostulacionErrorCodes {

    /** 409 - La postulacion ya no esta «calificada» o los resultados de la convocatoria ya se publicaron. */
    public static final String ERR_ARC_01 = "ERR_ARC_01";

    private CierrePostulacionErrorCodes() {
    }
}
