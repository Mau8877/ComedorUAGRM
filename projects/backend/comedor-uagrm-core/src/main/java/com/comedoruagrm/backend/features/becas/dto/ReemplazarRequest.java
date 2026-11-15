package com.comedoruagrm.backend.features.becas.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Confirma el reemplazo con el postulante que el Administrador vio como candidato (CU23, paso 14). */
public record ReemplazarRequest(@NotNull(message = "El postulante es obligatorio") UUID postulanteCodigo) {
}
