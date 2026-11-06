package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Filtros de la lista que se exporta a PDF: los mismos de la pantalla (CU06). */
public record ExportarListaRequest(
        @NotNull(message = "La convocatoria es obligatoria") UUID convocatoriaId,
        UUID carreraId,
        String search) {
}
