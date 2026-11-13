package com.comedoruagrm.backend.features.dashboard.controller;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.service.FiltrosDashboardService;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresPdfService;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresPdfService.PdfGenerado;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Dashboard de indicadores BI del Directivo (CU12). Todos los endpoints exigen el rol DIRECTIVO (lo comprueba el
 * `service`). El PDF es la unica respuesta fuera del sobre estandar: el exito es el archivo; los errores siguen
 * saliendo en el sobre.
 *
 * * Solo lectura: no hay rutas para refrescar la vista ni para escribir.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final String FILTRO_GESTION = "filter[gestion]";
    private static final String FILTRO_CONVOCATORIA = "filter[convocatoria]";
    private static final Set<String> FILTROS = Set.of(FILTRO_GESTION, FILTRO_CONVOCATORIA);

    private final FiltrosDashboardService filtrosService;
    private final IndicadoresService indicadoresService;
    private final IndicadoresPdfService pdfService;

    @GetMapping("/filtros")
    public ResponseEntity<ApiResponse<FiltrosDashboardResponse>> filtros() {
        return ResponseEntity.ok(ApiResponse.success(filtrosService.obtener(),
                "Filtros del dashboard obtenidos correctamente"));
    }

    @GetMapping("/indicadores")
    public ResponseEntity<ApiResponse<IndicadoresResponse>> indicadores(
            @RequestParam(name = FILTRO_GESTION, required = false) String gestion,
            @RequestParam(name = FILTRO_CONVOCATORIA, required = false) String convocatoria,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS);
        IndicadoresResponse indicadores = indicadoresService.consultar(uuid(FILTRO_GESTION, gestion),
                uuid(FILTRO_CONVOCATORIA, convocatoria));
        return ResponseEntity.ok(ApiResponse.success(indicadores, "Indicadores obtenidos correctamente"));
    }

    @GetMapping("/indicadores/pdf")
    public ResponseEntity<byte[]> indicadoresPdf(@CurrentUserId UUID actorCodigo,
            @RequestParam(name = FILTRO_GESTION, required = false) String gestion,
            @RequestParam(name = FILTRO_CONVOCATORIA, required = false) String convocatoria,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS);
        PdfGenerado pdf = pdfService.generar(actorCodigo, uuid(FILTRO_GESTION, gestion),
                uuid(FILTRO_CONVOCATORIA, convocatoria));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pdf.nombreArchivo()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(pdf.contenido());
    }

    /** Un filtro vacio o ausente es «sin filtro»; un valor que no es UUID es un error del cliente. */
    private static UUID uuid(String filtro, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("ERR_SYS_05", "El filtro «" + filtro + "» debe ser un código válido");
        }
    }
}
