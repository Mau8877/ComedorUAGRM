package com.comedoruagrm.backend.features.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Token de un solo uso que viaja en el enlace del correo de recuperacion (CU15).
 *
 * * Es una cadena aleatoria de 256 bits en Base64 URL (apta para una URL). En la
 * * base solo se guarda su hash SHA-256: una filtracion de la tabla no sirve para
 * * restablecer ninguna contraseña.
 * ! Ni el token ni su hash se loguean.
 */
@Component
public class TokenEnlace {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Genera un token nuevo, imposible de adivinar. */
    public String generar() {
        byte[] aleatorio = new byte[32];
        RANDOM.nextBytes(aleatorio);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
    }

    /** SHA-256 del token en hexadecimal (64 caracteres): lo unico que se guarda. */
    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
