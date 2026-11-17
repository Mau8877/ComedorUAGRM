package com.comedoruagrm.backend.features.convocatorias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaRequest;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResumenResponse;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.mapper.ConvocatoriaMapper;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;
import com.comedoruagrm.backend.features.convocatorias.repository.GestionAcademicaRepository;
import com.comedoruagrm.backend.features.postulaciones.service.PostulacionesDeConvocatoriaService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.AuthErrorCodes;

import lombok.RequiredArgsConstructor;

/**
 * Gestion de convocatorias (CU13): listar, consultar, crear, editar y eliminar. Abrir y cerrar estan en
 * {@link ConvocatoriaEstadoService}. Solo el Administrador DUBSS.
 *
 * * Una convocatoria nace en BORRADOR. Se edita en BORRADOR o ABIERTA; una CERRADA o PUBLICADA no se modifica,
 * * y solo se elimina una BORRADOR sin postulaciones (borrado logico).
 * * Cada operacion que escribe se anota en la bitacora con `@Auditar` y el administrador como actor.
 * ! `cantidadFinalistas` solo la ve el Administrador: este servicio solo lo usan endpoints de Administrador.
 */
@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

    private static final Logger LOG = LoggerFactory.getLogger(ConvocatoriaService.class);

    // Campo del `sort` (camelCase del DTO) -> propiedad del Model.
    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "nombre", "nombre",
            "fechaApertura", "fechaApertura",
            "fechaCierre", "fechaCierre",
            "estado", "estado",
            "creadoEn", "creadoEn");

    private static final String ORDEN_POR_DEFECTO = "-creadoEn";

    private final ConvocatoriaRepository convocatoriaRepository;
    private final GestionAcademicaRepository gestionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentosConvocatoriaService documentos;
    private final PostulacionesDeConvocatoriaService postulaciones;
    private final ConvocatoriaMapper mapper;

    /** Una pagina de convocatorias junto con su bloque `meta`. */
    public record PaginaConvocatorias(List<ConvocatoriaResumenResponse> datos, PageMeta meta) {
    }

    // ---- Consultar ----

    /** `page` es 1-indexed. Las lecturas no se loguean. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaConvocatorias listar(int page, int pageSize, String search, EstadoConvocatoria estado,
            UUID gestion, String sort) {
        Page<Convocatoria> pagina = convocatoriaRepository.buscar(ListadoConvocatorias.patron(search), estado,
                gestion, ListadoConvocatorias.pagina(page, pageSize, sort, CAMPOS_ORDENABLES, ORDEN_POR_DEFECTO));
        return new PaginaConvocatorias(pagina.getContent().stream().map(ConvocatoriaResumenResponse::de).toList(),
                new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages()));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ConvocatoriaResponse obtener(UUID codigo) {
        return mapper.detalle(buscar(codigo));
    }

    // ---- Crear ----

    /**
     * Crea la convocatoria en BORRADOR con su lista de documentos. Orden: fechas (400), gestion (404),
     * documentos (400/404) y recien entonces se guarda. No se comprueba «otra abierta»: eso se aplica al abrir.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.CREACION_CONVOCATORIA, entidad = "convocatoria")
    public ResultadoAuditado<ConvocatoriaResponse> crear(UUID actorCodigo, ConvocatoriaRequest request) {
        Usuario actor = actor(actorCodigo);
        validarFechas(request);
        GestionAcademica gestion = buscarGestion(request.gestionCodigo());
        Map<TipoDocumento, Boolean> requeridos = documentos.validar(request.documentos());

        Convocatoria convocatoria = convocatoriaRepository.saveAndFlush(new Convocatoria(gestion, actor.getId(),
                request.nombre().trim(), FechasConvocatoria.aperturaDesde(request.fechaApertura()),
                FechasConvocatoria.cierreHasta(request.fechaCierre()), request.cupos(), request.puntajeMinimo(),
                request.cantidadFinalistas()));
        documentos.reemplazar(convocatoria, requeridos);

        LOG.info("Convocatoria creada", kv("module", "convocatorias"),
                kv("convocatoriaId", convocatoria.getCodigo()));
        return new ResultadoAuditado<>(mapper.detalle(convocatoria), actor.getId(), actor.getCodigo(),
                convocatoria.getId(),
                Map.of("gestion", gestion.getCodigo().toString(), "cantidadDocumentos", requeridos.size()), true);
    }

    // ---- Editar ----

    /**
     * Reemplaza los datos de la convocatoria (PUT: los opcionales omitidos quedan vacios). Solo en BORRADOR o
     * ABIERTA (409 `ERR_CV_08`); la gestion solo cambia en BORRADOR; con postulaciones no cambian la fecha
     * de apertura ni los documentos (409 `ERR_CV_09`). Si no cambia nada no se anota una edicion vacia.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.EDICION_CONVOCATORIA, entidad = "convocatoria")
    public ResultadoAuditado<ConvocatoriaResponse> editar(UUID actorCodigo, UUID codigo,
            ConvocatoriaRequest request) {
        Usuario actor = actor(actorCodigo);
        Convocatoria convocatoria = buscar(codigo);

        if (convocatoria.getEstado() != EstadoConvocatoria.BORRADOR
                && convocatoria.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw rechazar("Edición de convocatoria rechazada", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_08, "Solo se puede editar una convocatoria en borrador o abierta"));
        }
        validarFechas(request);
        GestionAcademica gestion = buscarGestion(request.gestionCodigo());
        boolean cambiaGestion = !gestion.getId().equals(convocatoria.getGestion().getId());
        if (cambiaGestion && convocatoria.getEstado() != EstadoConvocatoria.BORRADOR) {
            throw rechazar("Edición de convocatoria rechazada", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_08, "La gestión solo se puede cambiar en una convocatoria en borrador"));
        }
        Map<TipoDocumento, Boolean> requeridos = documentos.validar(request.documentos());

        Instant apertura = FechasConvocatoria.aperturaDesde(request.fechaApertura());
        Instant cierre = FechasConvocatoria.cierreHasta(request.fechaCierre());
        boolean cambiaApertura = !apertura.equals(convocatoria.getFechaApertura());
        boolean cambiaDocumentos = documentos.difiere(convocatoria, requeridos);
        if ((cambiaApertura || cambiaDocumentos) && postulaciones.existenPara(convocatoria.getId())) {
            throw rechazar("Edición de convocatoria rechazada", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_09,
                    "La convocatoria ya tiene postulaciones: no se pueden cambiar la fecha de apertura ni los documentos"));
        }

        List<String> modificados = new ArrayList<>();
        String nombre = request.nombre().trim();
        if (!nombre.equals(convocatoria.getNombre())) {
            convocatoria.setNombre(nombre);
            modificados.add("nombre");
        }
        if (cambiaGestion) {
            convocatoria.setGestion(gestion);
            modificados.add("gestion");
        }
        if (cambiaApertura) {
            convocatoria.setFechaApertura(apertura);
            modificados.add("fechaApertura");
        }
        if (!cierre.equals(convocatoria.getFechaCierre())) {
            convocatoria.setFechaCierre(cierre);
            modificados.add("fechaCierre");
        }
        if (!Objects.equals(request.cupos(), convocatoria.getCupos())) {
            convocatoria.setCupos(request.cupos());
            modificados.add("cupos");
        }
        if (!iguales(request.puntajeMinimo(), convocatoria.getPuntajeMinimo())) {
            convocatoria.setPuntajeMinimo(request.puntajeMinimo());
            modificados.add("puntajeMinimo");
        }
        if (!Objects.equals(request.cantidadFinalistas(), convocatoria.getCantidadFinalistas())) {
            convocatoria.setCantidadFinalistas(request.cantidadFinalistas());
            modificados.add("cantidadFinalistas");
        }
        if (!modificados.isEmpty()) {
            convocatoriaRepository.saveAndFlush(convocatoria);
        }
        if (cambiaDocumentos) {
            documentos.reemplazar(convocatoria, requeridos);
            modificados.add("documentos");
        }

        if (!modificados.isEmpty()) {
            LOG.info("Convocatoria editada", kv("module", "convocatorias"),
                    kv("convocatoriaId", convocatoria.getCodigo()), kv("campos", modificados));
        }
        return new ResultadoAuditado<>(mapper.detalle(convocatoria), actor.getId(), actor.getCodigo(),
                convocatoria.getId(), Map.of("camposModificados", modificados), !modificados.isEmpty());
    }

    // ---- Eliminar ----

    /**
     * Elimina (borrado logico) una convocatoria en BORRADOR sin postulaciones, junto con sus documentos. Con
     * postulaciones nunca se elimina: solo se cierra (409 `ERR_CV_10`, se comprueba primero).
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.ELIMINACION_CONVOCATORIA, entidad = "convocatoria")
    public ResultadoAuditado<Void> eliminar(UUID actorCodigo, UUID codigo) {
        Usuario actor = actor(actorCodigo);
        Convocatoria convocatoria = buscar(codigo);

        if (postulaciones.existenPara(convocatoria.getId())) {
            throw rechazar("Eliminación de convocatoria rechazada", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_10,
                    "La convocatoria tiene postulaciones: no se puede eliminar, solo cerrar"));
        }
        if (convocatoria.getEstado() != EstadoConvocatoria.BORRADOR) {
            throw rechazar("Eliminación de convocatoria rechazada", new ConflictException(
                    ConvocatoriaErrorCodes.ERR_CV_08, "Solo se puede eliminar una convocatoria en borrador"));
        }

        documentos.eliminarTodos(convocatoria);
        convocatoria.setEliminadoEn(Instant.now());
        convocatoriaRepository.saveAndFlush(convocatoria);

        LOG.info("Convocatoria eliminada", kv("module", "convocatorias"),
                kv("convocatoriaId", convocatoria.getCodigo()));
        return new ResultadoAuditado<>(null, actor.getId(), actor.getCodigo(), convocatoria.getId(),
                Map.of("gestion", convocatoria.getGestion().getCodigo().toString()), true);
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

    private GestionAcademica buscarGestion(UUID codigo) {
        return gestionRepository.findByCodigo(codigo)
                .orElseThrow(() -> rechazar("Gestión académica no encontrada", new NotFoundException(
                        ConvocatoriaErrorCodes.ERR_CV_02, "La gestión académica indicada no existe")));
    }

    private void validarFechas(ConvocatoriaRequest request) {
        if (!request.fechaCierre().isAfter(request.fechaApertura())) {
            throw rechazar("Convocatoria rechazada: fechas incoherentes", new ValidationException(
                    ConvocatoriaErrorCodes.ERR_CV_04, "La fecha de cierre debe ser posterior a la de apertura"));
        }
    }

    // Los BigDecimal se comparan por valor (2 y 2.00 son el mismo puntaje).
    private boolean iguales(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    // WARN sin datos libres: solo el codigo de error.
    private <T extends BusinessException> T rechazar(String mensaje, T excepcion) {
        LOG.warn(mensaje, kv("module", "convocatorias"), kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
