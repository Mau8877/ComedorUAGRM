package com.comedoruagrm.backend.features.expedientes.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.expedientes.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse;
import com.comedoruagrm.backend.features.expedientes.dto.RegistrarAuditoriaRequest;
import com.comedoruagrm.backend.features.expedientes.dto.ResultadoAuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.service.ExpedienteService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Expediente de un finalista (CU07). Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el
 * `service`). Cuelgan de `/api/v1/finalistas/{codigo}` porque el expediente no existe sin el finalista.
 */
@RestController
@RequestMapping("/api/v1/finalistas/{codigo}")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteService expedienteService;

    @GetMapping("/expediente")
    public ResponseEntity<ApiResponse<ExpedienteResponse>> obtener(@PathVariable UUID codigo) {
        return ResponseEntity.ok(
                ApiResponse.success(expedienteService.obtener(codigo), "Expediente obtenido correctamente"));
    }

    /**
     * `201` si se creó la auditoría; `200` si fue una repetición idempotente (misma `Idempotency-Key`).
     * El header es opcional: sin él siempre se crea una auditoría nueva.
     */
    @PostMapping("/auditorias")
    public ResponseEntity<ApiResponse<ResultadoAuditoriaResponse>> registrarAuditoria(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo,
            @RequestHeader(name = "Idempotency-Key", required = false) String claveIdempotencia,
            @Valid @RequestBody RegistrarAuditoriaRequest request) {
        ResultadoAuditoriaResponse resultado = expedienteService
                .registrarAuditoria(actorCodigo, codigo, claveIdempotencia, request).datos();
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(resultado, "Auditoría registrada correctamente"));
    }

    @GetMapping("/documentos/{documentoCodigo}/acceso")
    public ResponseEntity<ApiResponse<AccesoDocumentoResponse>> accesoDocumento(
            @PathVariable UUID codigo, @PathVariable UUID documentoCodigo) {
        return ResponseEntity.ok(ApiResponse.success(expedienteService.accesoDocumento(codigo, documentoCodigo),
                "Acceso al documento generado"));
    }
}
