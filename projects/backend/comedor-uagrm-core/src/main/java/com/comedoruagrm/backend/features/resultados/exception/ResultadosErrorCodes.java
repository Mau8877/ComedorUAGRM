package com.comedoruagrm.backend.features.resultados.exception;

/**
 * Codigos de error de la publicacion de resultados (CU22), prefijo PUB. Catalogo vivo en
 * docs/errors/resultados/ERRORES_RESULTADOS.md. Una convocatoria inexistente reutiliza ERR_PRI_01 y un
 * `sort` o `filter` no soportado, ERR_SYS_05.
 */
public final class ResultadosErrorCodes {

    /** 409 - Quedan finalistas en «calificada»: faltan decisiones de adjudicacion. */
    public static final String ERR_PUB_01 = "ERR_PUB_01";

    /** 409 - Los resultados de la convocatoria ya estaban publicados. */
    public static final String ERR_PUB_02 = "ERR_PUB_02";

    /** 409 - La convocatoria no esta cerrada. */
    public static final String ERR_PUB_03 = "ERR_PUB_03";

    /** 404 - Los resultados de la convocatoria todavia no se publicaron. */
    public static final String ERR_PUB_04 = "ERR_PUB_04";

    private ResultadosErrorCodes() {
    }
}
