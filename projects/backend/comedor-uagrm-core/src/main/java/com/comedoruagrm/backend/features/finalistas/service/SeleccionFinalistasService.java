package com.comedoruagrm.backend.features.finalistas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
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
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.finalistas.model.Finalista;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaRepository;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionFinalistasResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionFinalistasResponse.FinalistaSeleccionado;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionarFinalistasRequest;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CandidataFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.service.AdjudicacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Seleccion de finalistas de una convocatoria cerrada (CU05). Solo el Administrador DUBSS.
 *
 * * Crea las filas de `finalista` de las mejores postulaciones calificadas que todavia no lo son, con su
 * * posicion en la lista priorizada completa. Se puede repetir para sumar mas sin repetir a los anteriores.
 * * La cantidad puede superar los cupos (los de mas son suplentes). No toca plazo, aviso ni entrega (CU21) ni
 * * el estado de la postulacion, y no avisa a nadie.
 * * Los datos de la convocatoria y de la lista los entrega `AdjudicacionService` (modulo `postulaciones`);
 * * este modulo no lee sus repositories. La convocatoria queda bloqueada durante la transaccion.
 * ! La bitacora lleva solo el codigo de la convocatoria y la cantidad: nunca nombres, CI ni puntajes.
 */
@Service
@RequiredArgsConstructor
public class SeleccionFinalistasService {

    private static final Logger LOG = LoggerFactory.getLogger(SeleccionFinalistasService.class);

    private final AdjudicacionService adjudicacionService;
    private final FinalistaRepository finalistaRepository;
    private final UsuarioService usuarioService;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.SELECCION_FINALISTAS, entidad = "finalista")
    public ResultadoAuditado<SeleccionFinalistasResponse> seleccionar(UUID actorCodigo, UUID convocatoriaCodigo,
            SeleccionarFinalistasRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria convocatoria = adjudicacionService.convocatoriaBloqueada(convocatoriaCodigo);

        if (!adjudicacionService.decisionesPosibles(convocatoria)) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_05,
                    "La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos para elegir finalistas");
        }
        if (adjudicacionService.resultadosPublicados(convocatoria.id())) {
            throw new ConflictException(AdjudicacionErrorCodes.ERR_ADJ_01,
                    "Los resultados de la convocatoria ya fueron publicados");
        }

        long disponibles = adjudicacionService.contarCalificadasSinFinalista(convocatoria.id());
        Integer pedida = request != null && request.cantidad() != null ? request.cantidad()
                : convocatoria.cantidadFinalistas();
        if (pedida == null) {
            throw new ValidationException("ERR_SYS_01", "Indica cuántos finalistas seleccionar: la convocatoria "
                    + "no tiene una cantidad configurada");
        }
        if (pedida <= 0) {
            throw new ValidationException("ERR_SYS_01", "La cantidad debe ser un número entero positivo");
        }
        if (pedida > disponibles) {
            throw new ValidationException("ERR_SYS_01", "La cantidad (" + pedida + ") supera las postulaciones "
                    + "calificadas que aún no son finalistas (" + disponibles + " disponibles)");
        }

        List<FinalistaSeleccionado> creados = new ArrayList<>();
        for (CandidataFinalista candidata : adjudicacionService.siguientesSinFinalista(convocatoria.id(), pedida)) {
            Finalista finalista = finalistaRepository.saveAndFlush(
                    new Finalista(candidata.postulacionId(), (int) candidata.posicion()));
            creados.add(new FinalistaSeleccionado(finalista.getCodigo(), candidata.postulacionCodigo(),
                    candidata.posicion()));
        }
        LOG.info("Finalistas seleccionados", kv("module", "finalistas"), kv("userId", actorCodigo),
                kv("cantidad", creados.size()));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("convocatoria", convocatoria.codigo().toString());
        detalle.put("seleccionados", creados.size());
        SeleccionFinalistasResponse respuesta = new SeleccionFinalistasResponse(convocatoria.codigo(),
                creados.size(), creados);
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), null, detalle, true);
    }
}
