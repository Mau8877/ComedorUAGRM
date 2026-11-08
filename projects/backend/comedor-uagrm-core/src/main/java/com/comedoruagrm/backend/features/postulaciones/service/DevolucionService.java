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
import com.comedoruagrm.backend.features.postulaciones.dto.DevolucionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DevolverPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.DevolucionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.DevolucionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DevolucionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Devolucion de una postulacion para corregir (CU19). Solo el Administrador DUBSS.
 *
 * * Cambia el estado «calificada» a «devuelta», deja la fila en el historial, anota la bitacora y avisa
 * * al postulante (bandeja, push y correo) en una sola transaccion. No toca documentos, puntaje,
 * * auditorias ni la fila de `finalista`.
 * * No hay idempotencia por clave: una segunda peticion sobre la misma postulacion recibe `ERR_DEV_01`
 * * porque ya no esta «calificada». Se bloquea la fila de la postulacion para serializar las simultaneas.
 * ! No se loguean ni se anotan en la bitacora datos personales ni el motivo.
 */
@Service
@RequiredArgsConstructor
public class DevolucionService {

    private static final Logger LOG = LoggerFactory.getLogger(DevolucionService.class);

    private final DevolucionConsultaRepository consulta;
    private final PostulacionRepository postulacionRepository;
    private final PostulacionHistorialRepository historialRepository;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.DEVOLUCION_POSTULACION, entidad = "postulacion_historial")
    public ResultadoAuditado<DevolucionResponse> devolver(UUID actorCodigo, UUID finalistaCodigo,
            DevolverPostulacionRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        Cabecera c = consulta.cabecera(finalistaCodigo).orElseThrow(() -> new NotFoundException(
                FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        consulta.bloquearPostulacion(c.postulacionId());
        String motivo = request.motivo().trim();

        // Se vuelve a leer el estado despues del bloqueo: la peticion que llego segunda ya lo ve cambiado.
        if (!EstadoPostulacion.calificada.name().equals(consulta.estadoActual(c.postulacionId()))) {
            throw new ConflictException(DevolucionErrorCodes.ERR_DEV_01,
                    "Solo se puede devolver una postulación calificada");
        }
        if (!consulta.hayAuditoriaPosteriorALaUltimaDevolucion(c.finalistaId(), c.postulacionId())) {
            throw new ConflictException(DevolucionErrorCodes.ERR_DEV_02,
                    "Primero se debe registrar la auditoría del expediente");
        }

        Postulacion postulacion = postulacionRepository.findById(c.postulacionId()).orElseThrow();
        postulacion.devolver();
        PostulacionHistorial historial = historialRepository.saveAndFlush(new PostulacionHistorial(
                c.postulacionId(), actor.getId(), EstadoPostulacion.calificada, EstadoPostulacion.devuelta, motivo));
        LOG.info("Postulacion devuelta para corregir", kv("module", "postulaciones"), kv("userId", actorCodigo));
        avisar(c, motivo);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("historial", historial.getCodigo().toString());
        detalle.put("postulacion", c.postulacionCodigo().toString());
        detalle.put("finalista", c.finalistaCodigo().toString());
        detalle.put("estadoAnterior", EstadoPostulacion.calificada.name());
        detalle.put("estadoNuevo", EstadoPostulacion.devuelta.name());
        Instant fecha = historial.getCreadoEn() == null ? Instant.now() : historial.getCreadoEn();
        DevolucionResponse respuesta = new DevolucionResponse(c.postulacionCodigo(),
                EstadoPostulacion.devuelta.name(), motivo, fecha, actor.getNombres() + " " + actor.getApellidos());
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), historial.getId(), detalle, true);
    }

    /** Avisa al postulante; cualquier fallo se registra como WARN y no deshace la devolucion. */
    private void avisar(Cabecera c, String motivo) {
        try {
            Usuario destinatario = usuarioService.obtenerPorCodigo(c.postulanteCodigo());
            Map<String, String> datos = new LinkedHashMap<>();
            datos.put("nombres", destinatario.getNombres());
            datos.put("observaciones", motivo);
            if (!notificacionService.notificar(destinatario, EventoNotificacion.POSTULACION_DEVUELTA, datos,
                    c.postulacionId(), null)) {
                LOG.warn("El aviso de la devolucion no se pudo enviar", kv("module", "postulaciones"));
            }
        } catch (RuntimeException e) {
            LOG.warn("El aviso de la devolucion fallo", kv("module", "postulaciones"),
                    kv("causa", e.getClass().getSimpleName()));
        }
    }
}
