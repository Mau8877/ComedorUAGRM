package com.comedoruagrm.backend.features.cotejos.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
import com.comedoruagrm.backend.features.cotejos.dto.CotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.DatosCotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.RegistrarCotejoRequest;
import com.comedoruagrm.backend.features.cotejos.dto.RegistroDigitalResponse;
import com.comedoruagrm.backend.features.cotejos.service.ConsultaCotejosService;
import com.comedoruagrm.backend.features.cotejos.service.CotejoAsistenciaService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Cotejo del registro digital contra la planilla fisica (CU11): los datos de la planilla, los registros digitales de su
 * periodo, los cotejos ya registrados y el registro de uno nuevo. Todos los endpoints exigen el rol
 * ADMINISTRADOR_DUBSS (lo comprueba el `service`).
 *
 * * Un cotejo es un registro de lo que se constato: no hay rutas para editarlo ni eliminarlo (`PUT`, `PATCH` y
 * * `DELETE` responden `405`). Los listados no tienen filtros ni busqueda.
 */
@RestController
@RequestMapping("/api/v1/planillas/{codigo}/cotejos")
@RequiredArgsConstructor
public class CotejoController {

    // Los listados del cotejo no tienen filtros ni busqueda: cualquier `filter[...]` es un error del cliente.
    private static final Set<String> SIN_FILTROS = Set.of();

    private final ConsultaCotejosService consultaService;
    private final CotejoAsistenciaService cotejoService;

    @GetMapping("/datos")
    public ResponseEntity<ApiResponse<DatosCotejoResponse>> datos(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(consultaService.datos(codigo),
                "Datos del cotejo obtenidos correctamente"));
    }

    @GetMapping("/registros")
    public ResponseEntity<ApiResponse<List<RegistroDigitalResponse>>> registros(
            @PathVariable UUID codigo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, SIN_FILTROS);
        Page<RegistroDigitalResponse> registros = consultaService.registros(codigo,
                ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(ApiResponse.successList(registros.getContent(),
                "Registros digitales obtenidos correctamente", meta(registros)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CotejoResponse>>> listar(
            @PathVariable UUID codigo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, SIN_FILTROS);
        Page<CotejoResponse> cotejos = consultaService.cotejos(codigo,
                ParametrosListado.pageable(page, pageSize, sort));
        return ResponseEntity.ok(ApiResponse.successList(cotejos.getContent(), "Cotejos obtenidos correctamente",
                meta(cotejos)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CotejoResponse>> registrar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody RegistrarCotejoRequest request) {
        CotejoResponse cotejo = cotejoService.registrar(actorCodigo, codigo, request).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cotejo, "Cotejo registrado correctamente"));
    }

    private static PageMeta meta(Page<?> pagina) {
        return new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
