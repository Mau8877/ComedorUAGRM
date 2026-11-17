package com.comedoruagrm.backend.features.convocatorias.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaRequest;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResumenResponse;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaEstadoService;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaService;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaService.PaginaConvocatorias;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestion de convocatorias (CU13). Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el
 * `service`, ver SEGURIDAD_AUTH_BACKEND.md); el administrador que actua sale del token (`@CurrentUserId`),
 * nunca del body.
 */
@RestController
@RequestMapping("/api/v1/convocatorias")
@RequiredArgsConstructor
public class ConvocatoriaController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[estado]", "filter[gestion]");

    private final ConvocatoriaService convocatoriaService;
    private final ConvocatoriaEstadoService estadoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConvocatoriaResumenResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[estado]", required = false) String estado,
            @RequestParam(name = "filter[gestion]", required = false) String gestion,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS_SOPORTADOS);
        PaginaConvocatorias pagina = convocatoriaService.listar(
                page, pageSize, search, aEstado(estado), aCodigo(gestion), sort);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Convocatorias obtenidas correctamente", pagina.meta()));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<ConvocatoriaResponse>> obtener(@PathVariable UUID codigo) {
        return ResponseEntity.ok(
                ApiResponse.success(convocatoriaService.obtener(codigo), "Convocatoria obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConvocatoriaResponse>> crear(
            @CurrentUserId UUID actorCodigo, @Valid @RequestBody ConvocatoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                convocatoriaService.crear(actorCodigo, request).datos(), "Convocatoria creada correctamente"));
    }

    // PUT y no PATCH: el formulario manda la convocatoria completa y los campos opcionales omitidos se vacian.
    @PutMapping("/{codigo}")
    public ResponseEntity<ApiResponse<ConvocatoriaResponse>> editar(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody ConvocatoriaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                convocatoriaService.editar(actorCodigo, codigo, request).datos(),
                "Convocatoria actualizada correctamente"));
    }

    @PostMapping("/{codigo}/abrir")
    public ResponseEntity<ApiResponse<ConvocatoriaResponse>> abrir(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
                estadoService.abrir(actorCodigo, codigo).datos(), "Convocatoria abierta correctamente"));
    }

    @PostMapping("/{codigo}/cerrar")
    public ResponseEntity<ApiResponse<ConvocatoriaResponse>> cerrar(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
                estadoService.cerrar(actorCodigo, codigo).datos(), "Convocatoria cerrada correctamente"));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@CurrentUserId UUID actorCodigo, @PathVariable UUID codigo) {
        convocatoriaService.eliminar(actorCodigo, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Convocatoria eliminada correctamente"));
    }

    // Un valor de filtro que no es del enum se rechaza (no se ignora en silencio).
    private EstadoConvocatoria aEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return EstadoConvocatoria.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("ERR_SYS_05",
                    "filter[estado] debe ser BORRADOR, ABIERTA, CERRADA o PUBLICADA");
        }
    }

    private UUID aCodigo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("ERR_SYS_05", "filter[gestion] debe ser el código de una gestión");
        }
    }
}
