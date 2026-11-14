package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ConvocatoriaPriorizableResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ExportarListaRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ResumenListaPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaPdfService;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaPdfService.PdfGenerado;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaService;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaService.PaginaListaPriorizada;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Lista priorizada de postulantes (CU06). Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo
 * comprueba el `service`). La exportacion a PDF es la unica respuesta fuera del sobre estandar: el
 * exito es el archivo; los errores siguen saliendo en el sobre.
 */
@RestController
@RequestMapping("/api/v1/lista-priorizada")
@RequiredArgsConstructor
public class ListaPriorizadaController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[convocatoriaId]", "filter[carreraId]");

    private final ListaPriorizadaService listaPriorizadaService;
    private final ListaPriorizadaPdfService pdfService;

    @GetMapping("/convocatorias")
    public ResponseEntity<ApiResponse<List<ConvocatoriaPriorizableResponse>>> convocatorias() {
        return ResponseEntity.ok(ApiResponse.success(
                listaPriorizadaService.listarConvocatorias(), "Convocatorias obtenidas correctamente"));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<ResumenListaPriorizadaResponse>> resumen(
            @RequestParam(name = "filter[convocatoriaId]") UUID convocatoriaId, HttpServletRequest request) {
        validarFiltros(request);
        return ResponseEntity.ok(ApiResponse.success(
                listaPriorizadaService.resumir(convocatoriaId), "Resumen obtenido correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostulacionPriorizadaResponse>>> listarPriorizados(
            @RequestParam(name = "filter[convocatoriaId]") UUID convocatoriaId,
            @RequestParam(name = "filter[carreraId]", required = false) UUID carreraId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        validarFiltros(request);
        PaginaListaPriorizada pagina = listaPriorizadaService.listarPriorizados(
                convocatoriaId, search, carreraId, page, pageSize, sort);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Lista priorizada obtenida correctamente", pagina.meta()));
    }

    @PostMapping("/exportaciones")
    public ResponseEntity<byte[]> exportar(
            @CurrentUserId UUID actorCodigo, @Valid @RequestBody ExportarListaRequest request) {
        PdfGenerado pdf = pdfService.exportar(actorCodigo, request).datos();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pdf.nombreArchivo()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(pdf.contenido());
    }

    // Un filter[...] que no es soportado se rechaza (no se ignora en silencio).
    private void validarFiltros(HttpServletRequest request) {
        request.getParameterMap().keySet().stream()
                .filter(nombre -> nombre.startsWith("filter[") && !FILTROS_SOPORTADOS.contains(nombre))
                .findFirst()
                .ifPresent(nombre -> {
                    throw new ValidationException(PriorizacionErrorCodes.ERR_PRI_02,
                            "El filtro «" + nombre + "» no está soportado. Filtros válidos: "
                                    + "filter[convocatoriaId], filter[carreraId]");
                });
    }
}
