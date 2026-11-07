package com.comedoruagrm.backend.features.expedientes.dto;

import java.time.Instant;

/** Direccion temporal para abrir un documento digital (CU07): deja de servir en `expiraEn`. */
public record AccesoDocumentoResponse(String url, Instant expiraEn) {
}
