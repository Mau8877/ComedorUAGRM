package com.comedoruagrm.backend.features.usuarios.dto;

import java.util.UUID;

/**
 * Una cuenta del personal para la gestion de cuentas (CU14). `rol` es el nombre
 * del rol y `codigo` el identificador publico. Nunca lleva el `id` interno, el
 * hash de la contraseña ni los datos del bloqueo.
 */
public record UsuarioPersonalResponse(
        UUID codigo,
        String nombres,
        String apellidos,
        String ci,
        String correo,
        String rol,
        boolean activo
) {
}
