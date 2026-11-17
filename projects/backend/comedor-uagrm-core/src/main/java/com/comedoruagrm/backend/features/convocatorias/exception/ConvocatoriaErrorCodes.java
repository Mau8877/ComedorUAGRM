package com.comedoruagrm.backend.features.convocatorias.exception;

/**
 * Catalogo de codigos de error del modulo convocatorias (prefijo CV), que incluye las gestiones academicas
 * y los documentos requeridos. Documentados en docs/errors/convocatorias/ERRORES_CONVOCATORIAS.md.
 */
public final class ConvocatoriaErrorCodes {

    /** 404 - La convocatoria no existe o esta eliminada. */
    public static final String ERR_CV_01 = "ERR_CV_01";

    /** 404 - La gestion academica no existe o esta eliminada. */
    public static final String ERR_CV_02 = "ERR_CV_02";

    /** 404 - Un tipo de documento indicado no existe. */
    public static final String ERR_CV_03 = "ERR_CV_03";

    /** 400 - La fecha de cierre no es posterior a la de apertura. */
    public static final String ERR_CV_04 = "ERR_CV_04";

    /** 400 - Ningun documento de la lista es obligatorio. */
    public static final String ERR_CV_05 = "ERR_CV_05";

    /** 400 - Un tipo de documento aparece mas de una vez en la lista. */
    public static final String ERR_CV_06 = "ERR_CV_06";

    /** 409 - Ya hay otra convocatoria abierta en la misma gestion academica. */
    public static final String ERR_CV_07 = "ERR_CV_07";

    /** 409 - La operacion no corresponde al estado actual de la convocatoria. */
    public static final String ERR_CV_08 = "ERR_CV_08";

    /** 409 - La convocatoria ya tiene postulaciones: no se cambian sus documentos ni su fecha de apertura. */
    public static final String ERR_CV_09 = "ERR_CV_09";

    /** 409 - La convocatoria tiene postulaciones: no se elimina, solo se cierra. */
    public static final String ERR_CV_10 = "ERR_CV_10";

    /** 400 - `sort` no soportado en el listado. */
    public static final String ERR_CV_11 = "ERR_CV_11";

    /** 409 - Ya existe una gestion academica con esa etiqueta. */
    public static final String ERR_CV_12 = "ERR_CV_12";

    /** 400 - La fecha de fin de la gestion academica no es posterior a la de inicio. */
    public static final String ERR_CV_13 = "ERR_CV_13";

    private ConvocatoriaErrorCodes() {
    }
}
