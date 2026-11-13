package com.comedoruagrm.backend.features.postulaciones.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.ConvocatoriaResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.FinalistaPostulanteResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.GestionAcademicaResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionDetalleResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ObservacionesResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PasoLineaTiempoResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.enums.SituacionPaso;
import com.comedoruagrm.backend.features.postulaciones.exception.MisPostulacionesErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.CabeceraDetalle;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.DatosFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaListado;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.Pagina;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * «Mis postulaciones» (CU04): el postulante consulta sus postulaciones enviadas y el detalle de cada una.
 *
 * * Solo lectura: ningun metodo escribe, anota en la bitacora, loguea lecturas ni marca avisos como leidos.
 * * El postulante sale del token (nunca de la URL ni del body) y solo ve las suyas; una postulacion ajena
 * * responde igual que una inexistente.
 * * Los borradores no se listan. El estado que se muestra es el visible (ver {@link EstadoVisible}): la decision
 * * final del CU05 se ve «calificada» hasta publicar los resultados (CU22).
 * ! Ni el listado ni el detalle llevan ids internos, CI, correo ni quien hizo un cambio.
 */
@Service
@RequiredArgsConstructor
public class MisPostulacionesService {

    private static final String ERR_SYS_05 = "ERR_SYS_05";
    private static final String CAMPO_ORDEN = "fechaEnvio";

    /** Pasos que aun faltan y no son un estado: la auditoria del expediente y el resultado de la adjudicacion. */
    static final String PASO_RESULTADO = "resultado";

    /** Lo que antecede a los documentos en el aviso de finalista (plantilla `FINALISTA_NOTIFICADO`, CU21). */
    private static final String MARCA_DOCUMENTOS = "Documentos a presentar: ";

    private final MisPostulacionesConsultaRepository consulta;
    private final UsuarioService usuarioService;

    /**
     * Una pagina de las postulaciones enviadas del postulante. `gestionAcademica` es el codigo publico de la
     * gestion (filtro opcional). `sort` solo admite `fechaEnvio`, por defecto de la mas reciente a la mas
     * antigua; un `sort` o filtro no soportado es `ERR_SYS_05`.
     */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public Page<MiPostulacionResumenResponse> listar(UUID usuarioCodigo, String gestionAcademica,
            Pageable pageable) {
        boolean ascendente = esAscendente(pageable.getSort());
        UUID gestion = parsearGestion(gestionAcademica);
        long usuarioId = usuarioService.obtenerPorCodigo(usuarioCodigo).getId();

        Pagina<FilaListado> pagina = consulta.listar(usuarioId, gestion, ascendente, pageable);
        List<MiPostulacionResumenResponse> resumenes = pagina.filas().stream().map(MisPostulacionesService::resumen)
                .toList();
        return new PageImpl<>(resumenes, pageable, pagina.total());
    }


    /**
     * El detalle de una postulacion propia: estado visible, linea de tiempo (pasada y pendiente) e informacion
     * asociada al estado. Una postulacion inexistente, ajena o en borrador es `ERR_POS_01`, con el mismo mensaje.
     */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public MiPostulacionDetalleResponse detalle(UUID usuarioCodigo, UUID postulacionCodigo) {
        long usuarioId = usuarioService.obtenerPorCodigo(usuarioCodigo).getId();
        CabeceraDetalle cabecera = consulta.cabecera(usuarioId, postulacionCodigo).orElseThrow(
                () -> new NotFoundException(MisPostulacionesErrorCodes.ERR_POS_01, "La postulación no existe"));
        FilaListado fila = cabecera.fila();
        boolean visibles = resultadosVisibles(fila);
        EstadoPostulacion visible = EstadoVisible.calcular(EstadoPostulacion.valueOf(fila.estado()), visibles);
        List<FilaHistorial> historial = consulta.historial(cabecera.id());

        Optional<DatosFinalista> finalista = consulta.finalista(cabecera.id());
        boolean publicadaConDecision = fila.resultadosPublicados() && fila.decisionFinal();
        String resultado = publicadaConDecision ? consulta.decisionFinal(cabecera.id()).orElse(null) : null;

        return new MiPostulacionDetalleResponse(fila.codigo(),
                new ConvocatoriaResumenResponse(fila.convocatoriaCodigo(), fila.convocatoriaNombre()),
                new GestionAcademicaResumenResponse(fila.gestionCodigo(), fila.gestionNombre()), fila.fechaEnvio(),
                visible.name(), fila.numeroEnvios(), lineaDeTiempo(historial, visible, visibles),
                observaciones(historial, visible), motivo(historial, visible, fila.decisionFinal()),
                finalista.map(f -> bloqueFinalista(f, cabecera.id())).orElse(null), resultado,
                visible == EstadoPostulacion.devuelta);
    }

    /** Los pasos que ocurrieron (del historial real, sin los ocultos) y los que aun faltan segun el estado visible. */
    private static List<PasoLineaTiempoResponse> lineaDeTiempo(List<FilaHistorial> historial,
            EstadoPostulacion visible, boolean resultadosVisibles) {
        List<PasoLineaTiempoResponse> pasos = new ArrayList<>();
        for (FilaHistorial fila : historial) {
            if (!EstadoVisible.ocultaFila(EstadoPostulacion.valueOf(fila.estadoNuevo()), resultadosVisibles)) {
                pasos.add(new PasoLineaTiempoResponse(fila.estadoNuevo(), fila.fecha(), SituacionPaso.COMPLETADO));
            }
        }
        for (String pendiente : pasosPendientes(visible)) {
            pasos.add(new PasoLineaTiempoResponse(pendiente, null, SituacionPaso.PENDIENTE));
        }
        return pasos;
    }

