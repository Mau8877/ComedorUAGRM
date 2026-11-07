package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.notificaciones.dto.NotificacionResponse;
import com.comedoruagrm.backend.features.notificaciones.dto.ResumenNotificacionesResponse;
import com.comedoruagrm.backend.features.notificaciones.exception.NotificacionErrorCodes;
import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Bandeja de avisos del usuario autenticado (contrato de docs/pendientes/NOTIFICACIONES_WEB.md).
 *
 * * Todo opera sobre el usuario del token: nunca se ve ni se toca el aviso de otro. Marcar como leido
 * * o consultar el resumen no se anota en la bitacora (son lecturas del propio usuario).
 * * `sort`: solo `fecha` o `-fecha` (el default es `-fecha`, el mas nuevo primero).
 */
@Service
@RequiredArgsConstructor
public class BandejaNotificacionesService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioService usuarioService;

    /** Una pagina de avisos junto con su bloque `meta`. */
    public record PaginaNotificaciones(List<NotificacionResponse> datos, PageMeta meta) {
    }

    @Transactional(readOnly = true)
    public PaginaNotificaciones listar(UUID usuarioCodigo, int page, int pageSize, String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        Sort.Direction direccion = direccionDe(sort);
        Long usuarioId = usuarioService.obtenerPorCodigo(usuarioCodigo).getId();
        Page<Notificacion> pagina = notificacionRepository.findByUsuarioId(usuarioId,
                PageRequest.of(page - 1, pageSize, Sort.by(direccion, "creadoEn").and(Sort.by(direccion, "id"))));
        List<NotificacionResponse> datos = pagina.getContent().stream()
                .map(BandejaNotificacionesService::aRespuesta).toList();
        return new PaginaNotificaciones(datos,
                new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ResumenNotificacionesResponse resumir(UUID usuarioCodigo) {
        Long usuarioId = usuarioService.obtenerPorCodigo(usuarioCodigo).getId();
        return new ResumenNotificacionesResponse(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId));
    }

    /** Idempotente: marcar leido uno ya leido no falla. 404 si no existe o es de otro usuario. */
    @Transactional
    public void marcarLeida(UUID usuarioCodigo, UUID notificacionCodigo) {
        Long usuarioId = usuarioService.obtenerPorCodigo(usuarioCodigo).getId();
        Notificacion aviso = notificacionRepository.findByCodigoAndUsuarioId(notificacionCodigo, usuarioId)
                .orElseThrow(() -> new NotFoundException(NotificacionErrorCodes.ERR_NOT_01,
                        "La notificación no existe"));
        if (!aviso.isLeida()) {
            aviso.setLeida(true);
            notificacionRepository.save(aviso);
        }
    }

    @Transactional
    public void marcarTodasLeidas(UUID usuarioCodigo) {
        notificacionRepository.marcarTodasLeidas(usuarioService.obtenerPorCodigo(usuarioCodigo).getId());
    }

    @Transactional
    public void eliminarTodas(UUID usuarioCodigo) {
        notificacionRepository.eliminarTodas(usuarioService.obtenerPorCodigo(usuarioCodigo).getId());
    }

    private static Sort.Direction direccionDe(String sort) {
        if (sort == null || sort.isBlank() || sort.equals("-fecha")) {
            return Sort.Direction.DESC;
        }
        if (sort.equals("fecha")) {
            return Sort.Direction.ASC;
        }
        throw new ValidationException(NotificacionErrorCodes.ERR_NOT_02,
                "La bandeja solo se puede ordenar por «fecha» (o «-fecha»)");
    }

    private static NotificacionResponse aRespuesta(Notificacion aviso) {
        return new NotificacionResponse(aviso.getCodigo(), aviso.getTipo(), aviso.getTitulo(), aviso.getMensaje(),
                aviso.getCreadoEn(), aviso.isLeida());
    }
}
