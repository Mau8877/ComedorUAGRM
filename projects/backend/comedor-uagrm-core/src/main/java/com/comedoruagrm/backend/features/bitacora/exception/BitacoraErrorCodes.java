package com.comedoruagrm.backend.features.bitacora.exception;

/**
 * Catalogo de codigos de error del modulo bitacora (prefijo BIT). Documentados en
 * docs/errors/bitacora/ERRORES_BITACORA.md.
 */
public final class BitacoraErrorCodes {

    /** 400 - El campo de `sort` no esta soportado por la consulta de la bitacora. */
    public static final String ERR_BIT_01 = "ERR_BIT_01";

    /** 400 - El rango de fechas es invalido: `desde` es posterior a `hasta`. */
    public static final String ERR_BIT_02 = "ERR_BIT_02";

    private BitacoraErrorCodes() {
    }
}
