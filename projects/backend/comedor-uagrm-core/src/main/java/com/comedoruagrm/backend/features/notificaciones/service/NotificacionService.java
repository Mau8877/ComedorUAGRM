package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.exception.PlantillaIncompletaException;
import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository;
import com.comedoruagrm.backend.features.notificaciones.service.PlantillaNotificacion.NotificacionRenderizada;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Punto de entrada del servicio de notificaciones (CU-EXTRA-01): cualquier modulo avisa a un
 * usuario con {@link #notificar}, sin saber como se envia.
 *
 * * Guarda el aviso en la bandeja dentro de la transaccion de quien llama y publica un evento
 * * que `DespachoNotificaciones` atiende DESPUES del commit y en otro hilo: un rollback no deja
 * * avisos ni pushes fantasma, y un envio lento no demora la operacion de negocio.
 * ! Nunca se loguea el titulo, el mensaje, el correo ni los tokens.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final PlantillaNotificacion plantilla;
    private final ApplicationEventPublisher publicador;
    private final UsuarioService usuarioService;

    /**
     * Avisa a un usuario de un evento. {@code datos} son los valores de los marcadores de la
     * plantilla del evento; si falta alguno no se guarda ni se envia nada y se registra (A10).
     */
    @Transactional
    public void notificar(Usuario destinatario, EventoNotificacion evento, Map<String, String> datos) {
        NotificacionRenderizada aviso;
        try {
            aviso = plantilla.renderizar(evento, datos);
        } catch (PlantillaIncompletaException e) {
            LOG.error("No se notifica: la plantilla del evento esta incompleta",
                    kv("module", "notificaciones"), kv("evento", evento.name()), kv("detalle", e.getMessage()));
            return;
        }
        Notificacion guardada = notificacionRepository.save(
                new Notificacion(destinatario, aviso.titulo(), aviso.mensaje(), aviso.tipo()));
        publicador.publishEvent(new NotificacionGuardada(guardada.getId(), guardada.getCodigo(),
                destinatario.getId(), destinatario.getCorreo(), aviso.titulo(), aviso.mensaje(), evento));
    }

    /** Aviso de prueba del Administrador DUBSS, siempre a si mismo (CU-EXTRA-01, flujo E). */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    public void enviarPrueba(UUID usuarioCodigo) {
        Usuario usuario = usuarioService.obtenerPorCodigo(usuarioCodigo);
        notificar(usuario, EventoNotificacion.PRUEBA, Map.of("nombres", usuario.getNombres()));
    }

    /** Deja constancia de que el aviso se entrego por al menos una via (uso del despacho). */
    @Transactional
    public void marcarEnviada(Long notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(notificacion -> {
            notificacion.setEnviadaEn(Instant.now());
            notificacionRepository.save(notificacion);
        });
    }
}
