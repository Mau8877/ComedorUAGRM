package com.comedoruagrm.backend.features.resultados.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una decision de adjudicacion para la pantalla del Administrador (CU22): sin CI, correo ni motivo.
 * `resultado` es `ADJUDICADA` o `NO_ADJUDICADA`; `puntajeFinal` puede ser nulo (ver CU05).
 */
public record DecisionResultadoResponse(
        UUID finalista,
        String nombres,
        String apellidos,
        String resultado,
        BigDecimal puntajeFinal,
        Instant fechaAdjudicacion) {
}
