package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;

import com.comedoruagrm.backend.features.postulaciones.enums.SituacionPaso;

/**
 * Un paso de la linea de tiempo de una postulacion (CU04). Los pasos `COMPLETADO` salen del historial real (con
 * su fecha); los `PENDIENTE` son los que aun faltan y no llevan fecha. Nunca lleva quien hizo el cambio.
 */
public record PasoLineaTiempoResponse(String estado, Instant fecha, SituacionPaso situacion) {
}
