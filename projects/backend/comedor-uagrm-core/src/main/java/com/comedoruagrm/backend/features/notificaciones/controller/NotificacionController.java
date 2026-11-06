package com.comedoruagrm.backend.features.notificaciones.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * Notificaciones (CU-EXTRA-01). Por ahora solo el aviso de prueba del Administrador DUBSS (lo
 * exige el `service`); los endpoints de la bandeja quedan para otro trabajo.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping("/prueba")
    public ResponseEntity<ApiResponse<Void>> enviarPrueba(@CurrentUserId UUID userId) {
        notificacionService.enviarPrueba(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "La notificación de prueba se está enviando"));
    }
}
