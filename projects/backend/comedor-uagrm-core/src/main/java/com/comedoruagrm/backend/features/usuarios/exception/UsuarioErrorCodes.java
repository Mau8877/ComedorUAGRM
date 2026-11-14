package com.comedoruagrm.backend.features.usuarios.exception;

/**
 * Catalogo de codigos de error del modulo usuarios (prefijo US). Documentados
 * en docs/errors/usuarios/ERRORES_USUARIOS.md.
 */
public final class UsuarioErrorCodes {

    /** 409 - El CI ya esta registrado. */
    public static final String ERR_US_01 = "ERR_US_01";

    /** 409 - El correo ya esta registrado. */
    public static final String ERR_US_02 = "ERR_US_02";

    /** 409 - El registro universitario ya esta registrado. */
    public static final String ERR_US_03 = "ERR_US_03";

    /** 404 - La carrera indicada no existe. */
    public static final String ERR_US_04 = "ERR_US_04";

    /** Retirado: era el registro simultaneo con el mismo firebase_uid. No se reasigna el numero. */
    public static final String ERR_US_05 = "ERR_US_05";

    /** 404 - La cuenta del personal no existe, esta eliminada o es de un Postulante. */
    public static final String ERR_US_06 = "ERR_US_06";

    /** 409 - La cuenta ya esta en el estado pedido (desactivar una desactivada, reactivar una activa). */
    public static final String ERR_US_07 = "ERR_US_07";

    /** 403 - No se puede desactivar ni cambiar el rol de la propia cuenta. */
    public static final String ERR_US_08 = "ERR_US_08";

    /** 403 - La operacion dejaria al sistema sin Administrador activo. */
    public static final String ERR_US_09 = "ERR_US_09";

    /** 400 - Rol no valido para el personal (inexistente o Postulante). */
    public static final String ERR_US_10 = "ERR_US_10";

    /** 400 - `sort` o `filter` no soportado en el listado de cuentas del personal. */
    public static final String ERR_US_11 = "ERR_US_11";

    /** 409 - La cuenta ya tiene ese rol. */
    public static final String ERR_US_12 = "ERR_US_12";

    /** 400 - Solo el Postulante tiene teléfono: no se puede actualizar en otra cuenta. */
    public static final String ERR_US_13 = "ERR_US_13";

    private UsuarioErrorCodes() {
    }
}
