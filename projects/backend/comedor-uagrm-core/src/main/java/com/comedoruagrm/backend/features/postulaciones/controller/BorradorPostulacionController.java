package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorGuardadoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.GuardarBorradorRequest;
import com.comedoruagrm.backend.features.postulaciones.service.BorradorPostulacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * Guardar y retomar el borrador de la postulacion (CU17). Exige el rol POSTULANTE (lo comprueba el `service`); el
 * postulante sale del token.
 *
 * * `PUT /api/v1/postulaciones/{codigo}/borrador`: guarda el avance del formulario (`200`).
 * * `GET /api/v1/postulaciones/mi-borrador`: el borrador de la convocatoria abierta (`200`; `data: null` si no hay).
 */
@RestController
@RequestMapping("/api/v1/postulaciones")
@RequiredArgsConstructor
public class BorradorPostulacionController {

    private final BorradorPostulacionService borradorService;

    @PutMapping("/{codigo}/borrador")
    public ResponseEntity<ApiResponse<BorradorGuardadoResponse>> guardar(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo, @RequestBody GuardarBorradorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(borradorService.guardar(usuarioCodigo, codigo, request),
                "Borrador guardado correctamente"));
    }

    @GetMapping("/mi-borrador")
    public ResponseEntity<ApiResponse<BorradorResumenResponse>> miBorrador(@CurrentUserId UUID usuarioCodigo) {
        return ResponseEntity.ok(ApiResponse.success(borradorService.obtener(usuarioCodigo).orElse(null),
                "Borrador obtenido correctamente"));
    }
}
