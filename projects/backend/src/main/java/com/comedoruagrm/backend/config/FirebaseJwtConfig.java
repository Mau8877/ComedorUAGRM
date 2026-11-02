package com.comedoruagrm.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Verificacion del ID token de Firebase: firma (con las claves publicas de
 * Google), emisor, audiencia y expiracion. El backend nunca ve la contraseña
 * ni emite tokens propios (ver SEGURIDAD_AUTH_BACKEND.md).
 *
 * * Se usa el resource server de Spring Security y no el Admin SDK de
 * * Firebase porque para verificar no hace falta una credencial de servicio.
 * * Las claves se descargan de forma perezosa en la primera verificacion
 * * (y se cachean), asi que el arranque no depende de internet.
 */
@Configuration
public class FirebaseJwtConfig {

    static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.firebase.project-id:}") String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "Falta app.firebase.project-id: defini la variable de entorno FIREBASE_PROJECT_ID "
                            + "con el projectId del proyecto de Firebase.");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        decoder.setJwtValidator(validador(projectId));
        return decoder;
    }

    /** Emisor `https://securetoken.google.com/{projectId}`, audiencia = projectId y expiracion. */
    static OAuth2TokenValidator<Jwt> validador(String projectId) {
        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(projectId));
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId),
                audiencia);
    }
}
