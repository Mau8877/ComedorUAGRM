package com.comedoruagrm.backend.features.convocatorias.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaRequest;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaResponse;
import com.comedoruagrm.backend.features.convocatorias.service.GestionAcademicaService;
import com.comedoruagrm.backend.features.convocatorias.service.GestionAcademicaService.PaginaGestiones;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestiones academicas (CU13). Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el
 * `service`); el administrador que actua sale del token (`@CurrentUserId`), nunca del body.
 */
@RestController
@RequestMapping("/api/v1/gestiones-academicas")
@RequiredArgsConstructor
public class GestionAcademicaController {

    private final GestionAcademicaService gestionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GestionAcademicaResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, Set.of());
        PaginaGestiones pagina = gestionService.listar(page, pageSize, search, sort);
        return ResponseEntity.ok(ApiResponse.successList(pagina.datos(),
                "Gestiones académicas obtenidas correctamente", pagina.meta()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GestionAcademicaResponse>> crear(
            @CurrentUserId UUID actorCodigo, @Valid @RequestBody GestionAcademicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                gestionService.crear(actorCodigo, request).datos(), "Gestión académica creada correctamente"));
    }

    // PUT y no PATCH: la pantalla manda la gestion completa (los tres campos son obligatorios).
    @PutMapping("/{codigo}")
    public ResponseEntity<ApiResponse<GestionAcademicaResponse>> editar(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody GestionAcademicaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                gestionService.editar(actorCodigo, codigo, request).datos(),
                "Gestión académica actualizada correctamente"));
    }
}
