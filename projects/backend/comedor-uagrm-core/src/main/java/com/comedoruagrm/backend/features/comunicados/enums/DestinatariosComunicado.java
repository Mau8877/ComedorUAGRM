package com.comedoruagrm.backend.features.comunicados.enums;

/**
 * A quien va dirigido un comunicado; en la base es el enum de PostgreSQL `destinatarios_comunicado`.
 *
 * * `ADJUDICADOS` (finalistas con adjudicacion `ADJUDICADA` de una convocatoria) se habilito con el CU22;
 * * como `FINALISTAS`, necesita la convocatoria.
 */
public enum DestinatariosComunicado {
    TODOS,
    FINALISTAS,
    ADJUDICADOS
}
