package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error de la lista priorizada (CU06), prefijo PRI. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md.
 */
public final class PriorizacionErrorCodes {

    /** 404 - La convocatoria pedida no existe o esta eliminada. */
    public static final String ERR_PRI_01 = "ERR_PRI_01";

    /** 400 - Se mando un `sort` o un `filter[...]` que la lista no soporta. */
    public static final String ERR_PRI_02 = "ERR_PRI_02";

    /** 400 - La lista a exportar esta vacia: no hay postulaciones que exportar. */
    public static final String ERR_PRI_03 = "ERR_PRI_03";

    private PriorizacionErrorCodes() {
    }
}
