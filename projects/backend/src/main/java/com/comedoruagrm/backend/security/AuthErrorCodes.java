package com.comedoruagrm.backend.security;

/**
 * Catalogo de codigos de error de autenticacion (modulo AUTH). Documentados
 * en docs/errors/auth/ERRORES_AUTH.md.
 */
public final class AuthErrorCodes {

    /** 401 - Token de acceso ausente, mal firmado o vencido. */
    public static final String ERR_AUTH_01 = "ERR_AUTH_01";

    /** 401 - Token valido, pero el usuario del token ya no existe en el sistema. */
    public static final String ERR_AUTH_02 = "ERR_AUTH_02";

    /** 403 - El usuario existe pero su cuenta esta desactivada. */
    public static final String ERR_AUTH_03 = "ERR_AUTH_03";

    /** 401 - Correo o contraseña incorrectos (mismo mensaje en ambos casos). */
    public static final String ERR_AUTH_04 = "ERR_AUTH_04";

    /** 403 - Cuenta bloqueada temporalmente por intentos fallidos de inicio de sesion. */
    public static final String ERR_AUTH_05 = "ERR_AUTH_05";

    /** 401 - Refresh token invalido, vencido o ya revocado. */
    public static final String ERR_AUTH_06 = "ERR_AUTH_06";

    private AuthErrorCodes() {
    }
}
