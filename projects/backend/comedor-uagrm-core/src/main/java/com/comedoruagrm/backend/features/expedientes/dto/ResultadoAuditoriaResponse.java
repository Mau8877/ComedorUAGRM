package com.comedoruagrm.backend.features.expedientes.dto;

/**
 * Lo que devuelve el registro de una auditoria: la auditoria y si se creo ahora (`creada = true`) o era
 * una repeticion de la ultima y se devolvio esa (idempotencia, RNF-03). El controller responde 201 o 200.
 */
public record ResultadoAuditoriaResponse(AuditoriaResponse auditoria, boolean creada) {
}
