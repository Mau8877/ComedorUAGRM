package com.comedoruagrm.backend.features.postulaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
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
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.dto.DecisionAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.NoAdjudicarRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.PanelAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.enums.ResultadoAdjudicacion;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Adjudicacion;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CabeceraFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CandidataFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.UltimaAuditoria;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Adjudicacion de becas (CU05): el panel de cupos y la decision final sobre cada finalista, adjudicar o no
 * adjudicar. Solo el Administrador DUBSS. Seleccionar finalistas vive en el modulo `finalistas`
 * (`SeleccionFinalistasService`), dueno de esa tabla, y usa los metodos de consulta publicados aqui.
 *
 * * Una decision cambia el estado «calificada» a «adjudicada» o «no adjudicada», crea la fila de
 * * `adjudicacion` (inmutable), deja la fila en el historial y anota la bitacora en una sola transaccion.
 * * No avisa al postulante (eso es del CU22) y no toca documentos, puntaje de la IA, auditorias ni `finalista`.
 * * No hay idempotencia por clave: se bloquea primero la convocatoria y despues la postulacion, asi dos
 * * decisiones simultaneas se atienden una tras otra y la segunda recibe `ERR_ADJ_01` (o `ERR_ADJ_04` si el
 * * ultimo cupo ya se uso con otro finalista).
 * ! No se loguean ni se anotan en la bitacora datos personales ni el motivo.
 */
@Service
@RequiredArgsConstructor
public class AdjudicacionService {

    private static final Logger LOG = LoggerFactory.getLogger(AdjudicacionService.class);

