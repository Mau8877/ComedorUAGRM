package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.comedoruagrm.backend.config.NotificacionesConfig;

import lombok.RequiredArgsConstructor;

/**
 * Entrega un lote de avisos ya guardado (CU22): los telefonos de todos los destinatarios se resuelven en una
 * sola consulta y luego cada aviso sale por push y correo, igual que en {@link DespachoNotificaciones}.
 *
 * * Corre despues del commit, en el ejecutor de notificaciones. Cada aviso es independiente: que falle uno no
 * * detiene a los demas, y cualquier error se registra y se traga (la operacion de negocio ya termino).
 * * Los avisos entregados por al menos una via se marcan enviados con una sola sentencia al final.
 * ! Nunca se loguean tokens, correos ni textos de avisos.
 */
@Component
@RequiredArgsConstructor
public class DespachoLoteNotificaciones {

    private static final Logger LOG = LoggerFactory.getLogger(DespachoLoteNotificaciones.class);

    private final DespachoNotificaciones despachoIndividual;
    private final DispositivoFcmService dispositivoService;
    private final CorreoNotificacionService correoService;
    private final NotificacionService notificacionService;

    @Async(NotificacionesConfig.EJECUTOR_NOTIFICACIONES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void despachar(LoteNotificacionesGuardadas lote) {
        List<Long> entregados = new ArrayList<>();
        try {
            Set<Long> usuarios = lote.avisos().stream().map(NotificacionGuardada::usuarioId)
                    .collect(Collectors.toSet());
            Map<Long, List<String>> telefonos = dispositivoService.tokensDe(usuarios);
            for (NotificacionGuardada aviso : lote.avisos()) {
                if (despacharUno(aviso, telefonos.getOrDefault(aviso.usuarioId(), List.of()))) {
                    entregados.add(aviso.notificacionId());
                }
            }
            notificacionService.marcarEnviadas(entregados);
            LOG.info("Lote de avisos despachado", kv("module", "notificaciones"), kv("avisos", lote.avisos().size()),
                    kv("entregados", entregados.size()));
        } catch (RuntimeException e) {
            LOG.error("Fallo inesperado al despachar un lote de avisos", kv("module", "notificaciones"),
                    kv("causa", e.getClass().getSimpleName()));
        }
    }

    /** Devuelve si el aviso llego por al menos una via. */
    private boolean despacharUno(NotificacionGuardada aviso, List<String> tokens) {
        try {
            int telefonos = despachoIndividual.enviarPush(aviso, tokens);
            boolean correo = aviso.evento().enviaCorreo() && correoService.enviar(aviso.correo(), aviso.titulo(),
                    aviso.mensaje());
            return telefonos > 0 || correo;
        } catch (RuntimeException e) {
            LOG.error("Fallo inesperado al despachar un aviso del lote", kv("module", "notificaciones"),
                    kv("userId", aviso.usuarioId()), kv("evento", aviso.evento().name()),
                    kv("causa", e.getClass().getSimpleName()));
            return false;
        }
    }
}
