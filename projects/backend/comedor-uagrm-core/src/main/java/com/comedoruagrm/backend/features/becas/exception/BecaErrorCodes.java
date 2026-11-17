package com.comedoruagrm.backend.features.becas.exception;

/**
 * Catalogo de codigos de error del modulo becas (prefijo BEC): el padron de becados (CU23). Documentados en
 * docs/errors/becas/ERRORES_BECAS.md.
 */
public final class BecaErrorCodes {

    /** 404 - La beca no existe o esta eliminada. */
    public static final String ERR_BEC_01 = "ERR_BEC_01";

    /** 404 - La convocatoria no existe o esta eliminada. */
    public static final String ERR_BEC_02 = "ERR_BEC_02";

    /** 409 - Los resultados de la convocatoria todavia no fueron publicados. */
    public static final String ERR_BEC_03 = "ERR_BEC_03";

    /** 409 - No hay adjudicados pendientes de alta en la convocatoria. */
    public static final String ERR_BEC_04 = "ERR_BEC_04";

    /** 409 - La beca no esta vigente: no se puede dar de baja ni suspender. */
    public static final String ERR_BEC_05 = "ERR_BEC_05";

    /** 409 - La beca no fue dada de baja: no se puede reemplazar. */
    public static final String ERR_BEC_06 = "ERR_BEC_06";

    /** 409 - No hay un postulante disponible para ocupar el cupo. */
    public static final String ERR_BEC_07 = "ERR_BEC_07";

    /** 409 - El postulante confirmado ya no es el siguiente disponible. */
    public static final String ERR_BEC_08 = "ERR_BEC_08";

    /** 409 - La beca ya fue reemplazada. */
    public static final String ERR_BEC_09 = "ERR_BEC_09";

    /** 400 - El campo de `sort` no esta soportado por el listado del padron. */
    public static final String ERR_BEC_10 = "ERR_BEC_10";

    private BecaErrorCodes() {
    }
}
