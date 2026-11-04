package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.comedoruagrm.backend.security.TokenService;
import com.comedoruagrm.backend.security.TokenService.TokenAcceso;

class AutenticacionConfigTest {

    private static final String SECRETO = "secreto-de-pruebas-solo-para-tests-0123456789";
    private static final String EMISOR = "comedor-uagrm";

    private final AutenticacionConfig config = new AutenticacionConfig();

    @Test
    void jwtSecretKey_sinSecreto_fallaConUnMensajeQueNombraLaVariable() {
        assertThatThrownBy(() -> config.jwtSecretKey(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void jwtSecretKey_conUnSecretoCorto_falla() {
        assertThatThrownBy(() -> config.jwtSecretKey("corto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void tokenEmitido_seDecodificaConElMismoSecretoYLlevaElCodigoDelUsuario() {
        UUID codigo = UUID.randomUUID();

        Jwt jwt = decoder(SECRETO, EMISOR).decode(tokenService(SECRETO, EMISOR, 15).emitirAcceso(codigo).token());

        assertThat(jwt.getSubject()).isEqualTo(codigo.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(EMISOR);
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void emitirAcceso_informaLaVidaDelTokenEnSegundos() {
        TokenAcceso acceso = tokenService(SECRETO, EMISOR, 15).emitirAcceso(UUID.randomUUID());

        assertThat(acceso.expiraEnSegundos()).isEqualTo(900);
    }

    @Test
    void decode_conUnTokenFirmadoConOtroSecreto_loRechaza() {
        String token = tokenService("otro-secreto-distinto-de-32-bytes-o-mas!!", EMISOR, 15)
                .emitirAcceso(UUID.randomUUID()).token();

        assertThatThrownBy(() -> decoder(SECRETO, EMISOR).decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void decode_conUnTokenDeOtroEmisor_loRechaza() {
        String token = tokenService(SECRETO, "otro-emisor", 15).emitirAcceso(UUID.randomUUID()).token();

        assertThatThrownBy(() -> decoder(SECRETO, EMISOR).decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void decode_conUnTokenVencido_loRechaza() {
        Instant hace = Instant.now().minusSeconds(3600);
        String token = config.jwtEncoder(config.jwtSecretKey(SECRETO)).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(EMISOR).subject(UUID.randomUUID().toString())
                        .issuedAt(hace).expiresAt(hace.plusSeconds(60)).build())).getTokenValue();

        assertThatThrownBy(() -> decoder(SECRETO, EMISOR).decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void passwordEncoder_guardaUnHashQueNoEsLaContrasenaYLaVerifica() {
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("secreta1");

        assertThat(hash).isNotEqualTo("secreta1");
        assertThat(encoder.matches("secreta1", hash)).isTrue();
        assertThat(encoder.matches("otra-cosa", hash)).isFalse();
    }

    private TokenService tokenService(String secreto, String emisor, long vidaMinutos) {
        JwtEncoder encoder = config.jwtEncoder(config.jwtSecretKey(secreto));
        return new TokenService(encoder, emisor, vidaMinutos);
    }

    private JwtDecoder decoder(String secreto, String emisor) {
        SecretKey clave = config.jwtSecretKey(secreto);
        return config.jwtDecoder(clave, emisor);
    }
}
