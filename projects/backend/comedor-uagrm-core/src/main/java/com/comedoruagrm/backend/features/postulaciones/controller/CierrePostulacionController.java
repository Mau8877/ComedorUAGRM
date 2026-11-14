package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.CierrePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.service.CierrePostulacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Archivar o rechazar la postulacion de un finalista (CU20). Exige el rol ADMINISTRADOR_DUBSS (lo comprueba
 * el `service`). Cuelga de `/api/v1/finalistas/{codigo}` porque se llega desde su expediente (CU07).
 */
@RestController
@RequestMapping("/api/v1/finalistas/{codigo}")
@RequiredArgsConstructor
public class CierrePostulacionController {

    private final CierrePostulacionService cierreService;

    @PostMapping("/archivar")
    public ResponseEntity<ApiResponse<CierrePostulacionResponse>> archivar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody CierrePostulacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cierreService.archivar(actorCodigo, codigo, request).datos(),
                "Postulación archivada correctamente"));
    }

    @PostMapping("/rechazar")
    public ResponseEntity<ApiResponse<CierrePostulacionResponse>> rechazar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody CierrePostulacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cierreService.rechazar(actorCodigo, codigo, request).datos(),
                "Postulación rechazada correctamente"));
    }
}
