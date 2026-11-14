package com.comedoruagrm.backend.features.expedientes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;

/** Una auditoria del expediente (CU07). `auditor` es el nombre completo de quien la registro. */
public record AuditoriaResponse(
        UUID codigo,
        ResultadoAuditoria resultado,
        String observaciones,
        BigDecimal puntajeAcademico,
        Instant fecha,
        String auditor) {
}
