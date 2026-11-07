package com.comedoruagrm.backend.features.finalistas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;
import com.comedoruagrm.backend.features.finalistas.dto.NotificarFinalistasRequest;
import com.comedoruagrm.backend.features.finalistas.dto.RegistrarEntregaRequest;
import com.comedoruagrm.backend.features.finalistas.dto.ResultadoNotificacionResponse;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.finalistas.model.Finalista;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaConsultaRepository;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaRepository;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Finalistas (CU21): listado con el estado de su aviso y de su entrega, notificacion y registro de la
 * entrega de la documentacion fisica. Solo el Administrador DUBSS.
 *
 * * Ninguna operacion cambia el estado de una postulacion.
 * * Los avisos usan el servicio de CU-EXTRA-01: aqui solo se registran en la bandeja; el push y el correo
 * * los despacha ese servicio en segundo plano y su resultado no se espera.
 * * Un cambio del plazo o de la fecha de entrega se anota en la bitacora; el lugar, el horario, los
 * * documentos y las observaciones no tienen columna y viajan en el aviso y en el detalle de la bitacora.
 * ! Nunca nombres, CI ni correos en la bitacora ni en los logs.
 */
@Service
@RequiredArgsConstructor
public class FinalistaService {

    private static final Logger LOG = LoggerFactory.getLogger(FinalistaService.class);
    private static final DateTimeFormatter FECHA_AVISO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinalistaRepository finalistaRepository;
    private final FinalistaConsultaRepository consulta;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    /** Una pagina de finalistas junto con su bloque `meta`. */
    public record PaginaFinalistas(List<FinalistaResponse> datos, PageMeta meta) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaFinalistas listar(UUID convocatoriaCodigo, String search, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        if (!consulta.existeConvocatoria(convocatoriaCodigo)) {
            throw new NotFoundException(FinalistaErrorCodes.ERR_FIN_02, "La convocatoria no existe");
        }
        String busqueda = patronDeBusqueda(search);
        long total = consulta.contar(convocatoriaCodigo, busqueda);
        long desplazamiento = (long) (page - 1) * pageSize;
        List<FinalistaResponse> datos = desplazamiento >= total
                ? List.of()
                : consulta.buscar(convocatoriaCodigo, busqueda, pageSize, desplazamiento);
        int totalPaginas = (int) ((total + pageSize - 1) / pageSize);
        return new PaginaFinalistas(datos, new PageMeta(page, pageSize, total, totalPaginas));
    }

