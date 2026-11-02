package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class FirebaseJwtConfigTest {

    private static final String PROJECT_ID = "comedor-uagrm";

    private final OAuth2TokenValidator<Jwt> validador = FirebaseJwtConfig.validador(PROJECT_ID);

    @Test
    void validar_conUnTokenDelProyectoVigente_noTieneErrores() {
        assertThat(validador.validate(token("https://securetoken.google.com/comedor-uagrm", PROJECT_ID, 3600)).hasErrors())
                .isFalse();
    }

    @Test
    void validar_conEmisorDeOtroProyecto_tieneErrores() {
        assertThat(validador.validate(token("https://securetoken.google.com/otro-proyecto", PROJECT_ID, 3600)).hasErrors())
                .isTrue();
    }

    @Test
    void validar_conAudienciaDeOtroProyecto_tieneErrores() {
        assertThat(validador.validate(token("https://securetoken.google.com/comedor-uagrm", "otro-proyecto", 3600)).hasErrors())
                .isTrue();
    }

    @Test
    void validar_conTokenVencido_tieneErrores() {
        assertThat(validador.validate(token("https://securetoken.google.com/comedor-uagrm", PROJECT_ID, -3600)).hasErrors())
                .isTrue();
    }

    @Test
    void jwtDecoder_sinProjectId_fallaConUnMensajeQueNombraLaVariable() {
        FirebaseJwtConfig config = new FirebaseJwtConfig();

        assertThatThrownBy(() -> config.jwtDecoder(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIREBASE_PROJECT_ID");
    }

    private Jwt token(String emisor, String audiencia, long segundosHastaExpirar) {
        Instant ahora = Instant.now();
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .subject("uid-123")
                .issuer(emisor)
                .audience(List.of(audiencia))
                .issuedAt(ahora.minusSeconds(7200))
                .expiresAt(ahora.plusSeconds(segundosHastaExpirar))
                .build();
    }
}
