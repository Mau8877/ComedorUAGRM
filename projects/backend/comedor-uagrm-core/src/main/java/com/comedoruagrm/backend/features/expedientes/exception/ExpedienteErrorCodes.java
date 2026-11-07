package com.comedoruagrm.backend.features.expedientes.exception;

/**
 * Codigos de error del expediente y su auditoria (CU07), prefijo EXP. Catalogo vivo en
 * docs/errors/expedientes/ERRORES_EXPEDIENTES.md. Un finalista inexistente reutiliza ERR_FIN_01 y los
 * campos vacios o de mas de 500 caracteres caen en ERR_SYS_01.
 */
public final class ExpedienteErrorCodes {

    /** 409 - La entrega de la documentacion fisica todavia no esta registrada. */
    public static final String ERR_EXP_01 = "ERR_EXP_01";

    /** 409 - La postulacion ya no esta «calificada». */
    public static final String ERR_EXP_02 = "ERR_EXP_02";

    /** 400 - Las observaciones son obligatorias si el resultado es «Observado» o «No valido». */
    public static final String ERR_EXP_03 = "ERR_EXP_03";

    /** 400 - El puntaje academico es obligatorio si el resultado es «Validado», o no esta entre 0 y 30. */
    public static final String ERR_EXP_04 = "ERR_EXP_04";

    /** 404 - El documento no existe o no pertenece al finalista. */
    public static final String ERR_EXP_05 = "ERR_EXP_05";

    /** 409 - La clave de idempotencia ya se uso para una auditoria distinta de este finalista. */
    public static final String ERR_EXP_06 = "ERR_EXP_06";

    /** 400 - El header `Idempotency-Key` no es un UUID valido. */
    public static final String ERR_EXP_07 = "ERR_EXP_07";

    private ExpedienteErrorCodes() {
    }
}
