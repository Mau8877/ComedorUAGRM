package com.comedoruagrm.backend.features.resultados.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.resultados.dto.DecisionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.dto.PublicacionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ResumenPublicacionResponse;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.FiltroDecisiones;
import com.comedoruagrm.backend.features.resultados.service.PublicacionResultadosService;
import com.comedoruagrm.backend.features.resultados.service.ResultadosAdminService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Publicar los resultados de adjudicacion y revisarlos antes (CU22). Exige el rol ADMINISTRADOR_DUBSS (lo
 * comprueba el `service`). Cuelga de `/api/v1/convocatorias/{codigo}/resultados`.
 */
@RestController
@RequestMapping("/api/v1/convocatorias/{codigo}/resultados")
@RequiredArgsConstructor
public class ResultadosAdminController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[resultado]");

    private final ResultadosAdminService adminService;
    private final PublicacionResultadosService publicacionService;

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<ResumenPublicacionResponse>> resumen(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(adminService.resumen(codigo),
                "Resumen de la publicación obtenido correctamente"));
    }

    @GetMapping("/decisiones")
    public ResponseEntity<ApiResponse<List<DecisionResultadoResponse>>> decisiones(
            @PathVariable UUID codigo,
            @RequestParam(name = "filter[resultado]", required = false) String resultado,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS_SOPORTADOS);
        PaginaResultados<DecisionResultadoResponse> pagina = adminService.decisiones(codigo,
                new FiltroDecisiones(resultado, search), ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Decisiones obtenidas correctamente", pagina.meta()));
    }

    @PostMapping("/publicar")
    public ResponseEntity<ApiResponse<PublicacionResultadoResponse>> publicar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        PublicacionResultadoResponse publicacion = publicacionService.publicar(actorCodigo, codigo).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(publicacion, "Resultados publicados correctamente"));
    }
}
