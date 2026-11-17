package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;

/** Direccion temporal firmada para abrir un documento (CU16). Nunca una ruta permanente. */
public record AccesoDocumentoResponse(String url, Instant expiraEn) {
}
