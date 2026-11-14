package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Una devolucion registrada (CU19). `postulacion` es su codigo publico; `devueltaPor` es el nombre
 * completo del administrador.
 */
public record DevolucionResponse(
        UUID postulacion,
        String estado,
        String motivo,
        Instant fecha,
        String devueltaPor) {
}
