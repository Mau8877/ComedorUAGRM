package com.comedoruagrm.backend.features.postulaciones.dto;

/**
 * Cuantas postulaciones de la convocatoria estan en la lista (calificadas, con puntaje vigente) y
 * cuantas siguen pendientes de precalificacion.
 */
public record ResumenListaPriorizadaResponse(long totalCalificadas, long totalPendientes) {
}
