package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionDetalleResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.service.MisPostulacionesService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * «Mis postulaciones» (CU04): el postulante consulta sus postulaciones enviadas y el detalle de cada una.
 * Exige el rol POSTULANTE (lo comprueba el `service`) y es solo lectura: el postulante sale del token, nunca de
 * la URL. `mias` es una ruta literal y se declara antes que `{codigo}`.
 */
@RestController
@RequestMapping("/api/v1/postulaciones")
@RequiredArgsConstructor
public class MisPostulacionesController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[gestionAcademica]");

    private final MisPostulacionesService misPostulacionesService;

    @GetMapping("/mias")
    public ResponseEntity<ApiResponse<List<MiPostulacionResumenResponse>>> mias(@CurrentUserId UUID usuarioCodigo,
            @RequestParam(name = "filter[gestionAcademica]", required = false) String gestionAcademica,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS_SOPORTADOS);
        Page<MiPostulacionResumenResponse> pagina = misPostulacionesService.listar(usuarioCodigo, gestionAcademica,
                ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(),
                "Postulaciones obtenidas correctamente", new PageMeta(pagina.getNumber() + 1, pagina.getSize(),
                        pagina.getTotalElements(), pagina.getTotalPages())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<MiPostulacionDetalleResponse>> detalle(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(misPostulacionesService.detalle(usuarioCodigo, codigo),
                "Postulación obtenida correctamente"));
    }
}
