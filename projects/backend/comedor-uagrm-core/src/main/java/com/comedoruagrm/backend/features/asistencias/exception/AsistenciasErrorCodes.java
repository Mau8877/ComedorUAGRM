package com.comedoruagrm.backend.features.asistencias.exception;

/**
 * Codigos de error de la asistencia digital (CU10), prefijo ASI. Catalogo vivo en
 * docs/errors/asistencias/ERRORES_ASISTENCIAS.md. Un CI vacio o demasiado largo cae en ERR_SYS_01 (Bean Validation).
 */
public final class AsistenciasErrorCodes {

    /** 404 - El CI no corresponde a ningun becado del padron. */
    public static final String ERR_ASI_01 = "ERR_ASI_01";

    /** 409 - La beca no esta vigente (suspendida o dada de baja): no se registra asistencia. */
    public static final String ERR_ASI_02 = "ERR_ASI_02";

    /** 404 - La beca no existe o fue eliminada. */
    public static final String ERR_ASI_03 = "ERR_ASI_03";

    private AsistenciasErrorCodes() {
    }
}
