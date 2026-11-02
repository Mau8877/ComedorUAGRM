package com.comedoruagrm.backend.security;

import java.util.UUID;

/** Principal de una peticion hecha por un usuario registrado y activo. */
public record UsuarioAutenticado(UUID codigo, String rol, String firebaseUid) {
}
