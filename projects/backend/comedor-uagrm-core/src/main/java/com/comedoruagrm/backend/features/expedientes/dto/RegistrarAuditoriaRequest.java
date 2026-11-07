package com.comedoruagrm.backend.features.expedientes.dto;

import java.math.BigDecimal;

import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro de la auditoria del expediente de un finalista (CU07). Las observaciones son obligatorias si el
 * resultado es OBSERVADO o NO_VALIDO y el puntaje academico (0 a 30) si es VALIDADO: esas dos reglas las
 * valida el `service` con su codigo ERR_EXP_*.
 */
public record RegistrarAuditoriaRequest(
        @NotNull(message = "El resultado de la auditoría es obligatorio") ResultadoAuditoria resultado,
        @Size(max = 500, message = "Las observaciones no pueden pasar de 500 caracteres") String observaciones,
        BigDecimal puntajeAcademico) {
}
