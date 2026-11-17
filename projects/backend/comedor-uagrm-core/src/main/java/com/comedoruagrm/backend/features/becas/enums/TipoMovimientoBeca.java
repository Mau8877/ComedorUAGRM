package com.comedoruagrm.backend.features.becas.enums;

/**
 * Tipo de un movimiento del padron (CU23); en la base es el enum de PostgreSQL `tipo_movimiento_beca`.
 *
 * * `REACTIVACION` existe en el modelo de datos, pero el flujo del CU23 todavia no la usa.
 */
public enum TipoMovimientoBeca {
    BAJA,
    SUSPENSION,
    REACTIVACION,
    REEMPLAZO
}
