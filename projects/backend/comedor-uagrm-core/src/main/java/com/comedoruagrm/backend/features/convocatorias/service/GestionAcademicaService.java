package com.comedoruagrm.backend.features.convocatorias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaRequest;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaResponse;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.convocatorias.repository.GestionAcademicaRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.AuthErrorCodes;

import lombok.RequiredArgsConstructor;

/**
 * Gestiones academicas (CU13): listar, crear y editar. Solo el Administrador DUBSS. No se eliminan.
 *
 * * Cada alta y edicion se anota en la bitacora con `@Auditar` y el administrador como actor.
 */
@Service
@RequiredArgsConstructor
public class GestionAcademicaService {

    private static final Logger LOG = LoggerFactory.getLogger(GestionAcademicaService.class);

    // Campo del `sort` (camelCase del DTO) -> propiedad del Model.
    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "etiqueta", "etiqueta",
            "fechaInicio", "fechaInicio",
            "fechaFin", "fechaFin");

    private static final String ORDEN_POR_DEFECTO = "-fechaInicio";

    private final GestionAcademicaRepository gestionRepository;
    private final UsuarioRepository usuarioRepository;

    /** Una pagina de gestiones junto con su bloque `meta`. */
    public record PaginaGestiones(List<GestionAcademicaResponse> datos, PageMeta meta) {
    }

    /** `page` es 1-indexed. Las lecturas no se loguean. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaGestiones listar(int page, int pageSize, String search, String sort) {
        Page<GestionAcademica> pagina = gestionRepository.buscar(ListadoConvocatorias.patron(search),
                ListadoConvocatorias.pagina(page, pageSize, sort, CAMPOS_ORDENABLES, ORDEN_POR_DEFECTO));
        return new PaginaGestiones(pagina.getContent().stream().map(GestionAcademicaResponse::de).toList(),
                new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages()));
    }

    /** Orden: fechas coherentes (400), etiqueta libre (409), y recien entonces se guarda. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.CREACION_GESTION, entidad = "gestion_academica")
    public ResultadoAuditado<GestionAcademicaResponse> crear(UUID actorCodigo, GestionAcademicaRequest request) {
        Usuario actor = actor(actorCodigo);
        String etiqueta = request.etiqueta().trim();
        validarFechas(request);
        if (gestionRepository.existsByEtiqueta(etiqueta)) {
            throw rechazar("Alta de gestión rechazada", etiquetaRepetida());
        }

        GestionAcademica gestion;
        try {
            gestion = gestionRepository.saveAndFlush(
                    new GestionAcademica(etiqueta, request.fechaInicio(), request.fechaFin()));
        } catch (DataIntegrityViolationException e) {
            throw traducirConflicto(e);
        }

        LOG.info("Gestión académica creada", kv("module", "convocatorias"), kv("gestionId", gestion.getCodigo()));
        return new ResultadoAuditado<>(GestionAcademicaResponse.de(gestion), actor.getId(), actor.getCodigo(),
                gestion.getId(), Map.of("etiqueta", etiqueta), true);
    }

    /** Reemplaza los tres campos (PUT). Si no cambia nada no se anota una edicion vacia. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.EDICION_GESTION, entidad = "gestion_academica")
    public ResultadoAuditado<GestionAcademicaResponse> editar(UUID actorCodigo, UUID codigo,
            GestionAcademicaRequest request) {
        Usuario actor = actor(actorCodigo);
        GestionAcademica gestion = gestionRepository.findByCodigo(codigo)
                .orElseThrow(() -> rechazar("Gestión académica no encontrada", new NotFoundException(
                        ConvocatoriaErrorCodes.ERR_CV_02, "La gestión académica indicada no existe")));
        String etiqueta = request.etiqueta().trim();
        validarFechas(request);

        List<String> modificados = new ArrayList<>();
        if (!etiqueta.equals(gestion.getEtiqueta())) {
            if (gestionRepository.existsByEtiquetaAndIdNot(etiqueta, gestion.getId())) {
                throw rechazar("Edición de gestión rechazada", etiquetaRepetida());
            }
            gestion.setEtiqueta(etiqueta);
            modificados.add("etiqueta");
        }
        if (!request.fechaInicio().equals(gestion.getFechaInicio())) {
            gestion.setFechaInicio(request.fechaInicio());
            modificados.add("fechaInicio");
        }
        if (!request.fechaFin().equals(gestion.getFechaFin())) {
            gestion.setFechaFin(request.fechaFin());
            modificados.add("fechaFin");
        }

        if (!modificados.isEmpty()) {
            try {
                gestionRepository.saveAndFlush(gestion);
            } catch (DataIntegrityViolationException e) {
                throw traducirConflicto(e);
            }
            LOG.info("Gestión académica editada", kv("module", "convocatorias"),
                    kv("gestionId", gestion.getCodigo()), kv("campos", modificados));
        }
        return new ResultadoAuditado<>(GestionAcademicaResponse.de(gestion), actor.getId(), actor.getCodigo(),
                gestion.getId(), Map.of("camposModificados", modificados), !modificados.isEmpty());
    }

    // ---- Ayudas ----

    /** El administrador que actua, resuelto desde el `sub` del token (nunca desde el body). */
    private Usuario actor(UUID actorCodigo) {
        return usuarioRepository.findByCodigo(actorCodigo)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_02,
                        "Tu cuenta no está registrada en el sistema."));
    }

    private void validarFechas(GestionAcademicaRequest request) {
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw rechazar("Gestión rechazada: fechas incoherentes", new ValidationException(
                    ConvocatoriaErrorCodes.ERR_CV_13, "La fecha de fin debe ser posterior a la de inicio"));
        }
    }

    private ConflictException etiquetaRepetida() {
        return new ConflictException(ConvocatoriaErrorCodes.ERR_CV_12, "Ya existe una gestión con esa etiqueta");
    }

    // Dos altas simultaneas pueden chocar recien contra el indice unico: mismo 409 que habria dado la verificacion.
    private RuntimeException traducirConflicto(DataIntegrityViolationException e) {
        String causa = String.valueOf(e.getMostSpecificCause().getMessage());
        if (causa.contains("uq_gestion_academica_etiqueta")) {
            return rechazar("Gestión rechazada: etiqueta repetida", etiquetaRepetida());
        }
        return e;
    }

    // WARN sin datos libres: solo el codigo de error.
    private <T extends BusinessException> T rechazar(String mensaje, T excepcion) {
        LOG.warn(mensaje, kv("module", "convocatorias"), kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
