package com.comedoruagrm.backend.features.finalistas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una fila de la pantalla de finalistas (CU21). `estadoNotificacion` es SIN_ENVIAR, ENVIADA o FALLIDA
 * (derivado del ultimo aviso). `plazoEntrega` y `documentacionRecibidaEn` son fechas de dia; la entrega
 * esta "Recibida" si `documentacionRecibidaEn` tiene valor. Nunca lleva ids internos.
 *
 * * `puntaje` es el de la IA (0 a 70). `resultadoAuditoria` (CU07) es SIN_AUDITAR, VALIDADO, OBSERVADO o
 * * NO_VALIDO, el de la ultima auditoria; `puntajeTotal` = IA + puntaje academico de esa auditoria, null si
 * * falta alguno de los dos.
 */
public record FinalistaResponse(
        UUID codigo,
        int posicion,
        String nombres,
        String apellidos,
        String ci,
        BigDecimal puntaje,
        String estadoNotificacion,
        LocalDate plazoEntrega,
        Instant notificadoEn,
        LocalDate documentacionRecibidaEn,
        String resultadoAuditoria,
        BigDecimal puntajeTotal) {
}
