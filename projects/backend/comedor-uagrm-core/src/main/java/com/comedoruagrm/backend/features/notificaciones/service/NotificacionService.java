package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.List;
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

import jakarta.persistence.EntityManager;
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

    /** Cada cuantos avisos de un envio masivo se vuelcan a la base. */
    private static final int LOTE = 500;

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final PlantillaNotificacion plantilla;
    private final ApplicationEventPublisher publicador;
    private final UsuarioService usuarioService;
    private final EntityManager entityManager;

    /**
     * Avisa a un usuario de un evento. {@code datos} son los valores de los marcadores de la
     * plantilla del evento; si falta alguno no se guarda ni se envia nada y se registra (A10).
     */
    @Transactional
    public boolean notificar(Usuario destinatario, EventoNotificacion evento, Map<String, String> datos) {
        return notificar(destinatario, evento, datos, null, null);
    }

    /**
     * Igual que {@link #notificar(Usuario, EventoNotificacion, Map)} pero ligando el aviso a una
     * postulacion y/o a un comunicado (CU21). Devuelve false si el aviso no se pudo registrar.
     */
    @Transactional
    public boolean notificar(Usuario destinatario, EventoNotificacion evento, Map<String, String> datos,
            Long postulacionId, Long comunicadoId) {
        NotificacionRenderizada aviso;
        try {
            aviso = plantilla.renderizar(evento, datos);
        } catch (PlantillaIncompletaException e) {
            LOG.error("No se notifica: la plantilla del evento esta incompleta",
                    kv("module", "notificaciones"), kv("evento", evento.name()), kv("detalle", e.getMessage()));
            return false;
        }
        Notificacion guardada = notificacionRepository.save(
                new Notificacion(destinatario, aviso.titulo(), aviso.mensaje(), aviso.tipo(),
                        postulacionId, comunicadoId));
        publicador.publishEvent(new NotificacionGuardada(guardada.getId(), guardada.getCodigo(),
                destinatario.getId(), destinatario.getCorreo(), aviso.titulo(), aviso.mensaje(), evento));
        return true;
    }

    /**
     * Avisa a muchos usuarios del mismo evento (un comunicado, CU21). Guarda los avisos en la bandeja
     * por lotes dentro de la transaccion de quien llama; el despacho (push) es el asincrono de siempre y
     * no se espera. Solo para eventos sin correo: no se conoce el correo de cada destinatario aqui.
     * Devuelve cuantos avisos se guardaron.
     */
    @Transactional
    public int notificarUsuarios(List<Long> usuarioIds, EventoNotificacion evento, Map<String, String> datos,
            Long comunicadoId) {
        if (evento.enviaCorreo()) {
            throw new IllegalArgumentException("El envio masivo es solo para eventos sin correo: " + evento);
        }
        NotificacionRenderizada aviso = plantilla.renderizar(evento, datos);
        int guardados = 0;
        for (Long usuarioId : usuarioIds) {
            Usuario referencia = entityManager.getReference(Usuario.class, usuarioId);
            Notificacion guardada = notificacionRepository.save(
                    new Notificacion(referencia, aviso.titulo(), aviso.mensaje(), aviso.tipo(), null, comunicadoId));
            publicador.publishEvent(new NotificacionGuardada(guardada.getId(), guardada.getCodigo(), usuarioId,
                    null, aviso.titulo(), aviso.mensaje(), evento));
            if (++guardados % LOTE == 0) {
                entityManager.flush();
            }
        }
        return guardados;
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

    /** Deja constancia de que varios avisos se entregaron por al menos una via (despacho por lotes). */
    @Transactional
    public void marcarEnviadas(java.util.Collection<Long> notificacionIds) {
        if (!notificacionIds.isEmpty()) {
            notificacionRepository.marcarEnviadas(notificacionIds, Instant.now());
        }
    }
}
