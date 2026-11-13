package com.comedoruagrm.backend.features.resultados.dto;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.comedoruagrm.backend.common.response.PageMeta;

/** Una pagina de resultados junto con su bloque `meta`. */
public record PaginaResultados<T>(List<T> datos, PageMeta meta) {

    /** Arma la pagina y su `meta` (pagina 1-indexed) a partir de las filas y el total que cumplen el filtro. */
    public static <T> PaginaResultados<T> de(List<T> filas, long total, Pageable pageable) {
        int tamano = pageable.getPageSize();
        int totalPaginas = (int) ((total + tamano - 1) / tamano);
        return new PaginaResultados<>(filas, new PageMeta(pageable.getPageNumber() + 1, tamano, total, totalPaginas));
    }
}
