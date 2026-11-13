package com.comedoruagrm.backend.features.resultados.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * El resultado propio del postulante (CU22). `resultado` es `ADJUDICADA`, `NO_ADJUDICADA` o nulo si el usuario
 * no es finalista con decision en esa convocatoria. Nunca lleva el motivo de «no adjudicada».
 */
public record MiResultadoResponse(String resultado, BigDecimal nota, Instant publicadoEn) {
}
