package com.comedoruagrm.backend.common.auditoria;

import java.util.Map;
import java.util.UUID;

/**
 * Resultado de un metodo anotado con {@link Auditar}: le dice a la bitacora
 * quien hizo la accion y sobre que. Vive en `common/` (no en `bitacora/`) para
 * que los modulos de negocio lo usen sin depender del modulo de bitacora.
 */
public interface Auditable {

    /** `id` interno del usuario que ejecuta la accion (FK `bitacora.usuario_id`). */
    Long actorId();

    /** `codigo` publico del mismo usuario; solo se usa para el log del evento. */
    UUID actorCodigo();

    /** `id` de la fila afectada; por defecto, la del propio actor. */
    default Long entidadId() {
        return actorId();
    }

    /** Datos extra del evento (`bitacora.detalle`). Nunca contraseñas ni tokens. */
    default Map<String, Object> detalle() {
        return Map.of();
    }

    /** Devuelve false cuando esta vez no hay nada que anotar (ej. un registro repetido). */
    default boolean auditar() {
        return true;
    }
}
