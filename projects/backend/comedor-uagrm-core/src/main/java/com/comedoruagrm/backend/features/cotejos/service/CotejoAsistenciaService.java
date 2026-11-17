package com.comedoruagrm.backend.features.cotejos.service;

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
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;
import com.comedoruagrm.backend.features.cotejos.dto.CotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.RegistrarCotejoRequest;
import com.comedoruagrm.backend.features.cotejos.enums.ResultadoCotejo;
import com.comedoruagrm.backend.features.cotejos.exception.CotejosErrorCodes;
import com.comedoruagrm.backend.features.cotejos.model.CotejoAsistencia;
import com.comedoruagrm.backend.features.cotejos.repository.CotejoAsistenciaRepository;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository.DatosPlanilla;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Registrar el cotejo de una planilla de asistencia contra los registros digitales (CU11). Solo el Administrador DUBSS.
 *
 * * El Administrador compara a mano la planilla firmada con los registros digitales y aqui se guarda lo que constato:
 * * total de registros, coincidencias, discrepancias, resultado y observaciones. No hay lectura automatica de la
 * * planilla (todavia): el sistema no decide el resultado, solo comprueba que lo declarado sea consistente.
 * * Reglas: coincidencias + discrepancias = total (`ERR_COT_02`); `CONFORME` exige 0 discrepancias y
 * * `CON_DISCREPANCIAS` al menos una (`ERR_COT_03`); con discrepancias las observaciones son obligatorias
 * * (`ERR_COT_04`, A2).
 * * Se pueden registrar varios cotejos de una misma planilla y se conservan todos (A3). No modifica ninguna asistencia
 * * ni la planilla. La fecha del cotejo es la de hoy (hora de Bolivia).
 * ! Ni el log ni la bitacora llevan las observaciones, nombres ni CI.
 */
@Service
@RequiredArgsConstructor
public class CotejoAsistenciaService {

    private static final Logger LOG = LoggerFactory.getLogger(CotejoAsistenciaService.class);

    private final CotejosConsultaRepository consulta;
    private final CotejoAsistenciaRepository cotejoRepository;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.COTEJO_ASISTENCIA, entidad = "cotejo_asistencia")
    public ResultadoAuditado<CotejoResponse> registrar(UUID actorCodigo, UUID planillaCodigo,
            RegistrarCotejoRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosPlanilla planilla = consulta.planilla(planillaCodigo)
                .orElseThrow(() -> new NotFoundException(CotejosErrorCodes.ERR_COT_01, "La planilla no existe"));
        String observaciones = limpiar(request.observaciones());
        validar(request, observaciones);

        LocalDate hoy = FechasConvocatoria.aFecha(reloj.instant());
        CotejoAsistencia cotejo = cotejoRepository.saveAndFlush(new CotejoAsistencia(planilla.id(), actor.getId(), hoy,
                request.totalRegistros(), request.coincidencias(), request.discrepancias(), request.resultado(),
                observaciones));
        LOG.info("Cotejo de asistencia registrado", kv("module", "cotejos"), kv("userId", actorCodigo),
                kv("planilla", planilla.codigo()), kv("resultado", request.resultado()));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("cotejo", cotejo.getCodigo().toString());
        detalle.put("planilla", planilla.codigo().toString());
        detalle.put("totalRegistros", request.totalRegistros());
        detalle.put("coincidencias", request.coincidencias());
        detalle.put("discrepancias", request.discrepancias());
        detalle.put("resultado", request.resultado().name());
        CotejoResponse respuesta = new CotejoResponse(cotejo.getCodigo(), planilla.codigo(), hoy,
                request.totalRegistros(), request.coincidencias(), request.discrepancias(), request.resultado(),
                observaciones, actor.getNombres() + " " + actor.getApellidos(), cotejo.getCreadoEn());
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), cotejo.getId(), detalle, true);
    }

    private static void validar(RegistrarCotejoRequest r, String observaciones) {
        if ((long) r.coincidencias() + r.discrepancias() != r.totalRegistros()) {
            throw new ValidationException(CotejosErrorCodes.ERR_COT_02,
                    "Las coincidencias y las discrepancias deben sumar el total de registros");
        }
        boolean hayDiscrepancias = r.discrepancias() > 0;
        if (hayDiscrepancias != (r.resultado() == ResultadoCotejo.CON_DISCREPANCIAS)) {
            throw new ValidationException(CotejosErrorCodes.ERR_COT_03, hayDiscrepancias
                    ? "Hay discrepancias: el resultado no puede ser «conforme»"
                    : "No hay discrepancias: el resultado no puede ser «con discrepancias»");
        }
        if (hayDiscrepancias && observaciones == null) {
            throw new ValidationException(CotejosErrorCodes.ERR_COT_04,
                    "Un cotejo con discrepancias exige observaciones");
        }
    }

    /** Las observaciones sin espacios sobrantes; vacias valen `null`. */
    private static String limpiar(String observaciones) {
        if (observaciones == null) {
            return null;
        }
        String limpio = observaciones.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
