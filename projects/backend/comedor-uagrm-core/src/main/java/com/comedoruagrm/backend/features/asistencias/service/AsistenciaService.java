package com.comedoruagrm.backend.features.asistencias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.asistencias.dto.AsistenciaResponse;
import com.comedoruagrm.backend.features.asistencias.dto.BecadoAsistenciaResponse;
import com.comedoruagrm.backend.features.asistencias.exception.AsistenciasErrorCodes;
import com.comedoruagrm.backend.features.asistencias.model.Asistencia;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciaRepository;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository.Becado;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository.CabeceraBeca;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Registro de la asistencia digital de un becado al comedor (CU10). Solo el Personal del Comedor.
 *
 * * `buscarBecado` busca la beca por CI y devuelve los datos del becado y el estado de su beca (A1: si el CI no
 * * figura, `ERR_ASI_01`). No exige que la beca este vigente: el personal debe poder ver por que no se registra (A2).
 * * `registrar` exige una beca `VIGENTE` (`ERR_ASI_02`) y crea la asistencia de HOY (hora de Bolivia) con la hora
 * * y el usuario que la registra. Es idempotente (RNF-03, A3): si ya habia una de hoy no crea otra y devuelve la
 * * existente con `yaRegistrada = true`; dos peticiones simultaneas tampoco duplican (indice unico parcial).
 * * La fecha la fija el servidor, nunca el cliente: no se puede registrar asistencia de otro dia.
 * * No se anota en la bitacora: la propia fila ya dice quien y cuando (`registrado_por`, `hora_registro`) y se
 * * escribiria una fila por comida de cada becado. Tampoco cambia el estado de la beca ni avisa a nadie.
 * ! Ni el log ni las respuestas de error llevan el CI ni el nombre del becado.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private static final Logger LOG = LoggerFactory.getLogger(AsistenciaService.class);

    private static final String ESTADO_VIGENTE = "VIGENTE";

    private final AsistenciasConsultaRepository consulta;
    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    @PreAuthorize("hasRole('PERSONAL_COMEDOR')")
    @Transactional(readOnly = true)
    public BecadoAsistenciaResponse buscarBecado(String ci) {
        LocalDate hoy = hoy();
        Becado becado = consulta.buscarPorCi(ci.trim(), hoy).orElseThrow(() -> new NotFoundException(
                AsistenciasErrorCodes.ERR_ASI_01, "El estudiante no figura en el padrón de becados"));
        return new BecadoAsistenciaResponse(becado.becaCodigo(), becado.nombres(), becado.apellidos(), becado.ci(),
                becado.carrera(), becado.convocatoria(), becado.estado(), hoy, becado.horaRegistro());
    }

    @PreAuthorize("hasRole('PERSONAL_COMEDOR')")
    @Transactional
    public AsistenciaResponse registrar(UUID actorCodigo, UUID becaCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        CabeceraBeca beca = consulta.cabeceraBeca(becaCodigo).orElseThrow(() -> new NotFoundException(
                AsistenciasErrorCodes.ERR_ASI_03, "La beca no existe"));
        if (!ESTADO_VIGENTE.equals(beca.estado())) {
            throw new ConflictException(AsistenciasErrorCodes.ERR_ASI_02,
                    "La beca no está vigente (estado: " + beca.estado() + "): no se puede registrar la asistencia");
        }

        LocalDate hoy = hoy();
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        boolean creada = asistenciaRepository.insertarSiNoExiste(beca.id(), actor.getId(), hoy, ahora) == 1;
        Asistencia asistencia = asistenciaRepository.findByBecaIdAndFecha(beca.id(), hoy).orElseThrow();
        if (creada) {
            LOG.info("Asistencia registrada", kv("module", "asistencias"), kv("userId", actorCodigo),
                    kv("becaId", beca.codigo()));
        }
        return new AsistenciaResponse(asistencia.getCodigo(), beca.codigo(), hoy, asistencia.getHoraRegistro(),
                !creada);
    }

    private LocalDate hoy() {
        return FechasConvocatoria.aFecha(reloj.instant());
    }
}
