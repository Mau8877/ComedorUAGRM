package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Un cierre registrado (CU20). `postulacion` es su codigo publico; `estado` es `archivada` o
 * `no_adjudicada`; `decididaPor` es el nombre completo del administrador.
 */
public record CierrePostulacionResponse(
        UUID postulacion,
        String estado,
        String motivo,
        Instant fecha,
        String decididaPor) {
}
