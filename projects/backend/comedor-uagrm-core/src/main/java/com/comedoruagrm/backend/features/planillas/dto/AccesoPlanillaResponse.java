package com.comedoruagrm.backend.features.planillas.dto;

import java.time.Instant;

/** Direccion temporal y firmada para abrir el PDF de la planilla (CU09); vence en `expiraEn`. */
public record AccesoPlanillaResponse(String url, Instant expiraEn) {
}