    /**
     * Avisa a los finalistas elegidos: un aviso por cada uno en su bandeja (con push y correo) y los marca
     * como notificados con el plazo. Reenviar a los fallidos es repetir la llamada solo con ellos.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.NOTIFICACION_FINALISTAS, entidad = "finalista")
    public ResultadoAuditado<ResultadoNotificacionResponse> notificar(UUID actorCodigo,
            NotificarFinalistasRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        LocalDate hoy = LocalDate.now(FinalistaConsultaRepository.ZONA);
        if (!request.plazoEntrega().isAfter(hoy)) {
            throw new ValidationException(FinalistaErrorCodes.ERR_FIN_04,
                    "El plazo de entrega debe ser posterior a hoy");
        }
        List<UUID> codigos = request.finalistas().stream().distinct().toList();
        List<Finalista> finalistas = finalistaRepository.findByCodigoIn(codigos);
        if (finalistas.size() != codigos.size()) {
            throw new NotFoundException(FinalistaErrorCodes.ERR_FIN_01, "Algún finalista seleccionado no existe");
        }

        int enviados = 0;
        int fallidos = 0;
        Instant ahora = Instant.now();
        for (Finalista finalista : finalistas) {
            UUID usuarioCodigo = consulta.codigoUsuarioDePostulacion(finalista.getPostulacionId())
                    .orElseThrow(() -> new NotFoundException(FinalistaErrorCodes.ERR_FIN_01,
                            "Algún finalista seleccionado no existe"));
            Usuario destinatario = usuarioService.obtenerPorCodigo(usuarioCodigo);
            Map<String, String> datos = Map.of(
                    "nombres", destinatario.getNombres(),
                    "plazo", request.plazoEntrega().format(FECHA_AVISO),
                    "lugar", request.lugarHorario().trim(),
                    "documentos", request.documentos().trim());
            if (notificacionService.notificar(destinatario, EventoNotificacion.FINALISTA_NOTIFICADO, datos,
                    finalista.getPostulacionId(), null)) {
                finalista.setPlazoEntrega(request.plazoEntrega());
                finalista.setNotificadoEn(ahora);
                finalistaRepository.save(finalista);
                enviados++;
            } else {
                fallidos++;
            }
        }
        LOG.info("Finalistas notificados", kv("module", "finalistas"), kv("userId", actorCodigo),
                kv("solicitados", codigos.size()), kv("enviados", enviados), kv("fallidos", fallidos));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("finalistas", finalistas.stream().map(f -> f.getCodigo().toString()).toList());
        detalle.put("plazoEntrega", request.plazoEntrega().toString());
        detalle.put("lugarHorario", request.lugarHorario().trim());
        detalle.put("documentos", request.documentos().trim());
        detalle.put("enviados", enviados);
        detalle.put("fallidos", fallidos);
        return new ResultadoAuditado<>(new ResultadoNotificacionResponse(codigos.size(), enviados, fallidos),
                actor.getId(), actor.getCodigo(), null, detalle, true);
    }

    /**
     * Registra (o corrige) la fecha en que el finalista entrego su documentacion. Sin fecha se usa hoy;
     * una fecha futura se rechaza. Corregir una fecha deja la anterior en el detalle de la bitacora.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.ENTREGA_DOCUMENTACION_REGISTRADA, entidad = "finalista")
    public ResultadoAuditado<FinalistaResponse> registrarEntrega(UUID actorCodigo, UUID finalistaCodigo,
            RegistrarEntregaRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        Finalista finalista = finalistaRepository.findByCodigo(finalistaCodigo)
                .orElseThrow(() -> new NotFoundException(FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        LocalDate hoy = LocalDate.now(FinalistaConsultaRepository.ZONA);
        LocalDate fecha = request.fechaRecepcion() == null ? hoy : request.fechaRecepcion();
        if (fecha.isAfter(hoy)) {
            throw new ValidationException(FinalistaErrorCodes.ERR_FIN_03,
                    "La fecha de recepción no puede ser futura");
        }
        LocalDate anterior = finalista.getDocumentacionRecibidaEn() == null ? null
                : finalista.getDocumentacionRecibidaEn().atZone(FinalistaConsultaRepository.ZONA).toLocalDate();

        finalista.setDocumentacionRecibidaEn(fecha.atStartOfDay(FinalistaConsultaRepository.ZONA).toInstant());
        finalistaRepository.saveAndFlush(finalista);
        LOG.info("Entrega de documentacion registrada", kv("module", "finalistas"), kv("userId", actorCodigo),
                kv("correccion", anterior != null));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("finalista", finalista.getCodigo().toString());
        detalle.put("fechaRecepcion", fecha.toString());
        if (anterior != null) {
            detalle.put("fechaAnterior", anterior.toString());
        }
        if (request.observaciones() != null && !request.observaciones().isBlank()) {
            detalle.put("observaciones", request.observaciones().trim());
        }
        FinalistaResponse respuesta = consulta.buscarUno(finalistaCodigo)
                .orElseThrow(() -> new NotFoundException(FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), finalista.getId(), detalle,
                true);
    }

    /** `%texto%` en minusculas con `%`, `_` y `\` escapados; o null si no hay texto. */
    static String patronDeBusqueda(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String texto = search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + texto + "%";
    }
}
