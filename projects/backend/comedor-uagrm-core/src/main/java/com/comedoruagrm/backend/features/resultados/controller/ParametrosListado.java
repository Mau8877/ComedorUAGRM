package com.comedoruagrm.backend.features.resultados.controller;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.comedoruagrm.backend.common.exception.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Lectura de los query params de listado de los controllers de resultados (CU22): `page` (1-indexed),
 * `pageSize`, `sort` (un solo campo, `campo` o `-campo`) y los `filter[...]` permitidos.
 *
 * * Solo traduce el contrato de ENDPOINTS_BACKEND.md a un `Pageable`; que campo de `sort` es valido lo decide
 * * la consulta de cada lista. Un valor mal formado o un `filter` no soportado es `ERR_SYS_05` (400), no se
 * * ignora en silencio.
 */
final class ParametrosListado {

    private ParametrosListado() {
    }

    static Pageable pageable(int page, int pageSize, String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        return PageRequest.of(page - 1, pageSize, ordenar(sort));
    }

    private static Sort ordenar(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String valor = sort.trim();
        boolean descendente = valor.startsWith("-");
        String campo = descendente ? valor.substring(1) : valor;
        if (campo.isBlank() || campo.contains(",") || campo.contains(" ")) {
            throw new ValidationException("ERR_SYS_05", "El «sort» admite un solo campo: campo o -campo");
        }
        return Sort.by(descendente ? Sort.Direction.DESC : Sort.Direction.ASC, campo);
    }

    /** Rechaza cualquier `filter[...]` que no este en la lista. */
    static void validarFiltros(HttpServletRequest request, Set<String> soportados) {
        request.getParameterMap().keySet().stream()
                .filter(nombre -> nombre.startsWith("filter[") && !soportados.contains(nombre))
                .findFirst()
                .ifPresent(nombre -> {
                    throw new ValidationException("ERR_SYS_05", "El filtro «" + nombre
                            + "» no está soportado. Filtros válidos: " + String.join(", ", soportados));
                });
    }
}
