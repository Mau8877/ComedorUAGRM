package com.comedoruagrm.backend.features.planillas.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.planillas.dto.AccesoPlanillaResponse;
import com.comedoruagrm.backend.features.planillas.dto.GenerarPlanillaRequest;
import com.comedoruagrm.backend.features.planillas.dto.PlanillaResponse;
import com.comedoruagrm.backend.features.planillas.dto.ResumenPlanillaResponse;
import com.comedoruagrm.backend.features.planillas.service.ConsultaPlanillasService;
import com.comedoruagrm.backend.features.planillas.service.PlanillaAsistenciaService;
import com.comedoruagrm.backend.features.planillas.service.PlanillaAsistenciaService.VistaPreviaPlanilla;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Planilla de asistencia imprimible (CU09): resumen del padron vigente, generar, vista previa, listar y descargar.
 * Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el `service`). La vista previa es la unica
 * respuesta fuera del sobre estandar: el exito es el PDF; los errores siguen saliendo en el sobre.
 *
 * * Una planilla es un registro de lo impreso: no hay rutas para editarla ni eliminarla (`PUT`, `PATCH` y `DELETE`
 * * responden `405`).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlanillaController {

    // El listado de planillas no tiene filtros ni busqueda: cualquier `filter[...]` es un error del cliente.
    private static final Set<String> SIN_FILTROS = Set.of();

    private final PlanillaAsistenciaService generacionService;
    private final ConsultaPlanillasService consultaService;

    @GetMapping("/convocatorias/{codigo}/planillas/resumen")
    public ResponseEntity<ApiResponse<ResumenPlanillaResponse>> resumen(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(generacionService.resumen(codigo),
                "Resumen de la planilla obtenido correctamente"));
    }

    @GetMapping("/convocatorias/{codigo}/planillas")
    public ResponseEntity<ApiResponse<List<PlanillaResponse>>> listar(
            @PathVariable UUID codigo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, SIN_FILTROS);
        Page<PlanillaResponse> planillas = consultaService.listar(codigo,
                ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(ApiResponse.successList(planillas.getContent(),
                "Planillas obtenidas correctamente", new PageMeta(planillas.getNumber() + 1, planillas.getSize(),
                        planillas.getTotalElements(), planillas.getTotalPages())));
    }

    @PostMapping("/convocatorias/{codigo}/planillas")
    public ResponseEntity<ApiResponse<PlanillaResponse>> generar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody GenerarPlanillaRequest request) {
        PlanillaResponse planilla = generacionService
                .generar(actorCodigo, codigo, request.fechaDesde(), request.fechaHasta()).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planilla, "Planilla generada correctamente"));
    }

    @GetMapping("/convocatorias/{codigo}/planillas/vista-previa")
    public ResponseEntity<byte[]> vistaPrevia(@PathVariable UUID codigo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        VistaPreviaPlanilla vista = generacionService.vistaPrevia(codigo, desde, hasta);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(vista.nombreArchivo()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(vista.contenido());
    }

    @GetMapping("/planillas/{codigo}/descarga")
    public ResponseEntity<ApiResponse<AccesoPlanillaResponse>> descargar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(consultaService.descargar(actorCodigo, codigo).datos(),
                "Dirección de descarga generada correctamente"));
    }
}
