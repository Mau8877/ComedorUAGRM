package com.comedoruagrm.backend.features.preseleccion.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.preseleccion.dto.GuardarModeloRequest;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloPreseleccionResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloPreseleccionResumenResponse;
import com.comedoruagrm.backend.features.preseleccion.service.ModeloPreseleccionService;
import com.comedoruagrm.backend.features.preseleccion.service.ModeloPreseleccionService.PaginaModelos;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las versiones del modelo de preseleccion (CU26): historial, version vigente y guardado de una version nueva. El
 * rol ADMINISTRADOR_DUBSS lo comprueba el `service`; quien actua sale del token (`@CurrentUserId`).
 */
@RestController
@RequestMapping("/api/v1/modelos-preseleccion")
@RequiredArgsConstructor
public class ModeloPreseleccionController {

    private final ModeloPreseleccionService modeloService;

    /** El historial, de la version mas reciente a la mas antigua. No admite `search`, `filter` ni `sort`. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModeloPreseleccionResumenResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, Set.of());
        PaginaModelos pagina = modeloService.listar(page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.datos(), "Versiones obtenidas correctamente",
                pagina.meta()));
    }

    /** La version vigente; `data` es `null` si todavia no se guardo ninguna. */
    @GetMapping("/vigente")
    public ResponseEntity<ApiResponse<ModeloPreseleccionResponse>> vigente() {
        return ResponseEntity.ok(ApiResponse.success(modeloService.vigente(),
                "Versión vigente obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ModeloPreseleccionResponse>> guardar(@CurrentUserId UUID actorCodigo,
            @Valid @RequestBody GuardarModeloRequest request) {
        ModeloPreseleccionResponse guardada = modeloService.guardar(actorCodigo, request).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(guardada, "Versión " + guardada.version() + " guardada correctamente"));
    }
}
