package com.comedoruagrm.backend.features.convocatorias.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Un documento de la lista de requeridos de una convocatoria: el tipo y si es obligatorio. */
public record DocumentoRequeridoRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        UUID tipoDocumentoCodigo,

        @NotNull(message = "Indica si el documento es obligatorio")
        Boolean obligatorio) {
}
