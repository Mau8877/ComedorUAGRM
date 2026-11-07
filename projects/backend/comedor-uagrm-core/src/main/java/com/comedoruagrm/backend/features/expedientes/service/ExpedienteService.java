package com.comedoruagrm.backend.features.expedientes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.expedientes.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.expedientes.dto.AuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.ConvocatoriaExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.EntregaExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.FinalistaExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.PostulanteExpediente;
import com.comedoruagrm.backend.features.expedientes.dto.RegistrarAuditoriaRequest;
import com.comedoruagrm.backend.features.expedientes.dto.ResultadoAuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.expedientes.exception.ExpedienteErrorCodes;
import com.comedoruagrm.backend.features.expedientes.model.AuditoriaExpediente;
import com.comedoruagrm.backend.features.expedientes.repository.AuditoriaExpedienteRepository;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaConsultaRepository;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Expediente del finalista (CU07). Solo el Administrador DUBSS.
 *
 * * Ni la consulta ni el registro de una auditoria cambian el estado de la postulacion ni su puntaje.
 * * Una auditoria es append-only: corregir un resultado es registrar otra.
 * ! No se loguean ni se anotan en la bitacora datos personales ni las observaciones.
 */
@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private static final Logger LOG = LoggerFactory.getLogger(ExpedienteService.class);

    static final BigDecimal PUNTAJE_ACADEMICO_MAXIMO = new BigDecimal("30");

    private final ExpedienteConsultaRepository consulta;
    private final AuditoriaExpedienteRepository auditoriaRepository;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;
    private final AlmacenamientoService almacenamiento;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ExpedienteResponse obtener(UUID finalistaCodigo) {
        Cabecera c = consulta.cabecera(finalistaCodigo).orElseThrow(() -> new NotFoundException(
                FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        List<AuditoriaResponse> auditorias = consulta.auditorias(c.finalistaId());
        BigDecimal academico = auditorias.isEmpty() ? null : auditorias.get(0).puntajeAcademico();
        Instant recibida = c.entregaRecibidaEn() == null ? null : c.entregaRecibidaEn().toInstant();
        EntregaExpediente entrega = new EntregaExpediente(recibida == null ? "PENDIENTE" : "RECIBIDA",
                recibida == null ? null : recibida.atZone(FinalistaConsultaRepository.ZONA).toLocalDate());
        return new ExpedienteResponse(
                new FinalistaExpediente(c.finalistaCodigo(), c.posicion(), c.plazoEntrega(), entrega),
                new PostulanteExpediente(c.nombres(), c.apellidos(), c.ci(), c.carrera()),
                new ConvocatoriaExpediente(c.convocatoriaCodigo(), c.convocatoriaNombre()),
                consulta.respuestaSocioeconomica(c.postulacionId()).orElse(null),
                consulta.documentos(c.postulacionId(), c.convocatoriaId()),
                c.estadoPostulacion(), c.puntajeIa(), desglose(c.desgloseJson()), academico,
                total(c.puntajeIa(), academico), auditorias);
    }

    /**
     * URL temporal (5 minutos) para abrir un documento del finalista. Es una lectura: no anota la bitacora.
     *
     * ! Ni la ruta guardada ni la URL se loguean. Si MinIO falla, la excepcion de infraestructura sube sola.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public AccesoDocumentoResponse accesoDocumento(UUID finalistaCodigo, UUID documentoCodigo) {
        Cabecera c = consulta.cabecera(finalistaCodigo).orElseThrow(() -> new NotFoundException(
                FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        String ruta = consulta.rutaDocumento(c.postulacionId(), documentoCodigo).orElseThrow(
                () -> new NotFoundException(ExpedienteErrorCodes.ERR_EXP_05,
                        "El documento no existe o no pertenece a este expediente"));
        Instant expiraEn = Instant.now().plus(AlmacenamientoService.VIGENCIA_ACCESO);
        return new AccesoDocumentoResponse(
                almacenamiento.urlTemporal(ruta, AlmacenamientoService.VIGENCIA_ACCESO), expiraEn);
    }

    /**
     * Guarda la auditoria del expediente, la anota en la bitacora y avisa al postulante.
     *
     * * Un fallo del aviso no deshace la auditoria: se registra un WARN y sigue.
     * * Idempotente por clave (RNF-03): repetir la peticion con la misma `Idempotency-Key` devuelve la
     * * auditoria ya guardada (`creada = false`), sin bitacora ni aviso nuevos. Sin clave siempre se crea.
     * * La misma clave con otro contenido es un conflicto (`ERR_EXP_06`); una clave que no es UUID, `ERR_EXP_07`.
     * * Se bloquea la fila del finalista: dos peticiones simultaneas se atienden una despues de la otra.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.AUDITORIA_EXPEDIENTE, entidad = "auditoria_expediente")
    public ResultadoAuditado<ResultadoAuditoriaResponse> registrarAuditoria(UUID actorCodigo,
            UUID finalistaCodigo, String claveIdempotencia, RegistrarAuditoriaRequest request) {
        UUID clave = leerClave(claveIdempotencia);
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        Cabecera c = consulta.cabecera(finalistaCodigo).orElseThrow(() -> new NotFoundException(
                FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        consulta.bloquearFinalista(c.finalistaId());
        ResultadoAuditoria resultado = request.resultado();
        String observaciones = request.observaciones() == null || request.observaciones().isBlank()
                ? null : request.observaciones().trim();
        BigDecimal puntaje = request.puntajeAcademico();

        // La repeticion se resuelve antes de validar el estado: la auditoria ya existe y se devuelve tal cual.
        AuditoriaExpediente previa = clave == null ? null : auditoriaRepository
                .findByFinalistaIdAndClaveIdempotencia(c.finalistaId(), clave).orElse(null);
        if (previa != null) {
            if (!mismoContenido(previa, resultado, observaciones, puntaje)) {
                throw new ConflictException(ExpedienteErrorCodes.ERR_EXP_06,
                        "La clave de idempotencia ya se usó para otra auditoría");
            }
            AuditoriaResponse existente = consulta.auditorias(c.finalistaId()).stream()
                    .filter(a -> a.codigo().equals(previa.getCodigo())).findFirst().orElseThrow();
            LOG.info("Auditoria repetida, no se crea otra", kv("module", "expedientes"),
                    kv("userId", actorCodigo));
            return new ResultadoAuditado<>(new ResultadoAuditoriaResponse(existente, false), actor.getId(),
                    actor.getCodigo(), previa.getId(), Map.of(), false);
        }

        if (resultado != ResultadoAuditoria.VALIDADO && observaciones == null) {
            throw new ValidationException(ExpedienteErrorCodes.ERR_EXP_03,
                    "Las observaciones son obligatorias si el expediente es observado o no válido");
        }
        if (resultado == ResultadoAuditoria.VALIDADO && puntaje == null) {
            throw new ValidationException(ExpedienteErrorCodes.ERR_EXP_04,
                    "El puntaje académico es obligatorio si el expediente es validado");
        }
        if (puntaje != null && (puntaje.signum() < 0 || puntaje.compareTo(PUNTAJE_ACADEMICO_MAXIMO) > 0
                || puntaje.stripTrailingZeros().scale() > 2)) {
            throw new ValidationException(ExpedienteErrorCodes.ERR_EXP_04,
                    "El puntaje académico debe estar entre 0 y 30, con hasta dos decimales");
        }
        if (!"calificada".equals(c.estadoPostulacion())) {
            throw new ConflictException(ExpedienteErrorCodes.ERR_EXP_02,
                    "Solo se puede auditar el expediente de una postulación calificada");
        }
        if (c.entregaRecibidaEn() == null) {
            throw new ConflictException(ExpedienteErrorCodes.ERR_EXP_01,
                    "Primero se debe registrar la recepción de la documentación física");
        }

        Instant ahora = Instant.now();
        AuditoriaExpediente guardada = auditoriaRepository.saveAndFlush(new AuditoriaExpediente(
                c.finalistaId(), actor.getId(), ahora, resultado, observaciones, puntaje, clave));
        LOG.info("Auditoria de expediente registrada", kv("module", "expedientes"), kv("userId", actorCodigo),
                kv("resultado", resultado));
        avisar(c, resultado, observaciones);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("auditoria", guardada.getCodigo().toString());
        detalle.put("finalista", c.finalistaCodigo().toString());
        detalle.put("resultado", resultado.name());
        if (puntaje != null) {
            detalle.put("puntajeAcademico", puntaje.toPlainString());
        }
        AuditoriaResponse respuesta = new AuditoriaResponse(guardada.getCodigo(), resultado, observaciones,
                puntaje, ahora, actor.getNombres() + " " + actor.getApellidos());
        return new ResultadoAuditado<>(new ResultadoAuditoriaResponse(respuesta, true), actor.getId(),
                actor.getCodigo(), guardada.getId(), detalle, true);
    }

    private static boolean mismoContenido(AuditoriaExpediente a, ResultadoAuditoria resultado,
            String observaciones, BigDecimal puntaje) {
        return a.getResultado() == resultado
                && Objects.equals(a.getObservaciones(), observaciones)
                && (a.getPuntajeAcademico() == null ? puntaje == null
                        : puntaje != null && a.getPuntajeAcademico().compareTo(puntaje) == 0);
    }

    /** El header es opcional; si viene, tiene que ser un UUID. */
    private static UUID leerClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(clave.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ExpedienteErrorCodes.ERR_EXP_07,
                    "La clave de idempotencia no es válida");
        }
    }

    /** Avisa al postulante; cualquier fallo se registra como WARN y no deshace la auditoria. */
    private void avisar(Cabecera c, ResultadoAuditoria resultado, String observaciones) {
        try {
            UUID usuarioCodigo = consulta.codigoPostulante(c.postulacionId()).orElse(null);
            if (usuarioCodigo == null) {
                LOG.warn("No se avisa la auditoria: el postulante no existe", kv("module", "expedientes"));
                return;
            }
            Usuario destinatario = usuarioService.obtenerPorCodigo(usuarioCodigo);
            EventoNotificacion evento = switch (resultado) {
                case VALIDADO -> EventoNotificacion.AUDITORIA_VALIDADA;
                case OBSERVADO -> EventoNotificacion.AUDITORIA_OBSERVADA;
                case NO_VALIDO -> EventoNotificacion.AUDITORIA_NO_VALIDA;
            };
            Map<String, String> datos = new LinkedHashMap<>();
            datos.put("nombres", destinatario.getNombres());
            if (resultado != ResultadoAuditoria.VALIDADO) {
                datos.put("observaciones", observaciones);
            }
            if (!notificacionService.notificar(destinatario, evento, datos, c.postulacionId(), null)) {
                LOG.warn("El aviso de la auditoria no se pudo enviar", kv("module", "expedientes"),
                        kv("evento", evento));
            }
        } catch (RuntimeException e) {
            LOG.warn("El aviso de la auditoria fallo", kv("module", "expedientes"),
                    kv("causa", e.getClass().getSimpleName()));
        }
    }

    /** IA + puntaje academico de la ultima auditoria; null si falta cualquiera de los dos. */
    static BigDecimal total(BigDecimal ia, BigDecimal academico) {
        return ia == null || academico == null ? null : ia.add(academico);
    }

    private static Map<String, Object> desglose(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JsonParserFactory.getJsonParser().parseMap(json);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
