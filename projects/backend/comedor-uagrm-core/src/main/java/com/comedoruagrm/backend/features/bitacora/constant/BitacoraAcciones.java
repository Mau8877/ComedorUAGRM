package com.comedoruagrm.backend.features.bitacora.constant;

/**
 * Valores de `bitacora.accion` que escribe el backend. Los intentos fallidos
 * de inicio de sesion no estan: se cuentan en `usuario.intentos_fallidos`.
 */
public final class BitacoraAcciones {

    public static final String REGISTRO_USUARIO = "REGISTRO_USUARIO";
    public static final String INICIO_SESION = "INICIO_SESION";

    private BitacoraAcciones() {
    }
}
