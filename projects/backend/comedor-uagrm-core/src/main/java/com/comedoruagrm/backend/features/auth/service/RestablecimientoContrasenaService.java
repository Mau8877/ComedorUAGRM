package com.comedoruagrm.backend.features.auth.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.security.AuthErrorCodes;

/**
 * Recuperacion de contraseña por correo (CU15, flujos A y B).
 *
 * * Solo hay un enlace vigente por cuenta: pedir otro sobrescribe el anterior (que deja de
 * * servir) y usarlo lo borra. En la base solo vive el hash del token.
 * * `solicitar` nunca distingue casos hacia afuera (mensaje neutro, sin bitacora, A1).
 * ! Ni el token, ni el enlace, ni el correo, ni la contraseña se loguean.
 */
@Service
public class RestablecimientoContrasenaService {

    private static final Logger LOG = LoggerFactory.getLogger(RestablecimientoContrasenaService.class);

    private static final String MENSAJE_ENLACE_INVALIDO = "El enlace ya no es válido. Solicita uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final SesionService sesionService;
    private final TokenEnlace tokenEnlace;
    private final CorreoRestablecimientoService correoService;
    private final Duration vigencia;

    public RestablecimientoContrasenaService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
            SesionService sesionService, TokenEnlace tokenEnlace, CorreoRestablecimientoService correoService,
            @Value("${app.auth.reset-ttl-minutes}") long vigenciaMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
        this.tokenEnlace = tokenEnlace;
        this.correoService = correoService;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
    }

    /** Vigencia del enlace en minutos (para el mensaje que ve la persona). */
    public long vigenciaMinutos() {
        return vigencia.toMinutes();
    }

    /**
     * Genera y envia el enlace si el correo es de una cuenta activa; en cualquier otro caso
     * (correo desconocido, cuenta desactivada) no hace nada visible (A1). No se anota en la
     * bitacora para no dejar rastro de correos inexistentes. El correo sale solo despues de
     * confirmar la transaccion: si la base deshace el cambio, el enlace no llega a nadie.
     */
    @Transactional
    public void solicitar(String correo) {
        var cuenta = usuarioRepository.findByCorreo(normalizarCorreo(correo)).filter(Usuario::isActivo);
        if (cuenta.isEmpty()) {
            LOG.info("Solicitud de restablecimiento sin cuenta activa", kv("module", "auth"));
            return;
        }
        Usuario usuario = cuenta.get();
        String token = tokenEnlace.generar();
        usuario.iniciarRestablecimiento(tokenEnlace.hash(token), Instant.now().plus(vigencia));
        usuarioRepository.save(usuario);

        String destino = usuario.getCorreo();
        String nombres = usuario.getNombres();
        despuesDeConfirmar(() -> correoService.enviarEnlace(destino, nombres, token));
        LOG.info("Enlace de restablecimiento generado", kv("module", "auth"), kv("userId", usuario.getCodigo()));
    }

    /**
     * Define la contraseña nueva con el enlace: guarda el hash, desbloquea la cuenta, invalida
     * el enlace y cierra todas sus sesiones. Cualquier enlace malo (desconocido, vencido, usado,
     * reemplazado o de una cuenta desactivada) da el mismo `400` `ERR_AUTH_08`.
     */
    @Transactional
    @Auditar(accion = BitacoraAcciones.RESTABLECIMIENTO_CONTRASENA, entidad = "usuario")
    public ResultadoAuditado<Void> restablecer(String token, String contrasenaNueva) {
        Instant ahora = Instant.now();
        Usuario usuario = usuarioRepository.findByRestablecimientoTokenHash(tokenEnlace.hash(token))
                .filter(u -> u.tieneRestablecimientoVigente(ahora))
                .filter(Usuario::isActivo)
                .orElseThrow(this::enlaceInvalido);

        usuarioService.restablecerContrasena(usuario, contrasenaNueva);
        usuario.limpiarRestablecimiento();
        usuarioRepository.save(usuario);
        sesionService.revocarTodas(usuario);

        LOG.info("Contraseña restablecida con el enlace", kv("module", "auth"), kv("userId", usuario.getCodigo()));
        return new ResultadoAuditado<>(null, usuario.getId(), usuario.getCodigo(), usuario.getId(),
                Map.of(), true);
    }

    private ValidationException enlaceInvalido() {
        LOG.warn("Enlace de restablecimiento rechazado",
                kv("module", "auth"), kv("errorCode", AuthErrorCodes.ERR_AUTH_08));
        return new ValidationException(AuthErrorCodes.ERR_AUTH_08, MENSAJE_ENLACE_INVALIDO);
    }

    // * El correo se compara en minuscula y sin espacios, igual que en el registro y el login.
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void despuesDeConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
