package com.comedoruagrm.backend.features.preseleccion.dto;

import java.time.Instant;

/** Un paso del proceso automatico de una postulacion, tal como quedo en su historial de estados. */
public record PasoTrazaResponse(String paso, String detalle, Instant fecha) {
}
