package com.comedoruagrm.backend.features.comunicados.enums;

/**
 * A quien va dirigido un comunicado; en la base es el enum de PostgreSQL `destinatarios_comunicado`.
 *
 * * Hoy se publican `TODOS` y `FINALISTAS`; `ADJUDICADOS` existe en el diseno logico pero se habilita
 * * cuando existan las adjudicaciones (CU05 / CU22).
 */
public enum DestinatariosComunicado {
    TODOS,
    FINALISTAS,
    ADJUDICADOS
}
