package com.comedoruagrm.backend.features.actas.dto;

import java.util.UUID;

/** La convocatoria a la que pertenece un acta (CU08). */
public record ConvocatoriaActaResponse(UUID codigo, String nombre) {
}
