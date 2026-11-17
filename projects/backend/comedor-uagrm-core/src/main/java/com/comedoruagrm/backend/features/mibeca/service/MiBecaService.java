package com.comedoruagrm.backend.features.mibeca.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.mibeca.dto.AsistenciaBecadoResponse;
import com.comedoruagrm.backend.features.mibeca.dto.MiBecaResponse;
import com.comedoruagrm.backend.features.mibeca.exception.MiBecaErrorCodes;
import com.comedoruagrm.backend.features.mibeca.repository.MiBecaConsultaRepository;
import com.comedoruagrm.backend.features.mibeca.repository.MiBecaConsultaRepository.Beca;

import lombok.RequiredArgsConstructor;

/**
 * Consulta del becado sobre su propia beca (CU24): el estado de la beca y el historial de asistencia. Solo el
 * Postulante (becado) y siempre sobre SU beca: el usuario sale del token, nunca de la URL ni del cuerpo (A4).
 *
 * * Es de solo lectura: no escribe, no anota en la bitacora y no loguea las consultas.
 * * Sin beca (A1) responde `ERR_MBE_01` (404); con la beca suspendida o dada de baja (A3) devuelve el estado y el
 * * motivo, y el historial sigue siendo consultable. Sin asistencias (A2) el listado es una pagina vacia.
 * * Las fechas del rango son dias de calendario en hora de Bolivia y se incluyen los dos extremos.
 */
@Service
@RequiredArgsConstructor
public class MiBecaService {

    private static final Logger LOG = LoggerFactory.getLogger(MiBecaService.class);

    // Campo del `sort` (camelCase del DTO) que se puede ordenar.
    private static final String CAMPO_ORDENABLE = "fecha";

    private final MiBecaConsultaRepository consulta;

    /** Una pagina del historial de asistencia junto con su bloque `meta`. */
    public record PaginaAsistencias(List<AsistenciaBecadoResponse> datos, PageMeta meta) {
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public MiBecaResponse miBeca(UUID usuarioCodigo) {
        Beca beca = beca(usuarioCodigo);
        return new MiBecaResponse(beca.codigo(), beca.estado(), beca.convocatoria(), beca.gestion(),
                beca.fechaAdjudicacion(), beca.fechaInicio(), beca.fechaFin(), beca.motivo(),
                beca.totalAsistencias());
    }

    /** `page` es 1-indexed; `sort` solo admite `fecha` o `-fecha` (por defecto, la mas reciente primero). */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public PaginaAsistencias asistencias(UUID usuarioCodigo, int page, int pageSize, LocalDate desde,
            LocalDate hasta, String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            LOG.warn("Consulta de asistencia rechazada", kv("module", "mibeca"),
                    kv("errorCode", MiBecaErrorCodes.ERR_MBE_02));
            throw new ValidationException(MiBecaErrorCodes.ERR_MBE_02,
                    "La fecha «desde» no puede ser posterior a la fecha «hasta»");
        }
        boolean descendente = descendente(sort);
        Beca beca = beca(usuarioCodigo);
        long total = consulta.contarAsistencias(beca.id(), desde, hasta);
        List<AsistenciaBecadoResponse> datos = consulta.listarAsistencias(beca.id(), desde, hasta, descendente,
                pageSize, (long) (page - 1) * pageSize);
        int paginas = (int) Math.ceil(total / (double) pageSize);
        return new PaginaAsistencias(datos, new PageMeta(page, pageSize, total, paginas));
    }

    private Beca beca(UUID usuarioCodigo) {
        return consulta.buscarBeca(usuarioCodigo).orElseThrow(() -> new NotFoundException(
                MiBecaErrorCodes.ERR_MBE_01, "No cuentas con una beca: no figuras en el padrón de becados"));
    }

    private static boolean descendente(String sort) {
        if (sort == null || sort.isBlank()) {
            return true;
        }
        String valor = sort.trim();
        boolean descendente = valor.startsWith("-");
        String campo = descendente ? valor.substring(1) : valor;
        if (!CAMPO_ORDENABLE.equals(campo)) {
            LOG.warn("Consulta de asistencia rechazada", kv("module", "mibeca"),
                    kv("errorCode", MiBecaErrorCodes.ERR_MBE_03));
            throw new ValidationException(MiBecaErrorCodes.ERR_MBE_03,
                    "No se puede ordenar por «" + campo + "». Campo válido: " + CAMPO_ORDENABLE);
        }
        return descendente;
    }
}
