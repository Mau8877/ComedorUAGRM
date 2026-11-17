package com.comedoruagrm.backend.features.becas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.becas.dto.AltaPadronResponse;
import com.comedoruagrm.backend.features.becas.dto.BecaResponse;
import com.comedoruagrm.backend.features.becas.dto.ResumenPadronResponse;
import com.comedoruagrm.backend.features.becas.dto.ResumenPadronResponse.ConvocatoriaPadron;
import com.comedoruagrm.backend.features.becas.enums.EstadoBeca;
import com.comedoruagrm.backend.features.becas.exception.BecaErrorCodes;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * El padron de becados de una convocatoria (CU23): el resumen, el listado paginado y el alta de los adjudicados.
 * Solo el Administrador DUBSS. Los cambios de estado de una beca estan en {@link BecaEstadoService}.
 *
 * * El padron solo existe cuando los resultados estan publicados (CU22): antes, el listado y el alta responden
 * * `ERR_BEC_03`. El alta crea una beca vigente por cada adjudicada sin beca, con la fecha de publicacion como
 * * inicio (hora de La Paz), y no se repite: sin pendientes responde `ERR_BEC_04`.
 * * Las lecturas no se loguean ni se anotan en la bitacora.
 * ! No se loguean ni se anotan en la bitacora nombres, CI ni correos de los becados.
 */
@Service
@RequiredArgsConstructor
public class PadronService {

    private static final Logger LOG = LoggerFactory.getLogger(PadronService.class);

    // Campo del `sort` (camelCase del DTO) -> fragmento SQL (nunca texto del cliente).
    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "becado", "u.apellidos %s, u.nombres %s",
            "ci", "u.ci %s",
            "carrera", "c.nombre %s",
            "estado", "b.estado %s",
            "fechaInicio", "b.fecha_inicio %s");

    private static final String ORDEN_POR_DEFECTO = "becado";

    private final PadronConsultaRepository consulta;
    private final UsuarioService usuarioService;

    /** Una pagina del padron junto con su bloque `meta`. */
    public record PaginaPadron(List<BecaResponse> datos, PageMeta meta) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ResumenPadronResponse resumen(UUID convocatoriaCodigo) {
        DatosConvocatoria c = convocatoria(convocatoriaCodigo);
        boolean publicados = consulta.fechaPublicacion(c.id()).isPresent();
        Map<String, Long> estados = consulta.contarPorEstado(c.id());
        return new ResumenPadronResponse(new ConvocatoriaPadron(c.codigo(), c.nombre()), publicados,
                consulta.contarAdjudicadas(c.id()), publicados ? consulta.contarAdjudicadasSinBeca(c.id()) : 0,
                estados.getOrDefault(EstadoBeca.VIGENTE.name(), 0L),
                estados.getOrDefault(EstadoBeca.SUSPENDIDA.name(), 0L),
                estados.getOrDefault(EstadoBeca.BAJA.name(), 0L));
    }

    /** `page` es 1-indexed; `search` busca en nombres, apellidos y CI; `estado` es opcional. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaPadron listar(UUID convocatoriaCodigo, int page, int pageSize, String search, EstadoBeca estado,
            String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        DatosConvocatoria c = convocatoria(convocatoriaCodigo);
        exigirPublicados(c);
        String orden = orden(sort);
        String patron = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        String filtroEstado = estado == null ? null : estado.name();
        long total = consulta.contar(c.id(), patron, filtroEstado);
        List<BecaResponse> datos = consulta.listar(c.id(), patron, filtroEstado, orden, pageSize,
                (long) (page - 1) * pageSize).stream().map(BecaResponse::de).toList();
        int paginas = (int) Math.ceil(total / (double) pageSize);
        return new PaginaPadron(datos, new PageMeta(page, pageSize, total, paginas));
    }

    /**
     * Da de alta a los adjudicados: crea una beca vigente por cada adjudicada sin beca. Bloquea la convocatoria, asi
     * dos altas simultaneas se atienden una tras otra y la segunda recibe `ERR_BEC_04`.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.ALTA_BECAS, entidad = "convocatoria")
    public ResultadoAuditado<AltaPadronResponse> darDeAlta(UUID actorCodigo, UUID convocatoriaCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria c = convocatoria(convocatoriaCodigo);
        consulta.bloquearConvocatoria(c.id());
        Instant publicado = exigirPublicados(c);
        if (consulta.contarAdjudicadasSinBeca(c.id()) == 0) {
            throw new ConflictException(BecaErrorCodes.ERR_BEC_04, "No hay adjudicados pendientes de alta");
        }
        int creadas = consulta.crearBecasFaltantes(c.id(), FechasConvocatoria.aFecha(publicado));
        LOG.info("Becados dados de alta", kv("module", "becas"), kv("userId", actorCodigo),
                kv("convocatoriaId", c.codigo()), kv("creadas", creadas));
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("convocatoria", c.codigo().toString());
        detalle.put("creadas", creadas);
        return new ResultadoAuditado<>(new AltaPadronResponse(creadas), actor.getId(), actor.getCodigo(), c.id(),
                detalle, creadas > 0);
    }

    // ---- Ayudas ----

    private DatosConvocatoria convocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(BecaErrorCodes.ERR_BEC_02, "La convocatoria no existe"));
    }

    /** La fecha de publicacion; `ERR_BEC_03` si los resultados todavia no se publicaron. */
    private Instant exigirPublicados(DatosConvocatoria c) {
        return consulta.fechaPublicacion(c.id()).orElseThrow(() -> new ConflictException(BecaErrorCodes.ERR_BEC_03,
                "Los resultados de la convocatoria todavía no fueron publicados"));
    }

    private String orden(String sort) {
        String valor = sort == null || sort.isBlank() ? ORDEN_POR_DEFECTO : sort.trim();
        boolean descendente = valor.startsWith("-");
        String campo = descendente ? valor.substring(1) : valor;
        String fragmento = CAMPOS_ORDENABLES.get(campo);
        if (fragmento == null) {
            LOG.warn("Listado del padron rechazado", kv("module", "becas"),
                    kv("errorCode", BecaErrorCodes.ERR_BEC_10));
            throw new ValidationException(BecaErrorCodes.ERR_BEC_10,
                    "No se puede ordenar por «" + campo + "». Campos válidos: "
                            + String.join(", ", CAMPOS_ORDENABLES.keySet().stream().sorted().toList()));
        }
        String direccion = descendente ? "DESC" : "ASC";
        // `id` desempata para que la paginacion sea estable.
        return fragmento.replace("%s", direccion) + ", b.id " + direccion;
    }
}
