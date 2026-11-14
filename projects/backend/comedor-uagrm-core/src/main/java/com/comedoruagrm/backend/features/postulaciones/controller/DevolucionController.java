package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DevolucionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DevolverPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.service.DevolucionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Devolucion de la postulacion de un finalista (CU19). Exige el rol ADMINISTRADOR_DUBSS (lo comprueba el
 * `service`). Cuelga de `/api/v1/finalistas/{codigo}` porque se llega desde su expediente (CU07).
 */
@RestController
@RequestMapping("/api/v1/finalistas/{codigo}")
@RequiredArgsConstructor
public class DevolucionController {

    private final DevolucionService devolucionService;

    @PostMapping("/devolver")
    public ResponseEntity<ApiResponse<DevolucionResponse>> devolver(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody DevolverPostulacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                devolucionService.devolver(actorCodigo, codigo, request).datos(),
                "Postulación devuelta correctamente"));
    }
}
