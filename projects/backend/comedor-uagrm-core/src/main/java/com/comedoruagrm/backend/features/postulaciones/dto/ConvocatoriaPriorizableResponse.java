package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/** Convocatoria que se puede consultar en la lista priorizada (CU06). */
public record ConvocatoriaPriorizableResponse(
        UUID codigo,
        String nombre,
        String gestion,
        String estado,
        Instant fechaApertura,
        Instant fechaCierre) {
}
