package com.comedoruagrm.backend.features.preseleccion.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.preseleccion.dto.LoteReprocesamientoResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ResumenReprocesamientoResponse;
import com.comedoruagrm.backend.features.preseleccion.exception.ModeloPreseleccionErrorCodes;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ModeloFila;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository.LoteFila;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * El reprocesamiento de los puntajes de una convocatoria (CU26): el resumen previo a la confirmacion, el arranque
 * del lote y la consulta de su avance. Solo el Administrador DUBSS. El calculo lo hace en segundo plano
 * {@link ReprocesamientoEjecucionService}.
 *
 * * Reprocesar recalcula con la version VIGENTE las postulaciones `calificada` de la convocatoria que aun no tienen
 * * puntaje vigente de esa version: repetirlo reintenta solo las que fallaron (A5).
 * * Se bloquea si ya se publicaron los resultados (A3, `ERR_MOD_04`), si hay un lote en curso (A4, `ERR_MOD_03`;
 * * el indice unico parcial lo garantiza aunque dos pedidos lleguen juntos), si no hay version vigente
 * * (`ERR_MOD_02`) o si no hay nada por recalcular (`ERR_MOD_06`).
 * * El lote se crea `PENDIENTE` y su ejecucion arranca al confirmar la transaccion (evento
 * * {@link LoteReprocesamientoCreado}); la consulta del avance es la lectura del propio lote.
 * * Las lecturas no se loguean ni se anotan en la bitacora.
 */
@Service
@RequiredArgsConstructor
public class ReprocesamientoService {

    private static final Logger LOG = LoggerFactory.getLogger(ReprocesamientoService.class);

    private final ReprocesamientoConsultaRepository consulta;
    private final ModeloPreseleccionConsultaRepository modelos;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher eventos;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ResumenReprocesamientoResponse resumen(UUID convocatoriaCodigo) {
        ConvocatoriaDatos c = convocatoria(convocatoriaCodigo);
        ModeloFila vigente = modelos.vigente().orElse(null);
        int porReprocesar = vigente == null ? 0 : consulta.porReprocesar(c.id(), vigente.id()).size();
        LoteReprocesamientoResponse enCurso = consulta.loteEnCurso(c.id()).map(ReprocesamientoService::aResponse)
                .orElse(null);
        return new ResumenReprocesamientoResponse(c.codigo(), c.nombre(), c.gestion(),
                vigente == null ? null : vigente.version(), consulta.calificadas(c.id()), porReprocesar,
                consulta.resultadosPublicados(c.id()), enCurso);
    }

    /** Crea el lote `PENDIENTE` y lo deja listo para ejecutarse al confirmar la transaccion. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.REPROCESAMIENTO_PUNTAJES, entidad = "lote_reprocesamiento")
    public ResultadoAuditado<LoteReprocesamientoResponse> reprocesar(UUID actorCodigo, UUID convocatoriaCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        ConvocatoriaDatos c = convocatoria(convocatoriaCodigo);
        if (consulta.resultadosPublicados(c.id())) {
            throw new ConflictException(ModeloPreseleccionErrorCodes.ERR_MOD_04,
                    "Los resultados de la convocatoria ya fueron publicados: no se pueden reprocesar los puntajes");
        }
        ModeloFila vigente = modelos.vigente().orElseThrow(() -> new ConflictException(
                ModeloPreseleccionErrorCodes.ERR_MOD_02,
                "Todavía no hay una versión vigente del modelo: guarde los parámetros primero"));
        if (consulta.loteEnCurso(c.id()).isPresent()) {
            throw enCurso();
        }
        int total = consulta.porReprocesar(c.id(), vigente.id()).size();
        if (total == 0) {
            throw new ConflictException(ModeloPreseleccionErrorCodes.ERR_MOD_06,
                    "No hay postulaciones calificadas por reprocesar con la versión " + vigente.version());
        }
        LoteFila lote;
        try {
            lote = consulta.crearLote(c.id(), vigente.id(), actor.getId(), total);
        } catch (DataIntegrityViolationException e) {
            throw enCurso();
        }
        eventos.publishEvent(new LoteReprocesamientoCreado(lote.id()));
        LOG.info("Reprocesamiento solicitado", kv("module", "preseleccion"), kv("userId", actorCodigo),
                kv("convocatoriaId", c.codigo()), kv("loteId", lote.codigo()), kv("total", total));
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("convocatoria", c.codigo().toString());
        detalle.put("modelo", vigente.version());
        detalle.put("total", total);
        return new ResultadoAuditado<>(aResponse(lote), actor.getId(), actor.getCodigo(), lote.id(), detalle, true);
    }

    /** El avance de un lote: se consulta cada pocos segundos mientras esta en curso. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public LoteReprocesamientoResponse lote(UUID loteCodigo) {
        return consulta.lote(loteCodigo).map(ReprocesamientoService::aResponse).orElseThrow(
                () -> new NotFoundException(ModeloPreseleccionErrorCodes.ERR_MOD_05,
                        "El reprocesamiento no existe"));
    }

    // ---- Ayudas ----

    private ConvocatoriaDatos convocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(() -> new NotFoundException(
                ConvocatoriaErrorCodes.ERR_CV_01, "La convocatoria indicada no existe"));
    }

    private static ConflictException enCurso() {
        return new ConflictException(ModeloPreseleccionErrorCodes.ERR_MOD_03,
                "Ya hay un reprocesamiento en curso para esa convocatoria");
    }

    private static LoteReprocesamientoResponse aResponse(LoteFila l) {
        return new LoteReprocesamientoResponse(l.codigo(), l.convocatoriaCodigo(), l.modeloVersion(), l.estado(),
                l.total(), l.procesadas(), l.fallidas(), l.iniciadoEn(), l.terminadoEn());
    }
}