    /** Los pasos futuros: nunca se inventa uno pasado. Los estados finales no tienen ninguno. */
    private static List<String> pasosPendientes(EstadoPostulacion visible) {
        return switch (visible) {
            case borrador, pendiente -> List.of(EstadoPostulacion.calificada.name(), PASO_RESULTADO);
            case devuelta -> List.of(EstadoPostulacion.pendiente.name(), EstadoPostulacion.calificada.name(),
                    PASO_RESULTADO);
            case calificada -> List.of(PASO_RESULTADO);
            case archivada, adjudicada, no_adjudicada -> List.of();
        };
    }

    /** Las observaciones de la ultima devolucion, solo si la postulacion esta «devuelta». */
    private static ObservacionesResponse observaciones(List<FilaHistorial> historial, EstadoPostulacion visible) {
        if (visible != EstadoPostulacion.devuelta) {
            return null;
        }
        return ultimaFila(historial, EstadoPostulacion.devuelta)
                .map(f -> new ObservacionesResponse(f.motivo() == null ? "" : f.motivo(), f.fecha())).orElse(null);
    }

    /**
     * El motivo del archivo o del rechazo del CU20. Un «no adjudicada» con fila de `adjudicacion` es la decision
     * final del CU05: su motivo lo ve solo el Administrador y nunca se devuelve.
     */
    private static String motivo(List<FilaHistorial> historial, EstadoPostulacion visible, boolean decisionFinal) {
        boolean esArchivo = visible == EstadoPostulacion.archivada;
        boolean esRechazo = visible == EstadoPostulacion.no_adjudicada && !decisionFinal;
        if (!esArchivo && !esRechazo) {
            return null;
        }
        return ultimaFila(historial, visible).map(FilaHistorial::motivo).orElse(null);
    }

    private static Optional<FilaHistorial> ultimaFila(List<FilaHistorial> historial, EstadoPostulacion estado) {
        for (int i = historial.size() - 1; i >= 0; i--) {
            if (historial.get(i).estadoNuevo().equals(estado.name())) {
                return Optional.of(historial.get(i));
            }
        }
        return Optional.empty();
    }

    private FinalistaPostulanteResponse bloqueFinalista(DatosFinalista finalista, long postulacionId) {
        String documentos = consulta.mensajeDelAvisoDeFinalista(postulacionId)
                .map(MisPostulacionesService::documentosDelAviso).orElse(null);
        return new FinalistaPostulanteResponse(finalista.plazoEntrega(), finalista.documentacionRecibida(),
                documentos);
    }

    /** Los documentos a presentar del aviso: lo que sigue a la marca, sin el punto final de la plantilla. */
    static String documentosDelAviso(String mensaje) {
        int inicio = mensaje.lastIndexOf(MARCA_DOCUMENTOS);
        if (inicio < 0) {
            return null;
        }
        String documentos = mensaje.substring(inicio + MARCA_DOCUMENTOS.length()).trim();
        if (documentos.endsWith(".")) {
            documentos = documentos.substring(0, documentos.length() - 1).trim();
        }
        return documentos.isEmpty() ? null : documentos;
    }

    private static MiPostulacionResumenResponse resumen(FilaListado fila) {
        EstadoPostulacion visible = EstadoVisible.calcular(EstadoPostulacion.valueOf(fila.estado()),
                resultadosVisibles(fila));
        return new MiPostulacionResumenResponse(fila.codigo(),
                new ConvocatoriaResumenResponse(fila.convocatoriaCodigo(), fila.convocatoriaNombre()),
                new GestionAcademicaResumenResponse(fila.gestionCodigo(), fila.gestionNombre()), fila.fechaEnvio(),
                visible.name(), fila.numeroEnvios());
    }

    /**
     * Si la decision de la postulacion ya se puede mostrar: con los resultados publicados, o si no es una
     * decision final del CU05 (un rechazo del CU20, que no tiene fila de `adjudicacion`, se ve de inmediato).
     */
    static boolean resultadosVisibles(FilaListado fila) {
        return fila.resultadosPublicados() || !fila.decisionFinal();
    }

    /** Solo se ordena por `fechaEnvio`; sin `sort` es descendente. Cualquier otro campo, o mas de uno, es 400. */
    private static boolean esAscendente(Sort sort) {
        if (sort.isUnsorted()) {
            return false;
        }
        List<Sort.Order> ordenes = sort.toList();
        if (ordenes.size() > 1) {
            throw new ValidationException(ERR_SYS_05, "Solo se puede ordenar por un campo a la vez");
        }
        Sort.Order orden = ordenes.get(0);
        if (!CAMPO_ORDEN.equals(orden.getProperty())) {
            throw new ValidationException(ERR_SYS_05,
                    "No se puede ordenar por «" + orden.getProperty() + "». Campo válido: " + CAMPO_ORDEN);
        }
        return orden.isAscending();
    }

    private static UUID parsearGestion(String gestionAcademica) {
        if (gestionAcademica == null || gestionAcademica.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(gestionAcademica.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ERR_SYS_05,
                    "El filtro «gestionAcademica» debe ser el código de una gestión académica");
        }
    }
}
