package com.comedoruagrm.backend.features.postulaciones.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una decision registrada (CU05). `finalista` y `postulacion` son codigos publicos; `resultado` es
 * `ADJUDICADA` o `NO_ADJUDICADA`; `estado` es `adjudicada` o `no_adjudicada`; `decididaPor` es el nombre
 * completo del administrador y `cuposDisponibles` ya viene actualizado (nulo si no hay cupos definidos).
 */
public record DecisionAdjudicacionResponse(
        UUID finalista,
        UUID postulacion,
        String resultado,
        String estado,
        BigDecimal puntajeFinal,
        Instant fechaAdjudicacion,
        String decididaPor,
        Integer cuposDisponibles) {
}
