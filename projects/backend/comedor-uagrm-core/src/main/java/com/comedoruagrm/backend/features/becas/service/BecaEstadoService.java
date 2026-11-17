package com.comedoruagrm.backend.features.becas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import com.comedoruagrm.backend.features.becas.dto.BecaResponse;
import com.comedoruagrm.backend.features.becas.dto.CandidatoReemplazoResponse;
import com.comedoruagrm.backend.features.becas.dto.ReemplazoResponse;
import com.comedoruagrm.backend.features.becas.enums.EstadoBeca;
import com.comedoruagrm.backend.features.becas.enums.TipoMovimientoBeca;
import com.comedoruagrm.backend.features.becas.exception.BecaErrorCodes;
import com.comedoruagrm.backend.features.becas.model.Beca;
import com.comedoruagrm.backend.features.becas.model.MovimientoBeca;
import com.comedoruagrm.backend.features.becas.repository.BecaRepository;
import com.comedoruagrm.backend.features.becas.repository.MovimientoBecaRepository;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.CabeceraBeca;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.Candidato;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Los cambios de una beca del padron (CU23): dar de baja, suspender y reemplazar un cupo liberado. Solo el
 * Administrador DUBSS.
 *
 * * Una baja o una suspension exige una beca `VIGENTE` y un motivo; cambia el estado, inserta el movimiento y
 * * anota la bitacora en una sola transaccion. Una baja es definitiva.
 * * Un reemplazo exige una beca en `BAJA` sin reemplazo previo: crea una beca vigente para el siguiente postulante
 * * disponible y la vincula con la liberada por un movimiento `REEMPLAZO`. El reemplazante no recibe adjudicacion
 * * propia y su postulacion no cambia de estado (decision pendiente de confirmar con la DUBSS, ver el CU23).
 * * Se bloquea primero la convocatoria y despues la beca, asi dos operaciones simultaneas se atienden una tras otra.
 * ! No se loguean ni se anotan en la bitacora el motivo, nombres, CI ni correos.
 */
@Service
@RequiredArgsConstructor
public class BecaEstadoService {

    private static final Logger LOG = LoggerFactory.getLogger(BecaEstadoService.class);

    /** El CU no pide motivo en el reemplazo y la columna es obligatoria, por eso es un texto fijo. */
    static final String MOTIVO_REEMPLAZO = "Reemplazo del cupo liberado por la baja de otra beca";

