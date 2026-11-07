package com.comedoruagrm.backend.features.finalistas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una fila de la pantalla de finalistas (CU21). `estadoNotificacion` es SIN_ENVIAR, ENVIADA o FALLIDA
 * (derivado del ultimo aviso). `plazoEntrega` y `documentacionRecibidaEn` son fechas de dia; la entrega
 * esta "Recibida" si `documentacionRecibidaEn` tiene valor. Nunca lleva ids internos.
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
        LocalDate documentacionRecibidaEn) {
}
