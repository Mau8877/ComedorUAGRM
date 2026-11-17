package com.comedoruagrm.backend.features.convocatorias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.mapper.ConvocatoriaMapper;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.AuthErrorCodes;

import lombok.RequiredArgsConstructor;

/**
 * Cambios de estado de una convocatoria (CU13): abrir (BORRADOR a ABIERTA), cerrar (ABIERTA a CERRADA) y el
 * cierre automatico al llegar la fecha de cierre.
 *
 * * Solo puede haber una convocatoria ABIERTA por gestion: se comprueba antes y, ademas, se traduce la violacion
 * * del indice unico parcial (dos aperturas simultaneas) al mismo 409.
 * * Cerrar solo cambia el estado: los borradores de postulacion quedan bloqueados porque el envio exige una
 * * convocatoria ABIERTA (CU02 y CU17), no porque este servicio los toque.
 */
@Service
@RequiredArgsConstructor
public class ConvocatoriaEstadoService {

    private static final Logger LOG = LoggerFactory.getLogger(ConvocatoriaEstadoService.class);

    private final ConvocatoriaRepository convocatoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConvocatoriaMapper mapper;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.APERTURA_CONVOCATORIA, entidad = "convocatoria")
    public ResultadoAuditado<ConvocatoriaResponse> abrir(UUID actorCodigo, UUID codigo) {
        Usuario actor = actor(actorCodigo);
        Convocatoria convocatoria = buscar(codigo);

        if (convocatoria.getEstado() != EstadoConvocatoria.BORRADOR) {
            throw rechazar("Apertura de convocatoria rechazada", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_08, "Solo se puede abrir una convocatoria en borrador"));
        }
        if (convocatoriaRepository.existsByGestionIdAndEstadoAndIdNot(convocatoria.getGestion().getId(),
                EstadoConvocatoria.ABIERTA, convocatoria.getId())) {
            throw rechazar("Apertura de convocatoria rechazada", otraAbierta());
        }

        convocatoria.setEstado(EstadoConvocatoria.ABIERTA);
        try {
            convocatoriaRepository.saveAndFlush(convocatoria);
        } catch (DataIntegrityViolationException e) {
            throw traducirConflicto(e);
        }

        LOG.info("Convocatoria abierta", kv("module", "convocatorias"),
                kv("convocatoriaId", convocatoria.getCodigo()), kv("estado", EstadoConvocatoria.ABIERTA));
        return new ResultadoAuditado<>(mapper.detalle(convocatoria), actor.getId(), actor.getCodigo(),
                convocatoria.getId(),
                Map.of("estadoAnterior", EstadoConvocatoria.BORRADOR.name(), "estadoNuevo",
                        EstadoConvocatoria.ABIERTA.name()),
                true);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.CIERRE_CONVOCATORIA, entidad = "convocatoria")
    public ResultadoAuditado<ConvocatoriaResponse> cerrar(UUID actorCodigo, UUID codigo) {
        Usuario actor = actor(actorCodigo);
        Convocatoria convocatoria = buscar(codigo);

        if (convocatoria.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw rechazar("Cierre de convocatoria rechazado", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_08, "Solo se puede cerrar una convocatoria abierta"));
        }

        convocatoria.setEstado(EstadoConvocatoria.CERRADA);
        convocatoriaRepository.saveAndFlush(convocatoria);

        LOG.info("Convocatoria cerrada", kv("module", "convocatorias"),
                kv("convocatoriaId", convocatoria.getCodigo()), kv("estado", EstadoConvocatoria.CERRADA));
        return new ResultadoAuditado<>(mapper.detalle(convocatoria), actor.getId(), actor.getCodigo(),
                convocatoria.getId(),
                Map.of("estadoAnterior", EstadoConvocatoria.ABIERTA.name(), "estadoNuevo",
                        EstadoConvocatoria.CERRADA.name()),
                true);
    }

    /**
     * Cierra una convocatoria ABIERTA cuya fecha de cierre ya paso (A5). La llama solo el job de cierre
     * automatico, cada una en su propia transaccion; el actor de la bitacora es nulo porque lo hace el sistema.
     *
     * ! Sin `@PreAuthorize` a proposito: ningun controller lo expone y nunca debe llegar por HTTP.
     * * Si otra instancia o el Administrador ya la cerraron, no hace nada ni anota nada.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditar(accion = BitacoraAcciones.CIERRE_AUTOMATICO_CONVOCATORIA, entidad = "convocatoria")
    public ResultadoAuditado<Void> cerrarVencida(Long convocatoriaId) {
        Convocatoria convocatoria = convocatoriaRepository.findById(convocatoriaId).orElse(null);
        if (convocatoria == null || convocatoria.getEstado() != EstadoConvocatoria.ABIERTA
                || convocatoria.getFechaCierre().isAfter(Instant.now())) {
            return new ResultadoAuditado<>(null, null, null, convocatoriaId, Map.of(), false);
        }

        convocatoria.setEstado(EstadoConvocatoria.CERRADA);
        convocatoriaRepository.saveAndFlush(convocatoria);

        LOG.info("Convocatoria cerrada automáticamente", kv("module", "convocatorias"),
                kv("convocatoriaId", convocatoria.getCodigo()), kv("estado", EstadoConvocatoria.CERRADA));
        return new ResultadoAuditado<>(null, null, null, convocatoria.getId(),
                Map.of("estadoAnterior", EstadoConvocatoria.ABIERTA.name(), "estadoNuevo",
                        EstadoConvocatoria.CERRADA.name(), "motivo", "fecha_cierre"),
                true);
    }

    // ---- Ayudas ----

    /** El administrador que actua, resuelto desde el `sub` del token (nunca desde el body). */
    private Usuario actor(UUID actorCodigo) {
        return usuarioRepository.findByCodigo(actorCodigo)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_02,
                        "Tu cuenta no está registrada en el sistema."));
    }

    private Convocatoria buscar(UUID codigo) {
        return convocatoriaRepository.findByCodigo(codigo)
                .orElseThrow(() -> rechazar("Convocatoria no encontrada", new NotFoundException(
                        ConvocatoriaErrorCodes.ERR_CV_01, "La convocatoria indicada no existe")));
    }

    private ConflictException otraAbierta() {
        return new ConflictException(ConvocatoriaErrorCodes.ERR_CV_07,
                "Ya hay otra convocatoria abierta en esa gestión académica");
    }

    // Dos aperturas simultaneas pueden chocar recien contra el indice unico parcial: mismo 409 que habria dado la
    // verificacion. Una violacion de otro indice sube tal cual (termina en el 500 generico).
    private RuntimeException traducirConflicto(DataIntegrityViolationException e) {
        String causa = String.valueOf(e.getMostSpecificCause().getMessage());
        if (causa.contains("uq_convocatoria_abierta_por_gestion")) {
            return rechazar("Apertura de convocatoria rechazada", otraAbierta());
        }
        return e;
    }

    // WARN sin datos libres: solo el codigo de error.
    private <T extends BusinessException> T rechazar(String mensaje, T excepcion) {
        LOG.warn(mensaje, kv("module", "convocatorias"), kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