    private final PadronConsultaRepository consulta;
    private final BecaRepository becaRepository;
    private final MovimientoBecaRepository movimientoRepository;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    // ---- Baja y suspension ----

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.BAJA_BECA, entidad = "beca")
    public ResultadoAuditado<BecaResponse> darDeBaja(UUID actorCodigo, UUID becaCodigo, String motivo) {
        return cambiar(TipoMovimientoBeca.BAJA, actorCodigo, becaCodigo, motivo);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.SUSPENSION_BECA, entidad = "beca")
    public ResultadoAuditado<BecaResponse> suspender(UUID actorCodigo, UUID becaCodigo, String motivo) {
        return cambiar(TipoMovimientoBeca.SUSPENSION, actorCodigo, becaCodigo, motivo);
    }

    private ResultadoAuditado<BecaResponse> cambiar(TipoMovimientoBeca tipo, UUID actorCodigo, UUID becaCodigo,
            String motivo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        CabeceraBeca cabecera = bloquear(becaCodigo);
        if (!EstadoBeca.VIGENTE.name().equals(cabecera.estado())) {
            throw new ConflictException(BecaErrorCodes.ERR_BEC_05,
                    "Solo una beca vigente se puede dar de baja o suspender (estado actual: " + cabecera.estado()
                            + ")");
        }
        Beca beca = becaRepository.findById(cabecera.id()).orElseThrow();
        EstadoBeca anterior = beca.getEstado();
        if (tipo == TipoMovimientoBeca.BAJA) {
            beca.darDeBaja();
        } else {
            beca.suspender();
        }
        becaRepository.saveAndFlush(beca);
        MovimientoBeca movimiento = movimientoRepository.saveAndFlush(
                new MovimientoBeca(beca.getId(), null, actor.getId(), tipo, hoy(), motivo.trim()));
        LOG.info(tipo == TipoMovimientoBeca.BAJA ? "Beca dada de baja" : "Beca suspendida", kv("module", "becas"),
                kv("userId", actorCodigo), kv("becaId", beca.getCodigo()));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("beca", beca.getCodigo().toString());
        detalle.put("movimiento", movimiento.getCodigo().toString());
        detalle.put("estadoAnterior", anterior.name());
        detalle.put("estadoNuevo", beca.getEstado().name());
        BecaResponse respuesta = BecaResponse.de(consulta.fila(beca.getId()).orElseThrow());
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), beca.getId(), detalle, true);
    }

    // ---- Reemplazo ----

    /** El siguiente postulante disponible para ocupar el cupo de una beca dada de baja (paso 13 del CU). */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public CandidatoReemplazoResponse candidatoDeReemplazo(UUID becaCodigo) {
        CabeceraBeca cabecera = consulta.cabeceraBeca(becaCodigo).orElseThrow(this::becaInexistente);
        exigirReemplazable(cabecera);
        return CandidatoReemplazoResponse.de(candidato(cabecera.convocatoriaId()));
    }

    /**
     * Confirma el reemplazo con el postulante que el Administrador vio. Vuelve a calcular el candidato ya con los
     * bloqueos tomados: si cambio, `ERR_BEC_08`.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.REEMPLAZO_BECA, entidad = "beca")
    public ResultadoAuditado<ReemplazoResponse> reemplazar(UUID actorCodigo, UUID becaCodigo,
            UUID postulanteCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        // Siempre la convocatoria primero y la beca despues: evita bloqueos cruzados.
        CabeceraBeca previa = consulta.cabeceraBeca(becaCodigo).orElseThrow(this::becaInexistente);
        consulta.bloquearConvocatoria(previa.convocatoriaId());
        CabeceraBeca cabecera = bloquear(becaCodigo);
        exigirReemplazable(cabecera);
        Candidato candidato = candidato(cabecera.convocatoriaId());
        if (!candidato.postulanteCodigo().equals(postulanteCodigo)) {
            throw new ConflictException(BecaErrorCodes.ERR_BEC_08,
                    "El postulante confirmado ya no es el siguiente disponible");
        }

        LocalDate hoy = hoy();
        Beca nueva = becaRepository.saveAndFlush(
                new Beca(cabecera.convocatoriaId(), candidato.postulanteId(), null, hoy));
        MovimientoBeca movimiento = movimientoRepository.saveAndFlush(new MovimientoBeca(cabecera.id(),
                nueva.getId(), actor.getId(), TipoMovimientoBeca.REEMPLAZO, hoy, MOTIVO_REEMPLAZO));
        LOG.info("Cupo de beca reemplazado", kv("module", "becas"), kv("userId", actorCodigo),
                kv("becaId", cabecera.codigo()));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("becaLiberada", cabecera.codigo().toString());
        detalle.put("becaReemplazante", nueva.getCodigo().toString());
        detalle.put("movimiento", movimiento.getCodigo().toString());
        ReemplazoResponse respuesta = new ReemplazoResponse(
                BecaResponse.de(consulta.fila(cabecera.id()).orElseThrow()),
                BecaResponse.de(consulta.fila(nueva.getId()).orElseThrow()));
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), cabecera.id(), detalle, true);
    }

    // ---- Ayudas ----

    private CabeceraBeca bloquear(UUID becaCodigo) {
        return consulta.bloquearBeca(becaCodigo).orElseThrow(this::becaInexistente);
    }

    private NotFoundException becaInexistente() {
        return new NotFoundException(BecaErrorCodes.ERR_BEC_01, "La beca no existe");
    }

    /** Una beca solo se reemplaza si esta en baja y todavia no fue reemplazada. */
    private void exigirReemplazable(CabeceraBeca cabecera) {
        if (!EstadoBeca.BAJA.name().equals(cabecera.estado())) {
            throw new ConflictException(BecaErrorCodes.ERR_BEC_06,
                    "Solo una beca dada de baja se puede reemplazar (estado actual: " + cabecera.estado() + ")");
        }
        if (consulta.yaReemplazada(cabecera.id())) {
            throw new ConflictException(BecaErrorCodes.ERR_BEC_09, "La beca ya fue reemplazada");
        }
    }

    private Candidato candidato(long convocatoriaId) {
        return consulta.siguienteDisponible(convocatoriaId).orElseThrow(() -> new ConflictException(
                BecaErrorCodes.ERR_BEC_07, "No hay un postulante disponible para ocupar el cupo"));
    }

    private LocalDate hoy() {
        return FechasConvocatoria.aFecha(reloj.instant());
    }
}
