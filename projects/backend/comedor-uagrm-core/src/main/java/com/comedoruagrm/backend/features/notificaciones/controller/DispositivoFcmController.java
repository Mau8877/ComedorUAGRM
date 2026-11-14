package com.comedoruagrm.backend.features.notificaciones.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.notificaciones.dto.BajaDispositivoRequest;
import com.comedoruagrm.backend.features.notificaciones.dto.DispositivoResponse;
import com.comedoruagrm.backend.features.notificaciones.dto.RegistrarDispositivoRequest;
import com.comedoruagrm.backend.features.notificaciones.service.DispositivoFcmService;
import com.comedoruagrm.backend.features.notificaciones.service.DispositivoFcmService.DispositivoRegistrado;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Telefonos que reciben push (CU-EXTRA-01). Cualquier usuario autenticado registra y da de baja
 * los suyos; el usuario sale siempre del token (`@CurrentUserId`), nunca del body.
 */
@RestController
@RequestMapping("/api/v1/dispositivos-fcm")
@RequiredArgsConstructor
public class DispositivoFcmController {

    private final DispositivoFcmService dispositivoService;

    @PostMapping
    public ResponseEntity<ApiResponse<DispositivoResponse>> registrar(
            @CurrentUserId UUID userId, @Valid @RequestBody RegistrarDispositivoRequest request) {
        DispositivoRegistrado registrado = dispositivoService.registrar(userId, request.token(), request.plataforma());
        HttpStatus estado = registrado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(ApiResponse.success(
                new DispositivoResponse(registrado.codigo(), registrado.plataforma()),
                "Dispositivo registrado correctamente"));
    }

    @PostMapping("/baja")
    public ResponseEntity<ApiResponse<Void>> darDeBaja(
            @CurrentUserId UUID userId, @Valid @RequestBody BajaDispositivoRequest request) {
        dispositivoService.darDeBaja(userId, request.token());
        return ResponseEntity.ok(ApiResponse.success(null, "Dispositivo dado de baja correctamente"));
    }
}
