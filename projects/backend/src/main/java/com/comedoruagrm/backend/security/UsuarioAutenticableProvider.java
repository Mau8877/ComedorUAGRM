package com.comedoruagrm.backend.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que `security/` necesita saber de un usuario del sistema. La define
 * `security/` y la implementa `features/usuarios/`, porque `security/` no
 * puede importar nada de `features/` (regla de dependencia).
 */
public interface UsuarioAutenticableProvider {

    Optional<DatosUsuario> buscarPorFirebaseUid(String firebaseUid);

    record DatosUsuario(UUID codigo, String rol, boolean activo) {
    }
}
