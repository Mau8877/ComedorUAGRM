package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * «Mi borrador» (CU17): el borrador de la convocatoria abierta, con lo que falta para poder enviarlo.
 * `guardadoEn` es nulo si todavia no se guardo ningun avance del formulario.
 */
public record BorradorResumenResponse(
        UUID postulacion,
        String convocatoria,
        String gestion,
        Instant fechaCierre,
        Instant guardadoEn,
        int camposPendientes,
        int documentosPendientes) {
}
