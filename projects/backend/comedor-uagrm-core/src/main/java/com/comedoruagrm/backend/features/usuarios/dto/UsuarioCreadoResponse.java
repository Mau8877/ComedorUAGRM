package com.comedoruagrm.backend.features.usuarios.dto;

import java.util.UUID;

/**
 * Respuesta del alta de una cuenta: la cuenta mas su contraseña temporal, que
 * se muestra **una unica vez** (no se vuelve a poder consultar; en la base solo
 * queda su hash). Se usa solo en esta respuesta.
 *
 * ! Nunca se loguea ni se guarda en la bitacora.
 */
public record UsuarioCreadoResponse(
        UUID codigo,
        String nombres,
        String apellidos,
        String ci,
        String correo,
        String rol,
        boolean activo,
        String contrasenaTemporal
) {

    public static UsuarioCreadoResponse de(UsuarioPersonalResponse cuenta, String contrasenaTemporal) {
        return new UsuarioCreadoResponse(cuenta.codigo(), cuenta.nombres(), cuenta.apellidos(), cuenta.ci(),
                cuenta.correo(), cuenta.rol(), cuenta.activo(), contrasenaTemporal);
    }
}
