package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.UUID;

/** Carrera del postulante, tal como se muestra en la lista priorizada. */
public record CarreraPriorizadaResponse(UUID codigo, String nombre) {
}
