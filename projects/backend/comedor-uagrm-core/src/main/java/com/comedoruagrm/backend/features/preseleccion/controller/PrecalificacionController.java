package com.comedoruagrm.backend.features.preseleccion.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.PrecalificacionResponse;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionDetalleService;

import lombok.RequiredArgsConstructor;

/**
 * La precalificacion con IA de una postulacion (CU03): puntaje, aporte de cada variable y traza. El rol
 * ADMINISTRADOR_DUBSS lo comprueba el `service`.
 */
@RestController
@RequestMapping("/api/v1/postulaciones")
@RequiredArgsConstructor
public class PrecalificacionController {

    private final PrecalificacionDetalleService detalleService;

    @GetMapping("/{codigo}/precalificacion")
    public ResponseEntity<ApiResponse<PrecalificacionResponse>> obtener(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(detalleService.obtener(codigo),
                "Precalificación obtenida correctamente"));
    }
}
