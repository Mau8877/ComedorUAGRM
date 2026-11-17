package com.comedoruagrm.backend.features.convocatorias.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.convocatorias.dto.TipoDocumentoResponse;
import com.comedoruagrm.backend.features.convocatorias.service.TipoDocumentoService;
import com.comedoruagrm.backend.features.convocatorias.service.TipoDocumentoService.PaginaTiposDocumento;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Catalogo de tipos de documento (CU13), para elegir los que exige una convocatoria. Exige el rol
 * ADMINISTRADOR_DUBSS (lo comprueba el `service`).
 */
@RestController
@RequestMapping("/api/v1/tipos-documento")
@RequiredArgsConstructor
public class TipoDocumentoController {

    private final TipoDocumentoService tipoDocumentoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipoDocumentoResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, Set.of());
        PaginaTiposDocumento pagina = tipoDocumentoService.listar(page, pageSize, search, sort);
        return ResponseEntity.ok(ApiResponse.successList(pagina.datos(),
                "Tipos de documento obtenidos correctamente", pagina.meta()));
    }
}
