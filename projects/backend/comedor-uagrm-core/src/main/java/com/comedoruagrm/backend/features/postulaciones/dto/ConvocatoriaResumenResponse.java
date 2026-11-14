package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.UUID;

/** La convocatoria de una postulacion, para «Mis postulaciones» (CU04). */
public record ConvocatoriaResumenResponse(UUID codigo, String nombre) {
}
