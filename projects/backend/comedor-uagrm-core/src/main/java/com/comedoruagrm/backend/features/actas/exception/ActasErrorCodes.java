package com.comedoruagrm.backend.features.actas.exception;

/**
 * Codigos de error del acta de adjudicacion (CU08), prefijo ACT. Catalogo vivo en
 * docs/errors/actas/ERRORES_ACTAS.md. Una convocatoria inexistente reutiliza ERR_PRI_01 y un `sort` o
 * `filter` no soportado, ERR_SYS_05.
 */
public final class ActasErrorCodes {

    /** 409 - Los resultados de la convocatoria todavia no estan publicados. */
    public static final String ERR_ACT_01 = "ERR_ACT_01";

    /** 409 - La convocatoria ya tiene acta. */
    public static final String ERR_ACT_02 = "ERR_ACT_02";

    /** 404 - La convocatoria todavia no tiene acta. */
    public static final String ERR_ACT_03 = "ERR_ACT_03";

    /** 404 - El archivo del acta no esta en el almacenamiento. */
    public static final String ERR_ACT_04 = "ERR_ACT_04";

    private ActasErrorCodes() {
    }
}
