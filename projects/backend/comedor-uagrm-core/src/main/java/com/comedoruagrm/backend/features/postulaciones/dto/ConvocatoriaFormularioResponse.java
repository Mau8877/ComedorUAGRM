package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/** La convocatoria a la que se postula. */
public record ConvocatoriaFormularioResponse(UUID codigo, String nombre, String gestion, Instant fechaCierre) {
}
