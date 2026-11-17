package com.comedoruagrm.backend.features.cotejos.enums;

/**
 * Resultado de un cotejo (CU11); en la base es un `varchar` con un CHECK.
 *
 * * `CONFORME`: no hay discrepancias. `CON_DISCREPANCIAS`: hay al menos una y las observaciones son obligatorias.
 */
public enum ResultadoCotejo {
    CONFORME,
    CON_DISCREPANCIAS
}
