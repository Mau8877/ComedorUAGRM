package com.comedoruagrm.backend.features.mibeca.exception;

/**
 * Codigos de error de la consulta del becado sobre su beca (CU24), prefijo MBE. Catalogo vivo en
 * docs/errors/mibeca/ERRORES_MIBECA.md. Un `page`/`pageSize` invalido o un `filter` no soportado cae en
 * ERR_SYS_01 / ERR_SYS_05.
 */
public final class MiBecaErrorCodes {

    /** 404 - El usuario no tiene ninguna beca (no figura en el padron de becados). */
    public static final String ERR_MBE_01 = "ERR_MBE_01";

    /** 400 - El rango de fechas del historial es invalido (la fecha «desde» es posterior a «hasta»). */
    public static final String ERR_MBE_02 = "ERR_MBE_02";

    /** 400 - El campo de `sort` no esta soportado por el historial de asistencia. */
    public static final String ERR_MBE_03 = "ERR_MBE_03";

    private MiBecaErrorCodes() {
    }
}
