package com.comedoruagrm.backend.features.bitacora.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.bitacora.dto.BitacoraResponse;
import com.comedoruagrm.backend.features.bitacora.exception.BitacoraErrorCodes;
import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.features.bitacora.repository.BitacoraRepository;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;

import lombok.RequiredArgsConstructor;

/**
 * Consulta de la bitacora (CU25): listado paginado y de solo lectura, con filtros por usuario, accion y rango de
 * fechas. Solo el Administrador DUBSS.
 *
 * * Las fechas son dias de calendario en hora de Bolivia (`America/La_Paz`): `desde` y `hasta` incluyen el dia
 * * completo. Las lecturas no se loguean ni se anotan en la bitacora.
 * ! Este servicio no escribe: la unica via de escritura sigue siendo `BitacoraService`.
 */
@Service
@RequiredArgsConstructor
public class BitacoraConsultaService {

    private static final Logger LOG = LoggerFactory.getLogger(BitacoraConsultaService.class);

    // Campo del `sort` (camelCase del DTO) -> propiedad del Model.
    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "fecha", "creadoEn",
            "accion", "accion",
            "entidad", "entidad");

    private static final String ORDEN_POR_DEFECTO = "-fecha";

    private final BitacoraRepository bitacoraRepository;

    /** Una pagina de la bitacora junto con su bloque `meta`. */
    public record PaginaBitacora(List<BitacoraResponse> datos, PageMeta meta) {
    }

    /** `page` es 1-indexed. `search` busca en nombres, apellidos y correo del usuario. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaBitacora listar(int page, int pageSize, String search, String accion, LocalDate desde,
            LocalDate hasta, String sort) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            LOG.warn("Consulta de bitacora rechazada", kv("module", "bitacora"),
                    kv("errorCode", BitacoraErrorCodes.ERR_BIT_02));
            throw new ValidationException(BitacoraErrorCodes.ERR_BIT_02,
                    "La fecha «desde» no puede ser posterior a la fecha «hasta»");
        }
        Instant desdeInstante = desde == null ? null : FechasConvocatoria.aperturaDesde(desde);
        Instant hastaExclusivo = hasta == null ? null
                : hasta.plusDays(1).atStartOfDay(FechasConvocatoria.ZONA).toInstant();
        Page<Bitacora> pagina = bitacoraRepository.buscar(patron(search), vacioANulo(accion), desdeInstante,
                hastaExclusivo, pagina(page, pageSize, sort));
        return new PaginaBitacora(pagina.getContent().stream().map(BitacoraResponse::de).toList(),
                new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages()));
    }

    /** Las acciones distintas que ya figuran en la bitacora (para el filtro de acciones de la pantalla). */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public List<String> acciones() {
        return bitacoraRepository.accionesRegistradas();
    }

    private static Pageable pagina(int page, int pageSize, String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        String valor = (sort == null || sort.isBlank()) ? ORDEN_POR_DEFECTO : sort.trim();
        Sort.Direction direccion = valor.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = valor.startsWith("-") ? valor.substring(1) : valor;
        String propiedad = CAMPOS_ORDENABLES.get(campo);
        if (propiedad == null) {
            LOG.warn("Consulta de bitacora rechazada", kv("module", "bitacora"),
                    kv("errorCode", BitacoraErrorCodes.ERR_BIT_01));
            throw new ValidationException(BitacoraErrorCodes.ERR_BIT_01,
                    "No se puede ordenar por «" + campo + "». Campos válidos: "
                            + String.join(", ", CAMPOS_ORDENABLES.keySet().stream().sorted().toList()));
        }
        // El id desempata para que la paginacion sea estable.
        return PageRequest.of(page - 1, pageSize, Sort.by(direccion, propiedad).and(Sort.by(direccion, "id")));
    }

    private static String patron(String search) {
        return search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
