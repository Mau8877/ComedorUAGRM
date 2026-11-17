package com.comedoruagrm.backend.features.bitacora.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;

/**
 * Una fila de la bitacora (CU25). Solo lectura.
 *
 * * `usuario` es null cuando lo hizo el sistema (ej. el cierre automatico de una convocatoria).
 * ! No lleva `entidadId`: es el id interno de la fila afectada y nunca sale de la API; la entidad afectada se
 * ! identifica por `entidad` y, cuando aplica, por los codigos publicos que trae `detalle`.
 */
public record BitacoraResponse(UUID codigo, Instant fecha, ActorResponse usuario, String accion, String entidad,
        Map<String, Object> detalle, String ip) {

    public record ActorResponse(UUID codigo, String nombres, String apellidos, String correo) {
    }

    public static BitacoraResponse de(Bitacora fila) {
        Usuario u = fila.getUsuario();
        ActorResponse actor = u == null ? null
                : new ActorResponse(u.getCodigo(), u.getNombres(), u.getApellidos(), u.getCorreo());
        return new BitacoraResponse(fila.getCodigo(), fila.getCreadoEn(), actor, fila.getAccion(),
                fila.getEntidad(), fila.getDetalle(), fila.getIp() == null ? null : fila.getIp().getHostAddress());
    }
}
