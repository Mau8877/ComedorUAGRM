package com.comedoruagrm.backend.common.auditoria;

import java.util.Map;
import java.util.UUID;

/**
 * Resultado de un metodo de `service` anotado con {@link Auditar} cuando el
 * controller necesita un DTO: lleva el DTO (`datos`) y, aparte, lo que el
 * aspecto de la bitacora necesita (actor, entidad afectada, detalle).
 *
 * * Solo tiene ids, codigos y datos simples: nunca entidades JPA, asi el
 * * controller (que solo conoce DTOs) puede recibirlo y llamar a `datos()`.
 * * `actorId` es quien hace la accion (ej. el administrador) y `entidadId` la
 * * fila afectada (ej. la cuenta que se desactiva); pueden ser distintos.
 * ! `detalle` nunca lleva contraseñas, tokens ni sus hashes.
 */
public record ResultadoAuditado<T>(
        T datos,
        Long actorId,
        UUID actorCodigo,
        Long entidadId,
        Map<String, Object> detalle,
        boolean auditar) implements Auditable {
}
