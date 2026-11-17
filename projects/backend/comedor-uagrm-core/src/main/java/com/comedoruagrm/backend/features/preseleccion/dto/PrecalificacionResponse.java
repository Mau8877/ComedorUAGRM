package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * La precalificacion con IA de una postulacion, para el Administrador DUBSS (CU03). Si la postulacion todavia no se
 * califico, `puntaje`, `modelo`, `fechaCalculo`, `posicion` y `variables` van vacios. No lleva respuestas
 * socioeconomicas, solo el aporte de cada variable.
 *
 * * `posicion` es el lugar en la lista priorizada de la convocatoria (CU06); solo existe si esta calificada.
 * * `marcadaParaRevision` es la marca del CU03 A1: la IA rechazo los datos `intentos` veces y el reintento
 * * automatico dejo de tomarla.
 */
public record PrecalificacionResponse(UUID postulacion, String nombres, String apellidos, String ci,
        String convocatoria, String estado, BigDecimal puntaje, BigDecimal puntajeMaximo, String modelo,
        Instant fechaCalculo, Long posicion, List<AporteVariableResponse> variables, List<String> advertencias,
        boolean marcadaParaRevision, Instant revisionDesde, int intentos, List<PasoTrazaResponse> traza) {
}
