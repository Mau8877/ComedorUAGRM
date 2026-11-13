package com.comedoruagrm.backend.features.actas.dto;

import java.time.Instant;
import java.util.UUID;

/** Una convocatoria con resultados publicados y sin acta, lista para generarla (CU08). */
public record ConvocatoriaPendienteActaResponse(UUID codigo, String nombre, Instant publicadoEn, long adjudicados) {
}
