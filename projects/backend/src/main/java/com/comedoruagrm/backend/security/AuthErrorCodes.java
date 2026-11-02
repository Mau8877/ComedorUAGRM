package com.comedoruagrm.backend.security;

/**
 * Catalogo de codigos de error de autenticacion (modulo AUTH). Documentados
 * en docs/errors/auth/ERRORES_AUTH.md.
 */
public final class AuthErrorCodes {

    /** 401 - Token ausente, mal firmado, de otro proyecto o vencido. */
    public static final String ERR_AUTH_01 = "ERR_AUTH_01";

    /** 401 - Token valido, pero no hay un usuario del sistema con ese firebase_uid. */
    public static final String ERR_AUTH_02 = "ERR_AUTH_02";

    /** 403 - El usuario existe pero su cuenta esta desactivada. */
    public static final String ERR_AUTH_03 = "ERR_AUTH_03";

    private AuthErrorCodes() {
    }
}
