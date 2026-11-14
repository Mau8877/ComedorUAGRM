package com.comedoruagrm.backend.features.postulaciones.enums;

/**
 * Resultado de la decision final sobre un finalista (CU05); en la base es el enum de PostgreSQL
 * `resultado_adjudicacion`.
 *
 * * `ADJUDICADA` entrega la beca; `NO_ADJUDICADA` es la decision final de no entregarla (distinta del
 * * rechazo del CU20, que no deja fila en `adjudicacion`).
 */
public enum ResultadoAdjudicacion {
    ADJUDICADA,
    NO_ADJUDICADA
}
