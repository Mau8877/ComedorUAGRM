package com.comedoruagrm.backend.features.preseleccion.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.preseleccion.constant.VariablesModelo;
import com.comedoruagrm.backend.features.preseleccion.dto.GuardarModeloRequest;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloPreseleccionResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloPreseleccionResumenResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ParametroModeloRequest;
import com.comedoruagrm.backend.features.preseleccion.dto.ParametroModeloResponse;
import com.comedoruagrm.backend.features.preseleccion.exception.ModeloPreseleccionErrorCodes;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ModeloFila;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Las versiones del modelo de preseleccion y sus parametros (CU26): consultar la vigente, el historial y guardar una
 * version nueva. Solo el Administrador DUBSS.
 *
 * * Guardar crea una version nueva (`v{M}.{n+1}`) que pasa a ser la vigente de inmediato y archiva la anterior
 * * (A2: sin reprocesar, solo se aplica a lo que se precalifique desde ese momento). Las versiones anteriores y sus
 * * parametros se conservan. El «Borrador» del mockup es solo la edicion en pantalla, sin guardar.
 * * Los parametros son una ponderacion por variable (puntos maximos) y suman 100; valores y rangos finales por
 * * definir con la DUBSS.
 * * Las lecturas no se loguean ni se anotan en la bitacora.
 */
@Service
@RequiredArgsConstructor
public class ModeloPreseleccionService {

    private static final Logger LOG = LoggerFactory.getLogger(ModeloPreseleccionService.class);
    private static final BigDecimal MAXIMO = BigDecimal.valueOf(100);

    private final ModeloPreseleccionConsultaRepository consulta;
    private final UsuarioService usuarioService;

    /** Una pagina del historial de versiones junto con su bloque `meta`. */
    public record PaginaModelos(List<ModeloPreseleccionResumenResponse> datos, PageMeta meta) {
    }

    /** La version vigente con sus parametros, o `null` si todavia no se guardo ninguna. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ModeloPreseleccionResponse vigente() {
        return consulta.vigente().map(this::detalle).orElse(null);
    }

    /** `page` es 1-indexed; las versiones van de la mas reciente a la mas antigua. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaModelos listar(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        long total = consulta.contar();
        List<ModeloPreseleccionResumenResponse> datos = consulta.listar(pageSize, (long) (page - 1) * pageSize)
                .stream().map(m -> new ModeloPreseleccionResumenResponse(m.codigo(), m.version(), m.descripcion(),
                        m.activo(), m.creadoEn()))
                .toList();
        int paginas = (int) Math.ceil(total / (double) pageSize);
        return new PaginaModelos(datos, new PageMeta(page, pageSize, total, paginas));
    }

    /**
     * Valida los parametros (A1) y crea la version nueva como vigente. Dos guardados a la vez: el segundo viola el
     * indice unico de la version y recibe `ERR_MOD_07`.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.NUEVA_VERSION_MODELO, entidad = "modelo_preseleccion")
    public ResultadoAuditado<ModeloPreseleccionResponse> guardar(UUID actorCodigo, GuardarModeloRequest request) {
        Map<String, BigDecimal> parametros = validar(request);
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        String descripcion = request.descripcion() == null || request.descripcion().isBlank() ? null
                : request.descripcion().trim();
        ModeloFila modelo;
        try {
            String version = VersionModelo.siguiente(consulta.versiones());
            consulta.archivarVigente();
            modelo = consulta.crear(actor.getId(), version, descripcion);
            for (Map.Entry<String, BigDecimal> p : parametros.entrySet()) {
                consulta.insertarParametro(modelo.id(), p.getKey(), p.getValue());
            }
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ModeloPreseleccionErrorCodes.ERR_MOD_07,
                    "Otra versión se guardó al mismo tiempo: vuelva a abrir los parámetros");
        }
        LOG.info("Version del modelo guardada", kv("module", "preseleccion"), kv("userId", actorCodigo),
                kv("version", modelo.version()));
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("version", modelo.version());
        detalle.put("parametros", parametros);
        return new ResultadoAuditado<>(detalle(modelo), actor.getId(), actor.getCodigo(), modelo.id(), detalle, true);
    }

    // ---- Validacion ----

    /** Devuelve clave → ponderacion (2 decimales, en el orden del mockup) o lanza `ERR_MOD_01` con todo lo que falla. */
    static Map<String, BigDecimal> validar(GuardarModeloRequest request) {
        List<String> errores = new ArrayList<>();
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        Set<String> vistas = new LinkedHashSet<>();
        List<ParametroModeloRequest> recibidos = request == null || request.parametros() == null ? List.of()
                : request.parametros();
        for (ParametroModeloRequest p : recibidos) {
            if (p == null || p.clave() == null || !VariablesModelo.NOMBRES.containsKey(p.clave())) {
                errores.add("variable desconocida" + (p == null || p.clave() == null ? "" : " «" + p.clave() + "»"));
                continue;
            }
            String nombre = VariablesModelo.NOMBRES.get(p.clave());
            if (!vistas.add(p.clave())) {
                errores.add(nombre + " está repetida");
                continue;
            }
            BigDecimal pond = p.ponderacion();
            if (pond == null) {
                errores.add(nombre + " no tiene ponderación");
            } else if (pond.signum() < 0 || pond.compareTo(MAXIMO) > 0) {
                errores.add(nombre + " debe estar entre 0 y 100");
            } else if (pond.stripTrailingZeros().scale() > 2) {
                errores.add(nombre + " admite como máximo 2 decimales");
            } else {
                resultado.put(p.clave(), pond.setScale(2, RoundingMode.UNNECESSARY));
            }
        }
        VariablesModelo.NOMBRES.forEach((clave, nombre) -> {
            if (!vistas.contains(clave)) {
                errores.add("falta " + nombre);
            }
        });
        if (errores.isEmpty()) {
            BigDecimal suma = resultado.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (suma.compareTo(BigDecimal.valueOf(VariablesModelo.SUMA_PONDERACIONES)) != 0) {
                errores.add("las ponderaciones suman " + suma.stripTrailingZeros().toPlainString() + " y deben sumar "
                        + VariablesModelo.SUMA_PONDERACIONES);
            }
        }
        if (!errores.isEmpty()) {
            throw new ValidationException(ModeloPreseleccionErrorCodes.ERR_MOD_01,
                    "Los parámetros no son válidos: " + String.join("; ", errores));
        }
        Map<String, BigDecimal> ordenado = new LinkedHashMap<>();
        VariablesModelo.NOMBRES.keySet().forEach(clave -> ordenado.put(clave, resultado.get(clave)));
        return ordenado;
    }

    // ---- Ayudas ----

    private ModeloPreseleccionResponse detalle(ModeloFila modelo) {
        List<ParametroModeloResponse> parametros = consulta.parametros(modelo.id()).stream()
                .map(p -> new ParametroModeloResponse(p.clave(),
                        VariablesModelo.NOMBRES.getOrDefault(p.clave(), p.clave()), p.ponderacion()))
                .toList();
        return new ModeloPreseleccionResponse(modelo.codigo(), modelo.version(), modelo.descripcion(),
                modelo.activo(), modelo.creadoEn(), parametros);
    }
}
