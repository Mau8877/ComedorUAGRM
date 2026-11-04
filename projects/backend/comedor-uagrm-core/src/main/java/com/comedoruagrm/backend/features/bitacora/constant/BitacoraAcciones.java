package com.comedoruagrm.backend.features.bitacora.constant;

/**
 * Valores de `bitacora.accion` que escribe el backend. Los intentos fallidos
 * de inicio de sesion no estan: se cuentan en `usuario.intentos_fallidos`.
 */
public final class BitacoraAcciones {

    public static final String REGISTRO_USUARIO = "REGISTRO_USUARIO";
    public static final String INICIO_SESION = "INICIO_SESION";

    // Gestion de cuentas del personal (CU14): el actor es el administrador, no la cuenta afectada.
    public static final String ALTA_PERSONAL = "ALTA_PERSONAL";
    public static final String EDICION_USUARIO = "EDICION_USUARIO";
    public static final String CAMBIO_ROL = "CAMBIO_ROL";
    public static final String DESACTIVACION_USUARIO = "DESACTIVACION_USUARIO";
    public static final String REACTIVACION_USUARIO = "REACTIVACION_USUARIO";

    private BitacoraAcciones() {
    }
}
