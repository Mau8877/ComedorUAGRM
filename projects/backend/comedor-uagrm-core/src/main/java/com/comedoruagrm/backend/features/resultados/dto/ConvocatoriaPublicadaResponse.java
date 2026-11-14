package com.comedoruagrm.backend.features.resultados.dto;

import java.time.Instant;
import java.util.UUID;

/** Una convocatoria con resultados publicados, para el listado del postulante (CU22). */
public record ConvocatoriaPublicadaResponse(UUID codigo, String nombre, Instant publicadoEn) {
}
