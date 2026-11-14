package com.comedoruagrm.backend.features.resultados.dto;

import java.time.Instant;

/**
 * Lo que el Administrador revisa antes de publicar (CU22). `puedePublicar` es verdadero solo si la convocatoria
 * esta cerrada, no esta publicada y no quedan finalistas en «calificada»; no exige que haya adjudicados.
 * `aAvisar` es cuantos finalistas recibiran aviso. `publicadoEn` y `publicadoPor` son nulos si no se publico.
 */
public record ResumenPublicacionResponse(
        ConvocatoriaResultadosResponse convocatoria,
        long adjudicados,
        long noAdjudicados,
        long calificadasPendientes,
        long totalFinalistas,
        long aAvisar,
        boolean yaPublicada,
        Instant publicadoEn,
        String publicadoPor,
        boolean puedePublicar) {
}
