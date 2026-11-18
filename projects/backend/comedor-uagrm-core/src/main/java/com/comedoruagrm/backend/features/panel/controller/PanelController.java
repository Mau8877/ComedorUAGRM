package com.comedoruagrm.backend.features.panel.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelAdministradorResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelComedorResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelPostulanteResponse;
import com.comedoruagrm.backend.features.panel.service.PanelService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * Pantallas de inicio de los roles Administrador DUBSS, Personal del Comedor y Postulante. Solo lectura; el rol de
 * cada una lo comprueba el `service` (`403` `ERR_AUTH_07` con otro rol).
 */
@RestController
@RequestMapping("/api/v1/panel")
@RequiredArgsConstructor
public class PanelController {

    private final PanelService service;

    @GetMapping("/administrador")
    public ResponseEntity<ApiResponse<PanelAdministradorResponse>> administrador() {
        return ResponseEntity.ok(ApiResponse.success(service.administrador(), "Panel obtenido correctamente"));
    }

    @GetMapping("/comedor")
    public ResponseEntity<ApiResponse<PanelComedorResponse>> comedor(@CurrentUserId UUID actorCodigo) {
        return ResponseEntity.ok(ApiResponse.success(service.comedor(actorCodigo), "Panel obtenido correctamente"));
    }

    @GetMapping("/postulante")
    public ResponseEntity<ApiResponse<PanelPostulanteResponse>> postulante(@CurrentUserId UUID actorCodigo) {
        return ResponseEntity.ok(ApiResponse.success(service.postulante(actorCodigo), "Panel obtenido correctamente"));
    }
}
