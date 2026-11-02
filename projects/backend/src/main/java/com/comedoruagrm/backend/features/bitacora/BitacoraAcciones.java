package com.comedoruagrm.backend.features.bitacora;

/**
 * Valores de `bitacora.accion` que escribe el backend. Los intentos fallidos
 * de inicio de sesion no estan: los ve Firebase, no este backend.
 */
public final class BitacoraAcciones {

    public static final String REGISTRO_USUARIO = "REGISTRO_USUARIO";
    public static final String INICIO_SESION = "INICIO_SESION";

    private BitacoraAcciones() {
    }
}
