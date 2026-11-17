package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * La respuesta del modulo de IA: el puntaje (0 a 70, el tope de la IA), la version del modelo que lo calculo (`modelo`, igual a
 * `modelo_preseleccion.version`) y, si el modelo lo explica, el detalle por variable.
 */
public record PuntajeCalculado(BigDecimal puntaje, String modelo, Map<String, Object> detalle) {
}
