package com.comedoruagrm.backend.features.becas.enums;

/**
 * Estado de una beca (CU23); en la base es el enum de PostgreSQL `estado_beca`.
 *
 * * `VIGENTE` forma parte del padron (y de las planillas de asistencia); `SUSPENDIDA` y `BAJA` no.
 */
public enum EstadoBeca {
    VIGENTE,
    SUSPENDIDA,
    BAJA
}
