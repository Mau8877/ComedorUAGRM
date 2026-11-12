package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DecisionAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.NoAdjudicarRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.PanelAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.service.AdjudicacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Panel de adjudicacion de una convocatoria y decision final sobre un finalista, adjudicar o no adjudicar
 * (CU05). Exige el rol ADMINISTRADOR_DUBSS (lo comprueba el `service`). La decision cuelga de
 * `/api/v1/finalistas/{codigo}` porque se llega desde su expediente (CU07).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdjudicacionController {

    private final AdjudicacionService adjudicacionService;

    @GetMapping("/convocatorias/{codigo}/adjudicacion")
    public ResponseEntity<ApiResponse<PanelAdjudicacionResponse>> panel(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(adjudicacionService.panel(codigo),
                "Panel de adjudicación obtenido correctamente"));
    }

    @PostMapping("/finalistas/{codigo}/adjudicar")
    public ResponseEntity<ApiResponse<DecisionAdjudicacionResponse>> adjudicar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(adjudicacionService.adjudicar(actorCodigo, codigo).datos(),
                "Beca adjudicada correctamente"));
    }

    @PostMapping("/finalistas/{codigo}/no-adjudicar")
    public ResponseEntity<ApiResponse<DecisionAdjudicacionResponse>> noAdjudicar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody NoAdjudicarRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adjudicacionService.noAdjudicar(actorCodigo, codigo, request).datos(),
                "Postulación marcada como no adjudicada correctamente"));
    }
}
