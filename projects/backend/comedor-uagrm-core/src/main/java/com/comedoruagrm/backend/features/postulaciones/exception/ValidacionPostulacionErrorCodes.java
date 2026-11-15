package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error de la validacion de elegibilidad y duplicados (CU18), prefijo VAL. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Una convocatoria inexistente reutiliza ERR_CV_01.
 */
public final class ValidacionPostulacionErrorCodes {

    /** 409 - El postulante ya tiene una postulacion vigente en la misma gestion academica. */
    public static final String ERR_VAL_01 = "ERR_VAL_01";

    /** 403 - El postulante no cumple las condiciones de elegibilidad (cuenta desactivada o que no es de un postulante). */
    public static final String ERR_VAL_02 = "ERR_VAL_02";

    /** 409 - La convocatoria no esta abierta o la fecha actual esta fuera de su apertura y cierre. */
    public static final String ERR_VAL_03 = "ERR_VAL_03";

    private ValidacionPostulacionErrorCodes() {
    }
}
