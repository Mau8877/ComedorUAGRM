package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.comedoruagrm.backend.config.NotificacionesConfig;

import lombok.RequiredArgsConstructor;

/**
 * Entrega un aviso ya guardado: push a todos los telefonos del usuario y correo (CU-EXTRA-01).
 *
 * * Corre despues del commit de quien notifico y en el ejecutor de notificaciones. Cada via
 * * es independiente: que falle una no impide la otra. Cualquier error se registra y se traga,
 * * porque la operacion de negocio que notifico ya termino bien.
 * * El push reintenta solo los telefonos que fallaron por un problema pasajero; los tokens que
 * * Firebase declara invalidos se eliminan. El aviso se marca enviado si al menos una via llego.
 * ! Nunca se loguean tokens, correo ni texto del aviso.
 */
@Component
@RequiredArgsConstructor
public class DespachoNotificaciones {

    private static final Logger LOG = LoggerFactory.getLogger(DespachoNotificaciones.class);

    private final CanalPush canalPush;
    private final DispositivoFcmService dispositivoService;
    private final CorreoNotificacionService correoService;
    private final NotificacionService notificacionService;
    private final Reintentos reintentos;

    @Async(NotificacionesConfig.EJECUTOR_NOTIFICACIONES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void despachar(NotificacionGuardada aviso) {
        try {
            int telefonos = enviarPush(aviso);
            boolean correo = aviso.evento().enviaCorreo()
                    && correoService.enviar(aviso.correo(), aviso.titulo(), aviso.mensaje());
            if (telefonos > 0 || correo) {
                notificacionService.marcarEnviada(aviso.notificacionId());
            }
            LOG.info("Aviso despachado", kv("module", "notificaciones"), kv("userId", aviso.usuarioId()),
                    kv("evento", aviso.evento().name()), kv("telefonos", telefonos), kv("correo", correo));
        } catch (RuntimeException e) {
            LOG.error("Fallo inesperado al despachar un aviso", kv("module", "notificaciones"),
                    kv("userId", aviso.usuarioId()), kv("evento", aviso.evento().name()),
                    kv("causa", e.getClass().getSimpleName()));
        }
    }

    /** Devuelve a cuantos telefonos llego el push. */
    private int enviarPush(NotificacionGuardada aviso) {
        List<String> pendientes = dispositivoService.tokensDe(aviso.usuarioId());
        if (pendientes.isEmpty()) {
            return 0;
        }
        MensajePush mensaje = new MensajePush(aviso.titulo(), aviso.mensaje(), Map.of(
                "codigoNotificacion", aviso.notificacionCodigo().toString(),
                "evento", aviso.evento().name()));
        int entregados = 0;
        Set<String> invalidos = new LinkedHashSet<>();
        try {
            for (int intento = 1; !pendientes.isEmpty(); intento++) {
                ResultadoPush resultado = canalPush.enviar(pendientes, mensaje);
                entregados += resultado.enviados();
                invalidos.addAll(resultado.tokensInvalidos());
                pendientes = resultado.tokensPasajeros();
                if (pendientes.isEmpty() || intento >= reintentos.intentos() || !reintentos.esperar(intento)) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Fallo el envio del push", kv("module", "notificaciones"),
                    kv("causa", e.getClass().getSimpleName()));
        }
        dispositivoService.eliminarInvalidos(invalidos);
        if (!pendientes.isEmpty()) {
            LOG.warn("Telefonos sin avisar tras agotar los reintentos", kv("module", "notificaciones"),
                    kv("telefonos", pendientes.size()));
        }
        return entregados;
    }
}
