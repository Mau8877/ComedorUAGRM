package com.comedoruagrm.backend.features.resultados.dto;

import java.time.Instant;

/** Resultado de publicar (CU22): cuando, quien y a cuantos finalistas se avisa. */
public record PublicacionResultadoResponse(
        ConvocatoriaResultadosResponse convocatoria,
        Instant publicadoEn,
        String publicadoPor,
        long adjudicados,
        long noAdjudicados,
        long avisados) {
}
