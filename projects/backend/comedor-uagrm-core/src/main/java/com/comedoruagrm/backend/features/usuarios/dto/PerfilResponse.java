package com.comedoruagrm.backend.features.usuarios.dto;

import java.util.UUID;

/**
 * Perfil del usuario autenticado (CU15). `postulante` es null si la cuenta no es de un
 * Postulante. Nunca lleva la contraseña ni sus hashes.
 */
public record PerfilResponse(
        UUID codigo,
        String nombres,
        String apellidos,
        String ci,
        String correo,
        String rol,
        boolean activo,
        PerfilPostulanteResponse postulante) {
}
