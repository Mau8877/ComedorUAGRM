package com.comedoruagrm.backend.features.finalistas.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;
import com.comedoruagrm.backend.features.finalistas.dto.NotificarFinalistasRequest;
import com.comedoruagrm.backend.features.finalistas.dto.RegistrarEntregaRequest;
import com.comedoruagrm.backend.features.finalistas.dto.ResultadoNotificacionResponse;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.finalistas.service.FinalistaService;
import com.comedoruagrm.backend.features.finalistas.service.FinalistaService.PaginaFinalistas;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Finalistas (CU21). Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el `service`). */
@RestController
@RequestMapping("/api/v1/finalistas")
@RequiredArgsConstructor
public class FinalistaController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[convocatoriaId]");

    private final FinalistaService finalistaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FinalistaResponse>>> listar(
            @RequestParam(name = "filter[convocatoriaId]") UUID convocatoriaId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        validarParametros(request, sort);
        PaginaFinalistas pagina = finalistaService.listar(convocatoriaId, search, page, pageSize);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Finalistas obtenidos correctamente", pagina.meta()));
    }

    @PostMapping("/notificaciones")
    public ResponseEntity<ApiResponse<ResultadoNotificacionResponse>> notificar(
            @CurrentUserId UUID actorCodigo, @Valid @RequestBody NotificarFinalistasRequest request) {
        ResultadoNotificacionResponse resultado = finalistaService.notificar(actorCodigo, request).datos();
        return ResponseEntity.ok(ApiResponse.success(resultado, "Notificación enviada a los finalistas"));
    }

    @PatchMapping("/{codigo}/entrega")
    public ResponseEntity<ApiResponse<FinalistaResponse>> registrarEntrega(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody RegistrarEntregaRequest request) {
        FinalistaResponse finalista = finalistaService.registrarEntrega(actorCodigo, codigo, request).datos();
        return ResponseEntity.ok(ApiResponse.success(finalista, "Entrega de documentación registrada"));
    }

    // Un filter[...] o un sort no soportado se rechaza (no se ignora en silencio).
    private void validarParametros(HttpServletRequest request, String sort) {
        if (sort != null && !sort.isBlank()) {
            throw new ValidationException(FinalistaErrorCodes.ERR_FIN_05,
                    "Los finalistas siempre van ordenados por posición: no admiten «sort»");
        }
        request.getParameterMap().keySet().stream()
                .filter(nombre -> nombre.startsWith("filter[") && !FILTROS_SOPORTADOS.contains(nombre))
                .findFirst()
                .ifPresent(nombre -> {
                    throw new ValidationException(FinalistaErrorCodes.ERR_FIN_05,
                            "El filtro «" + nombre + "» no está soportado. Filtro válido: filter[convocatoriaId]");
                });
    }
}
