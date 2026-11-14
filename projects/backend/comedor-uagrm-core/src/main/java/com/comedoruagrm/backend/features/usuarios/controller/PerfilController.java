package com.comedoruagrm.backend.features.usuarios.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarPerfilRequest;
import com.comedoruagrm.backend.features.usuarios.dto.PerfilResponse;
import com.comedoruagrm.backend.features.usuarios.service.PerfilService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Perfil del usuario autenticado (CU15), para cualquier rol. El usuario sale siempre del token
 * (`@CurrentUserId`), nunca del body. El cambio de contraseña vive en /api/v1/auth/cambiar-contrasena.
 */
@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    public ResponseEntity<ApiResponse<PerfilResponse>> obtener(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(perfilService.obtener(userId), "Perfil obtenido correctamente"));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<PerfilResponse>> actualizar(
            @CurrentUserId UUID userId, @Valid @RequestBody ActualizarPerfilRequest request) {
        PerfilResponse perfil = perfilService.actualizar(userId, request).datos();
        return ResponseEntity.ok(ApiResponse.success(perfil, "Perfil actualizado correctamente"));
    }
}
