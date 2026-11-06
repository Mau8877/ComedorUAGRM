package com.comedoruagrm.backend.features.postulaciones.enums;

/**
 * Valores del tipo `estado_postulacion` de PostgreSQL (en minuscula, como en el diseño logico:
 * el nombre de cada constante es el valor guardado).
 */
public enum EstadoPostulacion {
    borrador,
    pendiente,
    devuelta,
    archivada,
    calificada,
    adjudicada,
    no_adjudicada
}
