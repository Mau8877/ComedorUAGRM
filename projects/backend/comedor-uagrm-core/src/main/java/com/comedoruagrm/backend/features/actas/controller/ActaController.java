package com.comedoruagrm.backend.features.actas.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.actas.dto.AccesoActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaPendienteActaResponse;
import com.comedoruagrm.backend.features.actas.service.ActaAdjudicacionService;
import com.comedoruagrm.backend.features.actas.service.ActaAdjudicacionService.VistaPreviaActa;
import com.comedoruagrm.backend.features.actas.service.ConsultaActasService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Acta de adjudicacion (CU08): generar, ver la vista previa, listar, consultar y descargar. Todos los endpoints
 * exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el `service`). La vista previa es la unica respuesta fuera del
 * sobre estandar: el exito es el PDF; los errores siguen saliendo en el sobre.
 *
 * * El acta es definitiva: no hay rutas para regenerarla, reemplazarla ni eliminarla (`PUT`, `PATCH` y
 * * `DELETE` responden `405`).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActaController {

    // Las listas de actas no tienen filtros: cualquier `filter[...]` es un error del cliente.
    private static final Set<String> SIN_FILTROS = Set.of();

    private final ActaAdjudicacionService generacionService;
    private final ConsultaActasService consultaService;

    @GetMapping("/actas")
    public ResponseEntity<ApiResponse<List<ActaResponse>>> listar(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, SIN_FILTROS);
        Page<ActaResponse> actas = consultaService.listar(search, ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(ApiResponse.successList(actas.getContent(), "Actas obtenidas correctamente",
                meta(actas)));
    }

    @GetMapping("/actas/pendientes")
    public ResponseEntity<ApiResponse<List<ConvocatoriaPendienteActaResponse>>> pendientes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, SIN_FILTROS);
        Page<ConvocatoriaPendienteActaResponse> pendientes = consultaService
                .pendientes(ParametrosListado.pageable(page, pageSize, null));
        return ResponseEntity.ok(ApiResponse.successList(pendientes.getContent(),
                "Convocatorias pendientes de acta obtenidas correctamente", meta(pendientes)));
    }

    @GetMapping("/convocatorias/{codigo}/acta")
    public ResponseEntity<ApiResponse<ActaResponse>> obtener(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(consultaService.obtener(codigo),
                "Acta obtenida correctamente"));
    }

    @GetMapping("/convocatorias/{codigo}/acta/descarga")
    public ResponseEntity<ApiResponse<AccesoActaResponse>> descargar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(consultaService.descargar(actorCodigo, codigo).datos(),
                "Dirección de descarga generada correctamente"));
    }

    @GetMapping("/convocatorias/{codigo}/acta/vista-previa")
    public ResponseEntity<byte[]> vistaPrevia(@PathVariable UUID codigo) {
        VistaPreviaActa vista = generacionService.vistaPrevia(codigo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(vista.nombreArchivo()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(vista.contenido());
    }

    @PostMapping("/convocatorias/{codigo}/acta")
    public ResponseEntity<ApiResponse<ActaResponse>> generar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        ActaResponse acta = generacionService.generar(actorCodigo, codigo).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(acta, "Acta generada correctamente"));
    }

    private static PageMeta meta(Page<?> pagina) {
        return new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
