package com.comedoruagrm.backend.features.postulaciones.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una fila de la lista priorizada (CU06). `posicion` es el lugar en la lista completa de la
 * convocatoria (no en la pagina ni en el resultado de un filtro). Nunca lleva datos
 * socioeconomicos.
 */
public record PostulacionPriorizadaResponse(
        UUID codigo,
        long posicion,
        String nombres,
        String apellidos,
        String ci,
        CarreraPriorizadaResponse carrera,
        BigDecimal puntaje,
        Instant fechaCalculo,
        String estado) {
}
