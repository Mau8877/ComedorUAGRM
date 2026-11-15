package com.comedoruagrm.backend.features.convocatorias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;

/**
 * Lo que comparten los listados del modulo (convocatorias, gestiones y tipos de documento): validar `page` y
 * `pageSize`, traducir `sort` a un orden valido y armar el patron de busqueda.
 */
final class ListadoConvocatorias {

    private static final Logger LOG = LoggerFactory.getLogger(ListadoConvocatorias.class);

    private ListadoConvocatorias() {
    }

    /**
     * `page` es 1-indexed. `campos` mapea el nombre del `sort` (camelCase del DTO) a la propiedad del Model; un
     * campo que no esta es `ERR_CV_11`. El `id` desempata para que la paginacion sea estable.
     */
    static Pageable pagina(int page, int pageSize, String sort, Map<String, String> campos, String ordenPorDefecto) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        String valor = (sort == null || sort.isBlank()) ? ordenPorDefecto : sort.trim();
        Sort.Direction direccion = valor.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = valor.startsWith("-") ? valor.substring(1) : valor;
        String propiedad = campos.get(campo);
        if (propiedad == null) {
            LOG.warn("Listado rechazado", kv("module", "convocatorias"),
                    kv("errorCode", ConvocatoriaErrorCodes.ERR_CV_11));
            throw new ValidationException(ConvocatoriaErrorCodes.ERR_CV_11,
                    "No se puede ordenar por «" + campo + "». Campos válidos: "
                            + String.join(", ", campos.keySet().stream().sorted().toList()));
        }
        return PageRequest.of(page - 1, pageSize, Sort.by(direccion, propiedad).and(Sort.by(direccion, "id")));
    }

    /** `%texto%` en minuscula, o null si no hay texto (sin filtro). */
    static String patron(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
