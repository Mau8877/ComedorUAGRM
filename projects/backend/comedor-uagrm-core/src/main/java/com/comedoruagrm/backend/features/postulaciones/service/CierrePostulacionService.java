package com.comedoruagrm.backend.features.postulaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.CierrePostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.CierrePostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.CierrePostulacionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Cierre de una postulacion: archivar o rechazar (CU20). Solo el Administrador DUBSS.
 *
 * * Cambia el estado «calificada» a «archivada» (archivar) o «no adjudicada» (rechazar), deja la fila en
 * * el historial, anota la bitacora y avisa al postulante (bandeja, push y correo) en una sola transaccion.
 * * No toca documentos, puntaje, auditorias ni la fila de `finalista`. No exige auditoria previa ni entrega fisica.
 * * No hay idempotencia por clave: una segunda peticion sobre la misma postulacion recibe `ERR_ARC_01`
 * * porque ya no esta «calificada». Se bloquea la fila de la postulacion para serializar las simultaneas.
 * ! No se loguean ni se anotan en la bitacora datos personales ni el motivo.
 */
@Service
@RequiredArgsConstructor
public class CierrePostulacionService {

    private static final Logger LOG = LoggerFactory.getLogger(CierrePostulacionService.class);

    private final CierrePostulacionConsultaRepository consulta;
    private final PostulacionRepository postulacionRepository;
    private final PostulacionHistorialRepository historialRepository;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    /** Las dos salidas del cierre y lo que cambia entre una y otra. */
    private enum Accion {
        ARCHIVAR(EstadoPostulacion.archivada, EventoNotificacion.POSTULACION_ARCHIVADA, "archivada"),
        RECHAZAR(EstadoPostulacion.no_adjudicada, EventoNotificacion.POSTULACION_RECHAZADA, "rechazada");

        private final EstadoPostulacion estadoNuevo;
        private final EventoNotificacion evento;
        private final String participio;

        Accion(EstadoPostulacion estadoNuevo, EventoNotificacion evento, String participio) {
            this.estadoNuevo = estadoNuevo;
            this.evento = evento;
            this.participio = participio;
        }

        void aplicar(Postulacion postulacion) {
            if (this == ARCHIVAR) {
                postulacion.archivar();
            } else {
                postulacion.rechazar();
            }
        }
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.ARCHIVO_POSTULACION, entidad = "postulacion_historial")
    public ResultadoAuditado<CierrePostulacionResponse> archivar(UUID actorCodigo, UUID finalistaCodigo,
            CierrePostulacionRequest request) {
        return cerrar(Accion.ARCHIVAR, actorCodigo, finalistaCodigo, request);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.RECHAZO_POSTULACION, entidad = "postulacion_historial")
    public ResultadoAuditado<CierrePostulacionResponse> rechazar(UUID actorCodigo, UUID finalistaCodigo,
            CierrePostulacionRequest request) {
        return cerrar(Accion.RECHAZAR, actorCodigo, finalistaCodigo, request);
    }

    private ResultadoAuditado<CierrePostulacionResponse> cerrar(Accion accion, UUID actorCodigo,
            UUID finalistaCodigo, CierrePostulacionRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        Cabecera c = consulta.cabecera(finalistaCodigo).orElseThrow(() -> new NotFoundException(
                FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        consulta.bloquearPostulacion(c.postulacionId());
        String motivo = request.motivo().trim();

        // Se vuelve a leer el estado despues del bloqueo: la peticion que llego segunda ya lo ve cambiado.
        String estadoActual = consulta.estadoActual(c.postulacionId());
        if (!EstadoPostulacion.calificada.name().equals(estadoActual)) {
            throw new ConflictException(CierrePostulacionErrorCodes.ERR_ARC_01,
                    "La postulación ya no está calificada (estado actual: " + estadoActual + ")");
        }
        if (consulta.resultadosPublicados(c.convocatoriaId())) {
            throw new ConflictException(CierrePostulacionErrorCodes.ERR_ARC_01,
                    "Los resultados de la convocatoria ya fueron publicados");
        }

        Postulacion postulacion = postulacionRepository.findById(c.postulacionId()).orElseThrow();
        accion.aplicar(postulacion);
        PostulacionHistorial historial = historialRepository.saveAndFlush(new PostulacionHistorial(
                c.postulacionId(), actor.getId(), EstadoPostulacion.calificada, accion.estadoNuevo, motivo));
        LOG.info("Postulacion " + accion.participio, kv("module", "postulaciones"), kv("userId", actorCodigo));
        avisar(accion, c, motivo);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("historial", historial.getCodigo().toString());
        detalle.put("postulacion", c.postulacionCodigo().toString());
        detalle.put("finalista", c.finalistaCodigo().toString());
        detalle.put("estadoAnterior", EstadoPostulacion.calificada.name());
        detalle.put("estadoNuevo", accion.estadoNuevo.name());
        Instant fecha = historial.getCreadoEn() == null ? Instant.now() : historial.getCreadoEn();
        CierrePostulacionResponse respuesta = new CierrePostulacionResponse(c.postulacionCodigo(),
                accion.estadoNuevo.name(), motivo, fecha, actor.getNombres() + " " + actor.getApellidos());
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), historial.getId(), detalle, true);
    }

    /** Avisa al postulante; cualquier fallo se registra como WARN y no deshace la decision. */
    private void avisar(Accion accion, Cabecera c, String motivo) {
        try {
            Usuario destinatario = usuarioService.obtenerPorCodigo(c.postulanteUsuarioCodigo());
            Map<String, String> datos = new LinkedHashMap<>();
            datos.put("nombres", destinatario.getNombres());
            datos.put("motivo", motivo);
            if (!notificacionService.notificar(destinatario, accion.evento, datos, c.postulacionId(), null)) {
                LOG.warn("El aviso del cierre no se pudo enviar", kv("module", "postulaciones"));
            }
        } catch (RuntimeException e) {
            LOG.warn("El aviso del cierre fallo", kv("module", "postulaciones"),
                    kv("causa", e.getClass().getSimpleName()));
        }
    }
}
