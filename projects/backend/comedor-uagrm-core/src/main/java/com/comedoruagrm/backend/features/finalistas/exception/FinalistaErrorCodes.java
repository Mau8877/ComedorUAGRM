package com.comedoruagrm.backend.features.finalistas.exception;

/**
 * Codigos de error de finalistas (CU21), prefijo FIN. Catalogo vivo en
 * docs/errors/finalistas/ERRORES_FINALISTAS.md.
 */
public final class FinalistaErrorCodes {

    /** 404 - El finalista no existe o esta eliminado. */
    public static final String ERR_FIN_01 = "ERR_FIN_01";

    /** 404 - La convocatoria no existe o esta eliminada. */
    public static final String ERR_FIN_02 = "ERR_FIN_02";

    /** 400 - La fecha de recepcion de la documentacion es futura. */
    public static final String ERR_FIN_03 = "ERR_FIN_03";

    /** 400 - El plazo de entrega no es posterior a hoy. */
    public static final String ERR_FIN_04 = "ERR_FIN_04";

    /** 400 - Se mando un `sort` o un `filter[...]` que el listado de finalistas no soporta. */
    public static final String ERR_FIN_05 = "ERR_FIN_05";

    private FinalistaErrorCodes() {
    }
}
