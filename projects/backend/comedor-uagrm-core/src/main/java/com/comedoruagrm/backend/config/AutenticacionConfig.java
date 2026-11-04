package com.comedoruagrm.backend.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * JWT propio del backend (HS256) y hash de contraseñas (BCrypt). El backend
 * firma y verifica sus tokens de acceso con el mismo secreto (`JWT_SECRET`).
 *
 * * El secreto es obligatorio y de al menos 32 bytes (256 bits, el minimo de
 * * HS256): si falta o es corto el arranque falla en vez de firmar con una
 * * clave debil.
 * ! El secreto nunca se loguea ni se versiona (solo vive en el .env).
 */
@Configuration
public class AutenticacionConfig {

    static final int SECRETO_MIN_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret:}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < SECRETO_MIN_BYTES) {
            throw new IllegalStateException(
                    "Falta app.jwt.secret o es demasiado corto: defini la variable de entorno JWT_SECRET "
                            + "con al menos 32 caracteres.");
        }
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /** Firma (HS256), emisor y expiracion. */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, @Value("${app.jwt.issuer}") String emisor) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
