package com.comedoruagrm.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el token de acceso (JWT firmado, de vida corta). Lleva solo el
 * `codigo` publico del usuario como `sub`: el rol y el estado de la cuenta
 * se leen de la base en cada peticion (ver UsuarioActivoFilter).
 */
@Component
public class TokenService {

    /** `expiraEnSegundos` es la vida del token, para que el cliente sepa cuando renovarlo. */
    public record TokenAcceso(String token, long expiraEnSegundos) {
    }

    private final JwtEncoder encoder;
    private final String emisor;
    private final Duration vida;

    public TokenService(JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String emisor,
            @Value("${app.jwt.access-ttl-minutes}") long vidaMinutos) {
        this.encoder = encoder;
        this.emisor = emisor;
        this.vida = Duration.ofMinutes(vidaMinutos);
    }

    public TokenAcceso emitirAcceso(UUID codigoUsuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(codigoUsuario.toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vida))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenAcceso(token, vida.toSeconds());
    }
}
