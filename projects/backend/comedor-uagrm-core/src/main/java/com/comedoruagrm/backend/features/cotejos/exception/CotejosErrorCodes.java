package com.comedoruagrm.backend.features.cotejos.exception;

/**
 * Codigos de error del cotejo de asistencia (CU11), prefijo COT. Catalogo vivo en
 * docs/errors/cotejos/ERRORES_COTEJOS.md. Un campo vacio o fuera de rango cae en ERR_SYS_01 (Bean Validation) y un
 * `sort` no soportado, en ERR_SYS_05.
 */
public final class CotejosErrorCodes {

    /** 404 - La planilla no existe o fue eliminada. */
    public static final String ERR_COT_01 = "ERR_COT_01";

    /** 400 - Las coincidencias y las discrepancias no suman el total de registros. */
    public static final String ERR_COT_02 = "ERR_COT_02";

    /** 400 - El resultado no corresponde a las discrepancias (conforme con discrepancias, o al reves). */
    public static final String ERR_COT_03 = "ERR_COT_03";

    /** 400 - Un cotejo con discrepancias exige observaciones. */
    public static final String ERR_COT_04 = "ERR_COT_04";

    private CotejosErrorCodes() {
    }
}
