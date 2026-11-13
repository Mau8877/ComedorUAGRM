package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;

/** Las observaciones de una devolucion (CU19) y cuando se hicieron, para la postulacion «devuelta» (CU04). */
public record ObservacionesResponse(String texto, Instant fecha) {
}
