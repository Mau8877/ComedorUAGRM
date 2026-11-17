package com.comedoruagrm.backend.features.planillas.exception;

/**
 * Codigos de error de la planilla de asistencia (CU09), prefijo PLA. Catalogo vivo en
 * docs/errors/planillas/ERRORES_PLANILLAS.md. Una convocatoria inexistente reutiliza ERR_PRI_01 y un `sort` o
 * `filter` no soportado, ERR_SYS_05.
 */
public final class PlanillasErrorCodes {

    /** 400 - La fecha final del periodo es anterior a la inicial. */
    public static final String ERR_PLA_01 = "ERR_PLA_01";

    /** 400 - El periodo no esta comprendido en la gestion academica de la convocatoria. */
    public static final String ERR_PLA_02 = "ERR_PLA_02";

    /** 400 - El periodo supera los 31 dias que caben en una planilla. */
    public static final String ERR_PLA_03 = "ERR_PLA_03";

    /** 409 - La convocatoria no tiene becas vigentes. */
    public static final String ERR_PLA_04 = "ERR_PLA_04";

    /** 404 - La planilla no existe. */
    public static final String ERR_PLA_05 = "ERR_PLA_05";

    /** 404 - El archivo de la planilla no esta en el almacenamiento. */
    public static final String ERR_PLA_06 = "ERR_PLA_06";

    private PlanillasErrorCodes() {
    }
}
