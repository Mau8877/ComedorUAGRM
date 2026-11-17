package com.comedoruagrm.backend.features.convocatorias.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.convocatorias.dto.TipoDocumentoResponse;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.repository.TipoDocumentoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Catalogo de tipos de documento (CU13): solo se lista, para que el Administrador elija los que exige una
 * convocatoria. El catalogo en si se administra en otro CU.
 */
@Service
@RequiredArgsConstructor
public class TipoDocumentoService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("nombre", "nombre");

    private static final String ORDEN_POR_DEFECTO = "nombre";

    private final TipoDocumentoRepository tipoDocumentoRepository;

    /** Una pagina del catalogo junto con su bloque `meta`. */
    public record PaginaTiposDocumento(List<TipoDocumentoResponse> datos, PageMeta meta) {
    }

    /** `page` es 1-indexed. Las lecturas no se loguean. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaTiposDocumento listar(int page, int pageSize, String search, String sort) {
        Page<TipoDocumento> pagina = tipoDocumentoRepository.buscar(ListadoConvocatorias.patron(search),
                ListadoConvocatorias.pagina(page, pageSize, sort, CAMPOS_ORDENABLES, ORDEN_POR_DEFECTO));
        return new PaginaTiposDocumento(pagina.getContent().stream().map(TipoDocumentoResponse::de).toList(),
                new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages()));
    }
}
