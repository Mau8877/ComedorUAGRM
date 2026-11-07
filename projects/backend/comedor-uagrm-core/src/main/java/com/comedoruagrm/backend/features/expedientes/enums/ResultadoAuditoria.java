package com.comedoruagrm.backend.features.expedientes.enums;

/**
 * Resultado del cotejo del expediente de un finalista (CU07); en la base es el enum de PostgreSQL
 * `resultado_auditoria`.
 *
 * * `VALIDADO` deja al finalista habilitado para la adjudicacion (CU05); `OBSERVADO` es un expediente
 * * con defectos subsanables y `NO_VALIDO` uno que no procede. Este modulo no cambia el estado de la
 * * postulacion en ningun caso.
 */
public enum ResultadoAuditoria {
    VALIDADO,
    OBSERVADO,
    NO_VALIDO
}
