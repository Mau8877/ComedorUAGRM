package com.comedoruagrm.backend.features.comunicados.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.comedoruagrm.backend.features.comunicados.dto.ComunicadoResponse;
import com.comedoruagrm.backend.features.comunicados.dto.PublicarComunicadoRequest;
import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;
import com.comedoruagrm.backend.features.comunicados.exception.ComunicadoErrorCodes;
import com.comedoruagrm.backend.features.comunicados.model.Comunicado;
import com.comedoruagrm.backend.features.comunicados.repository.ComunicadoConsultaRepository;
import com.comedoruagrm.backend.features.comunicados.repository.ComunicadoRepository;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Comunicados (CU21, flujo C): publica un comunicado, deja un aviso en la bandeja de cada destinatario
 * (con push, sin correo) y lo anota en la bitacora. Solo el Administrador DUBSS.
 *
 * * Los avisos se guardan por lotes en la misma transaccion y el despacho (push) es el asincrono de
 * * CU-EXTRA-01: la respuesta no espera la entrega. No cambia el estado de ninguna postulacion.
 * * Un comunicado publicado no se edita ni se borra.
 * ! La bitacora lleva el codigo, los destinatarios y la cantidad, no el contenido.
 */
@Service
@RequiredArgsConstructor
public class ComunicadoService {

    private static final Logger LOG = LoggerFactory.getLogger(ComunicadoService.class);

    private final ComunicadoRepository comunicadoRepository;
    private final ComunicadoConsultaRepository consulta;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    /** Una pagina de comunicados junto con su bloque `meta`. */
    public record PaginaComunicados(List<ComunicadoResponse> datos, PageMeta meta) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.COMUNICADO_PUBLICADO, entidad = "comunicado")
    public ResultadoAuditado<ComunicadoResponse> publicar(UUID actorCodigo, PublicarComunicadoRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DestinatariosComunicado tipo = request.destinatarios();
        if (tipo == DestinatariosComunicado.ADJUDICADOS) {
            throw new ValidationException(ComunicadoErrorCodes.ERR_COM_01,
                    "Por ahora los comunicados solo pueden dirigirse a todos los postulantes o a los finalistas");
        }
        if (tipo == DestinatariosComunicado.FINALISTAS && request.convocatoriaId() == null) {
            throw new ValidationException(ComunicadoErrorCodes.ERR_COM_02,
                    "Para avisar a los finalistas indica la convocatoria");
        }
        Long convocatoriaId = request.convocatoriaId() == null ? null
                : consulta.buscarConvocatoriaId(request.convocatoriaId())
                        .orElseThrow(() -> new NotFoundException(FinalistaErrorCodes.ERR_FIN_02,
                                "La convocatoria no existe"));
        List<Long> destinatarios = consulta.destinatarios(tipo, convocatoriaId);
        if (destinatarios.isEmpty()) {
            throw new ValidationException(ComunicadoErrorCodes.ERR_COM_01,
                    "No hay destinatarios para este comunicado");
        }

        String titulo = request.titulo().trim();
        String contenido = request.contenido().trim();
        Comunicado comunicado = comunicadoRepository.save(
                new Comunicado(convocatoriaId, actor.getId(), titulo, contenido, tipo, Instant.now()));
        int total = notificacionService.notificarUsuarios(destinatarios, EventoNotificacion.COMUNICADO,
                Map.of("titulo", titulo, "contenido", contenido), comunicado.getId());
        LOG.info("Comunicado publicado", kv("module", "comunicados"), kv("userId", actorCodigo),
                kv("destinatarios", tipo.name()), kv("total", total));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("comunicado", comunicado.getCodigo().toString());
        detalle.put("destinatarios", tipo.name());
        detalle.put("convocatoria", request.convocatoriaId() == null ? null : request.convocatoriaId().toString());
        detalle.put("totalDestinatarios", total);
        ComunicadoResponse respuesta = new ComunicadoResponse(comunicado.getCodigo(), request.convocatoriaId(),
                titulo, contenido, tipo, comunicado.getPublicadoEn(), total);
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), comunicado.getId(), detalle,
                true);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaComunicados listar(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        long total = consulta.contar();
        long desplazamiento = (long) (page - 1) * pageSize;
        List<ComunicadoResponse> datos = desplazamiento >= total ? List.of()
                : consulta.listar(pageSize, desplazamiento);
        int totalPaginas = (int) ((total + pageSize - 1) / pageSize);
        return new PaginaComunicados(datos, new PageMeta(page, pageSize, total, totalPaginas));
    }
}