    private final AdjudicacionConsultaRepository consulta;
    private final AdjudicacionRepository adjudicacionRepository;
    private final PostulacionRepository postulacionRepository;
    private final PostulacionHistorialRepository historialRepository;
    private final UsuarioService usuarioService;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PanelAdjudicacionResponse panel(UUID convocatoriaCodigo) {
        DatosConvocatoria c = consulta.convocatoria(convocatoriaCodigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
        long adjudicados = consulta.contarAdjudicadas(c.id());
        long noAdjudicados = consulta.contarNoAdjudicadas(c.id());
        boolean publicados = consulta.resultadosPublicados(c.id());
        boolean habilitadas = decisionesPosibles(c) && !publicados;
        return new PanelAdjudicacionResponse(
                new PanelAdjudicacionResponse.ConvocatoriaPanel(c.codigo(), c.nombre(), c.estado()),
                c.cupos(), c.puntajeMinimo(), adjudicados, noAdjudicados,
                cuposDisponibles(c.cupos(), adjudicados), c.cantidadFinalistas(),
                consulta.contarFinalistas(c.id()), consulta.contarCalificadasSinFinalista(c.id()),
                publicados, habilitadas);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.ADJUDICACION_BECA, entidad = "adjudicacion")
    public ResultadoAuditado<DecisionAdjudicacionResponse> adjudicar(UUID actorCodigo, UUID finalistaCodigo) {
        return decidir(ResultadoAdjudicacion.ADJUDICADA, actorCodigo, finalistaCodigo, null);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.NO_ADJUDICACION_BECA, entidad = "adjudicacion")
    public ResultadoAuditado<DecisionAdjudicacionResponse> noAdjudicar(UUID actorCodigo, UUID finalistaCodigo,
            NoAdjudicarRequest request) {
        return decidir(ResultadoAdjudicacion.NO_ADJUDICADA, actorCodigo, finalistaCodigo,
                request.motivo().trim());
    }

    // ---- Consulta publicada para SeleccionFinalistasService (modulo `finalistas`) ----

    /**
     * La convocatoria con ese codigo, ya bloqueada hasta el fin de la transaccion en curso (dos selecciones
     * simultaneas se atienden una despues de la otra). 404 `ERR_PRI_01` si no existe.
     */
    public DatosConvocatoria convocatoriaBloqueada(UUID convocatoriaCodigo) {
        DatosConvocatoria c = consulta.convocatoria(convocatoriaCodigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
        consulta.bloquearConvocatoria(c.id());
        return c;
    }

    /** Si la convocatoria esta cerrada y tiene cupos y puntaje minimo (las decisiones y la seleccion se permiten). */
    public boolean decisionesPosibles(DatosConvocatoria c) {
        return EstadoConvocatoria.CERRADA.name().equals(c.estado()) && c.cupos() != null
                && c.puntajeMinimo() != null;
    }

    public boolean resultadosPublicados(long convocatoriaId) {
        return consulta.resultadosPublicados(convocatoriaId);
    }

    public long contarCalificadasSinFinalista(long convocatoriaId) {
        return consulta.contarCalificadasSinFinalista(convocatoriaId);
    }

    public List<CandidataFinalista> siguientesSinFinalista(long convocatoriaId, int limite) {
        return consulta.siguientesSinFinalista(convocatoriaId, limite);
    }

    // ---- Decision ----

    private ResultadoAuditado<DecisionAdjudicacionResponse> decidir(ResultadoAdjudicacion resultado,
            UUID actorCodigo, UUID finalistaCodigo, String motivo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        CabeceraFinalista c = consulta.cabeceraFinalista(finalistaCodigo).orElseThrow(() -> new NotFoundException(
                FinalistaErrorCodes.ERR_FIN_01, "El finalista no existe"));

        // Siempre la convocatoria primero y la postulacion despues: evita bloqueos cruzados.
        consulta.bloquearConvocatoria(c.convocatoriaId());
        consulta.bloquearPostulacion(c.postulacionId());
        DatosConvocatoria convocatoria = consulta.convocatoria(c.convocatoriaCodigo()).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));

        if (!decisionesPosibles(convocatoria)) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_05,
                    "La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos para decidir");
        }
        // Se vuelve a leer el estado despues del bloqueo: la peticion que llego segunda ya lo ve cambiado.
        String estadoActual = consulta.estadoActual(c.postulacionId());
        if (!EstadoPostulacion.calificada.name().equals(estadoActual)) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_01,
                    "La postulación ya no está calificada (estado actual: " + estadoActual + ")");
        }
        if (consulta.resultadosPublicados(c.convocatoriaId())) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_01,
                    "Los resultados de la convocatoria ya fueron publicados");
        }
        UltimaAuditoria auditoria = consulta.ultimaAuditoria(c.finalistaId()).orElseThrow(
                () -> new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_02,
                        "El expediente del finalista todavía no fue auditado"));

        BigDecimal total = total(c.puntajeIa(), auditoria.puntajeAcademico());
        if (resultado == ResultadoAdjudicacion.ADJUDICADA) {
            validarAdjudicacion(c, convocatoria, auditoria, total);
        }

        Postulacion postulacion = postulacionRepository.findById(c.postulacionId()).orElseThrow();
        EstadoPostulacion estadoNuevo = resultado == ResultadoAdjudicacion.ADJUDICADA
                ? EstadoPostulacion.adjudicada : EstadoPostulacion.no_adjudicada;
        if (resultado == ResultadoAdjudicacion.ADJUDICADA) {
            postulacion.adjudicar();
        } else {
            postulacion.noAdjudicar();
        }
        Adjudicacion adjudicacion = adjudicacionRepository.saveAndFlush(new Adjudicacion(c.postulacionId(),
                resultado, total, actor.getId(), Instant.now()));
        historialRepository.saveAndFlush(new PostulacionHistorial(c.postulacionId(), actor.getId(),
                EstadoPostulacion.calificada, estadoNuevo, motivo));
        LOG.info("Beca " + (resultado == ResultadoAdjudicacion.ADJUDICADA ? "adjudicada" : "no adjudicada"),
                kv("module", "postulaciones"), kv("userId", actorCodigo));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("adjudicacion", adjudicacion.getCodigo().toString());
        detalle.put("postulacion", c.postulacionCodigo().toString());
        detalle.put("finalista", c.finalistaCodigo().toString());
        detalle.put("convocatoria", c.convocatoriaCodigo().toString());
        detalle.put("resultado", resultado.name());
        detalle.put("estadoAnterior", EstadoPostulacion.calificada.name());
        detalle.put("estadoNuevo", estadoNuevo.name());
        Integer disponibles = cuposDisponibles(convocatoria.cupos(), consulta.contarAdjudicadas(c.convocatoriaId()));
        Instant fecha = adjudicacion.getFechaAdjudicacion();
        DecisionAdjudicacionResponse respuesta = new DecisionAdjudicacionResponse(c.finalistaCodigo(),
                c.postulacionCodigo(), resultado.name(), estadoNuevo.name(), adjudicacion.getPuntajeFinal(), fecha,
                actor.getNombres() + " " + actor.getApellidos(), disponibles);
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), adjudicacion.getId(), detalle,
                true);
    }

    /** Las tres reglas propias de adjudicar: ultima auditoria validada, puntaje suficiente y cupo libre. */
    private void validarAdjudicacion(CabeceraFinalista c, DatosConvocatoria convocatoria, UltimaAuditoria auditoria,
            BigDecimal total) {
        if (!ResultadoAuditoria.VALIDADO.name().equals(auditoria.resultado())) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_02,
                    "La última auditoría no es «Validado»: hay que auditar de nuevo o usar «No adjudicar»");
        }
        if (total == null || total.compareTo(convocatoria.puntajeMinimo()) < 0) {
            String actual = total == null ? "no disponible" : total.toPlainString();
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_03,
                    "El puntaje total (" + actual + ") no alcanza el puntaje mínimo ("
                            + convocatoria.puntajeMinimo().toPlainString() + ")");
        }
        Integer disponibles = cuposDisponibles(convocatoria.cupos(), consulta.contarAdjudicadas(c.convocatoriaId()));
        if (disponibles == null || disponibles <= 0) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_04,
                    "Los cupos de la convocatoria ya están agotados");
        }
    }

    /** IA vigente + puntaje academico de la ultima auditoria; nulo si falta cualquiera de los dos. */
    static BigDecimal total(BigDecimal ia, BigDecimal academico) {
        return ia == null || academico == null ? null : ia.add(academico);
    }

    /** Cupos menos adjudicadas, nunca negativo; nulo si no hay cupos definidos. */
    static Integer cuposDisponibles(Integer cupos, long adjudicadas) {
        return cupos == null ? null : (int) Math.max(0, cupos - adjudicadas);
    }
}
