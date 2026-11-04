package com.comedoruagrm.backend.features.auth.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditable;
import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.model.Sesion;
import com.comedoruagrm.backend.features.auth.repository.SesionRepository;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.TokenService;
import com.comedoruagrm.backend.security.TokenService.TokenAcceso;

/**
 * Sesiones por dispositivo: emite el par token de acceso + refresh token,
 * renueva (rotando el refresh token) y cierra.
 *
 * * El refresh token es una cadena aleatoria opaca de 256 bits; en la base
 * * solo se guarda su hash SHA-256, asi una filtracion de la tabla no sirve
 * * para abrir sesiones. Cada renovacion revoca el token usado y entrega uno nuevo.
 * * Cerrar sesion revoca solo el refresh token recibido: no toca las sesiones
 * * de otros dispositivos (CA19).
 * ! Ni el refresh token ni su hash se loguean.
 */
@Service
public class SesionService {

    private static final Logger LOG = LoggerFactory.getLogger(SesionService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Par de tokens recien emitido; `usuario` es el dueño de la sesion. */
    public record SesionAbierta(Usuario usuario, TokenAcceso acceso, String refreshToken) implements Auditable {

        @Override
        public Long actorId() {
            return usuario.getId();
        }

        @Override
        public UUID actorCodigo() {
            return usuario.getCodigo();
        }

        @Override
        public Map<String, Object> detalle() {
            return Map.of("rol", usuario.getRol().getNombre());
        }
    }

    private final SesionRepository sesionRepository;
    private final TokenService tokenService;
    private final Duration vidaRefresh;

    public SesionService(SesionRepository sesionRepository, TokenService tokenService,
            @Value("${app.jwt.refresh-ttl-days}") long vidaRefreshDias) {
        this.sesionRepository = sesionRepository;
        this.tokenService = tokenService;
        this.vidaRefresh = Duration.ofDays(vidaRefreshDias);
    }

    /**
     * Abre una sesion nueva y anota el inicio de sesion en la bitacora (por login
     * o por registro).
     *
     * ! `renovar` llama a este metodo desde la misma clase, y el proxy no
     * ! intercepta esa llamada: renovar el token NO anota un acceso nuevo, a
     * ! proposito (CU01). Si algun dia `renovar` pasa a llamar a otro bean,
     * ! hay que revisar que no empiece a auditar.
     */
    @Transactional
    @Auditar(accion = BitacoraAcciones.INICIO_SESION, entidad = "usuario")
    public SesionAbierta abrir(Usuario usuario) {
        byte[] aleatorio = new byte[32];
        RANDOM.nextBytes(aleatorio);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);

        sesionRepository.save(new Sesion(usuario, hash(refreshToken), Instant.now().plus(vidaRefresh)));
        return new SesionAbierta(usuario, tokenService.emitirAcceso(usuario.getCodigo()), refreshToken);
    }

    /** Canjea un refresh token vigente por un par nuevo y revoca el usado. */
    @Transactional
    public SesionAbierta renovar(String refreshToken) {
        Instant ahora = Instant.now();
        Sesion sesion = sesionRepository.findByTokenHash(hash(refreshToken))
                .filter(s -> s.estaVigente(ahora))
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_06,
                        "Tu sesión expiró. Inicia sesión de nuevo."));
        Usuario usuario = sesion.getUsuario();
        if (!usuario.isActivo()) {
            throw new ForbiddenException(AuthErrorCodes.ERR_AUTH_03, "Tu cuenta está desactivada. Contacta a la DUBSS.");
        }
        sesion.setRevocadaEn(ahora);
        return abrir(usuario);
    }

    /**
     * Revoca TODAS las sesiones de una cuenta (todos sus dispositivos). Es lo que usa la
     * gestion de cuentas (CU14) al desactivar una cuenta o cambiarle el rol; el cierre de
     * sesion del propio usuario sigue siendo `cerrar` (solo su dispositivo, CU01).
     * Idempotente: sin sesiones vigentes no hace nada ni es un error.
     */
    @Transactional
    public void revocarTodas(Usuario usuario) {
        int cerradas = sesionRepository.revocarTodasDe(usuario.getId(), Instant.now());
        LOG.info("Sesiones de la cuenta revocadas",
                kv("module", "auth"), kv("userId", usuario.getCodigo()), kv("sesionesCerradas", cerradas));
    }

    /** Idempotente: un token desconocido, vencido o ya cerrado se ignora (no es un error, A10). */
    @Transactional
    public void cerrar(String refreshToken) {
        sesionRepository.findByTokenHash(hash(refreshToken))
                .filter(s -> s.getRevocadaEn() == null)
                .ifPresent(s -> s.setRevocadaEn(Instant.now()));
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
