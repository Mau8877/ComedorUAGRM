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

    // Recuperacion de contraseña y perfil (CU15): el actor es el dueño de la cuenta.
    public static final String RESTABLECIMIENTO_CONTRASENA = "RESTABLECIMIENTO_CONTRASENA";
    public static final String CAMBIO_CONTRASENA = "CAMBIO_CONTRASENA";
    public static final String ACTUALIZACION_PERFIL = "ACTUALIZACION_PERFIL";

    // Lista priorizada (CU06): solo se anota la exportacion a PDF (saca datos personales del sistema).
    public static final String EXPORTACION_LISTA_PRIORIZADA = "EXPORTACION_LISTA_PRIORIZADA";

    // CU21: el actor es el administrador; el detalle nunca lleva nombres, CI, correos ni tokens.
    public static final String NOTIFICACION_FINALISTAS = "NOTIFICACION_FINALISTAS";
    public static final String ENTREGA_DOCUMENTACION_REGISTRADA = "ENTREGA_DOCUMENTACION_REGISTRADA";
    public static final String COMUNICADO_PUBLICADO = "COMUNICADO_PUBLICADO";

    private BitacoraAcciones() {
    }
}
