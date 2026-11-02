package com.comedoruagrm.backend.features.usuarios;

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

    private UsuarioErrorCodes() {
    }
}
