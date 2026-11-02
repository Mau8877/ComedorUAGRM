package com.comedoruagrm.backend.security;

/**
 * Principal de una peticion con un token valido de Firebase pero sin usuario
 * en el sistema: solo se acepta en el registro. `correo` sale del token.
 */
public record FirebaseSinRegistro(String firebaseUid, String correo) {
}
