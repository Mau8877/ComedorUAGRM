package com.comedoruagrm.backend.features.convocatorias.enums;

/** Valores del tipo `estado_convocatoria` de PostgreSQL. */
public enum EstadoConvocatoria {
    BORRADOR,
    ABIERTA,
    CERRADA,
    /** Los resultados de la adjudicacion ya se publicaron (CU22). */
    PUBLICADA
}
