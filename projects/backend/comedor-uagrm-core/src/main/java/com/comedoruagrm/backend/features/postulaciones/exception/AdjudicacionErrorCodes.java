package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error de la adjudicacion (CU05), prefijo ADJ, todos 409 / ConflictException. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Una convocatoria inexistente reutiliza ERR_PRI_01, un
 * finalista inexistente ERR_FIN_01 y una cantidad o un motivo invalido caen en ERR_SYS_01.
 */
public final class AdjudicacionErrorCodes {

    /** 409 - La postulacion ya no esta «calificada» o los resultados de la convocatoria ya se publicaron. */
    public static final String ERR_ADJ_01 = "ERR_ADJ_01";

    /** 409 - El expediente no esta auditado o la ultima auditoria no es «Validado» (al adjudicar). */
    public static final String ERR_ADJ_02 = "ERR_ADJ_02";

    /** 409 - El puntaje total no alcanza el puntaje minimo de la convocatoria. */
    public static final String ERR_ADJ_03 = "ERR_ADJ_03";

    /** 409 - Los cupos de la convocatoria estan agotados. */
    public static final String ERR_ADJ_04 = "ERR_ADJ_04";

    /** 409 - La convocatoria no esta cerrada o no tiene cupos o puntaje minimo definidos. */
    public static final String ERR_ADJ_05 = "ERR_ADJ_05";

    private AdjudicacionErrorCodes() {
    }
}
