package com.comedoruagrm.backend.features.finalistas.dto;

/**
 * Resultado de notificar a los finalistas: `enviados` son los avisos registrados en la bandeja y
 * `fallidos` los que no se pudieron registrar. El push y el correo se despachan aparte y no se esperan.
 */
public record ResultadoNotificacionResponse(int solicitados, int enviados, int fallidos) {
}
