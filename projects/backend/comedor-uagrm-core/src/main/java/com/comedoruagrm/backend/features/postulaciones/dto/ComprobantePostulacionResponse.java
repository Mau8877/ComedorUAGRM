package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * El comprobante del envio (CU02, paso 10). `codigo` es el codigo publico de la postulacion; `reenvio` es
 * verdadero cuando es el reenvio de una devuelta (A5).
 */
public record ComprobantePostulacionResponse(
        UUID codigo,
        String estado,
        Instant fechaEnvio,
        int numeroEnvios,
        boolean reenvio,
        String convocatoria,
        String gestion) {
}
