package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error del envio de la postulacion (CU02), prefijo ENV. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Los rechazos de elegibilidad y duplicados son del CU18
 * (`ERR_VAL_*`).
 */
public final class EnvioPostulacionErrorCodes {

    /** 409 - No hay una convocatoria abierta que reciba postulaciones (A1). */
    public static final String ERR_ENV_01 = "ERR_ENV_01";

    /** 404 - La postulacion no existe o no es del postulante. */
    public static final String ERR_ENV_02 = "ERR_ENV_02";

    /** 409 - La postulacion ya fue enviada y no esta en borrador ni devuelta: no se puede enviar. */
    public static final String ERR_ENV_03 = "ERR_ENV_03";

    /** 400 - Falta algun campo o documento obligatorio (A3). */
    public static final String ERR_ENV_04 = "ERR_ENV_04";

    /** 400 - Un dato del formulario tiene un formato, un rango o una opcion invalida. */
    public static final String ERR_ENV_05 = "ERR_ENV_05";

    private EnvioPostulacionErrorCodes() {
    }
}
