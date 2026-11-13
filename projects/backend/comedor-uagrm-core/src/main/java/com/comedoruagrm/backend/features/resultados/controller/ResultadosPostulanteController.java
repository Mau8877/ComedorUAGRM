package com.comedoruagrm.backend.features.resultados.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.resultados.dto.AdjudicadoPublicoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaPublicadaResponse;
import com.comedoruagrm.backend.features.resultados.dto.MiResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.service.ResultadosPostulanteService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Lo que el postulante consulta tras la publicacion de resultados (CU22): las convocatorias publicadas, la lista
 * de adjudicados y su propio resultado. Basta una sesion valida (lo comprueba el `service`).
 */
@RestController
@RequiredArgsConstructor
public class ResultadosPostulanteController {

    private final ResultadosPostulanteService postulanteService;

    @GetMapping("/api/v1/resultados/convocatorias")
    public ResponseEntity<ApiResponse<List<ConvocatoriaPublicadaResponse>>> convocatorias(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, Set.of());
        PaginaResultados<ConvocatoriaPublicadaResponse> pagina = postulanteService
                .convocatoriasPublicadas(ParametrosListado.pageable(page, pageSize, null));
        return ResponseEntity.ok(ApiResponse.successList(pagina.datos(),
                "Convocatorias con resultados publicados obtenidas correctamente", pagina.meta()));
    }

    @GetMapping("/api/v1/convocatorias/{codigo}/resultados")
    public ResponseEntity<ApiResponse<List<AdjudicadoPublicoResponse>>> adjudicados(
            @PathVariable UUID codigo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, Set.of());
        PaginaResultados<AdjudicadoPublicoResponse> pagina = postulanteService.adjudicados(codigo, search,
                ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Adjudicados obtenidos correctamente", pagina.meta()));
    }

    @GetMapping("/api/v1/convocatorias/{codigo}/resultados/mio")
    public ResponseEntity<ApiResponse<MiResultadoResponse>> miResultado(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(postulanteService.miResultado(codigo, usuarioCodigo),
                "Resultado obtenido correctamente"));
    }
}
