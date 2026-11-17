package com.comedoruagrm.backend.features.preseleccion.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.LoteReprocesamientoResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ResumenReprocesamientoResponse;
import com.comedoruagrm.backend.features.preseleccion.service.ReprocesamientoService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * El reprocesamiento de puntajes de una convocatoria (CU26): resumen previo, arranque del lote y su avance. El rol
 * ADMINISTRADOR_DUBSS lo comprueba el `service`; quien actua sale del token (`@CurrentUserId`).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReprocesamientoController {

    private final ReprocesamientoService reprocesamientoService;

    @GetMapping("/convocatorias/{codigo}/resumen-reprocesamiento")
    public ResponseEntity<ApiResponse<ResumenReprocesamientoResponse>> resumen(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(reprocesamientoService.resumen(codigo),
                "Resumen del reprocesamiento obtenido correctamente"));
    }

    /** `202`: el lote queda creado y se ejecuta en segundo plano; el avance se consulta en `lotes-reprocesamiento`. */
    @PostMapping("/convocatorias/{codigo}/reprocesar")
    public ResponseEntity<ApiResponse<LoteReprocesamientoResponse>> reprocesar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        LoteReprocesamientoResponse lote = reprocesamientoService.reprocesar(actorCodigo, codigo).datos();
        return ResponseEntity.accepted().body(ApiResponse.success(lote, "Reprocesamiento iniciado"));
    }

    @GetMapping("/lotes-reprocesamiento/{codigo}")
    public ResponseEntity<ApiResponse<LoteReprocesamientoResponse>> lote(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(reprocesamientoService.lote(codigo),
                "Reprocesamiento obtenido correctamente"));
    }
}
