package com.comedoruagrm.backend.features.actas.dto;

import java.time.Instant;

/** Direccion temporal y firmada para abrir el PDF del acta (CU08); vence en `expiraEn`. */
public record AccesoActaResponse(String url, Instant expiraEn) {
}
