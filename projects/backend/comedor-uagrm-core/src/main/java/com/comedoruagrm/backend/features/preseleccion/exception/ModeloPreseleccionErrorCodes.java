package com.comedoruagrm.backend.features.preseleccion.exception;

/**
 * Codigos de error de la parametrizacion del modelo y el reprocesamiento de puntajes (CU26) y su consulta de precalificacion (CU03), prefijo MOD.
 * Catalogo vivo en docs/errors/preseleccion/ERRORES_PRESELECCION.md. Una convocatoria inexistente reutiliza ERR_CV_01.
 */
public final class ModeloPreseleccionErrorCodes {

    /** 400 - Los parametros no son validos: falta una variable, hay una repetida o desconocida, una ponderacion fuera de rango o no suman 100. */
    public static final String ERR_MOD_01 = "ERR_MOD_01";

    /** 409 - No hay una version vigente del modelo: primero se guardan los parametros. */
    public static final String ERR_MOD_02 = "ERR_MOD_02";

    /** 409 - Ya hay un reprocesamiento en curso para esa convocatoria. */
    public static final String ERR_MOD_03 = "ERR_MOD_03";

    /** 409 - Los resultados de la convocatoria ya fueron publicados: no se reprocesa. */
    public static final String ERR_MOD_04 = "ERR_MOD_04";

    /** 404 - El lote de reprocesamiento no existe. */
    public static final String ERR_MOD_05 = "ERR_MOD_05";

    /** 409 - No hay postulaciones calificadas por reprocesar con la version vigente. */
    public static final String ERR_MOD_06 = "ERR_MOD_06";

    /** 409 - Otra version se guardo al mismo tiempo: hay que volver a abrir los parametros. */
    public static final String ERR_MOD_07 = "ERR_MOD_07";

    /** 404 - La postulacion cuya precalificacion se consulta no existe (CU03). */
    public static final String ERR_MOD_08 = "ERR_MOD_08";

    private ModeloPreseleccionErrorCodes() {
    }
}
