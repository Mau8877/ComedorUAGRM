package com.comedoruagrm.backend.features.auth.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.auth.dto.LoginRequest;
import com.comedoruagrm.backend.features.auth.dto.RefreshTokenRequest;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.AuthService;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Autenticacion con JWT propio: registro, login, renovacion y cierre de
 * sesion. Registro, login, refresh y logout son publicos (ver RutasPublicas);
 * el logout se identifica por el refresh token del cuerpo.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<SesionResponse>> registro(
            @Valid @RequestBody RegistroRequest request, HttpServletRequest http) {
        RegistroResultado resultado = authService.registrar(request, http.getRemoteAddr());
        HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(ApiResponse.success(resultado.sesion(), "Registro exitoso"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SesionResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.iniciarSesion(request, http.getRemoteAddr()), "Sesión iniciada correctamente"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SesionResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.renovar(request.refreshToken()), "Sesión renovada correctamente"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.cerrarSesion(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

    @GetMapping("/usuario-actual")
    public ResponseEntity<ApiResponse<UsuarioResponse>> usuarioActual(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.usuarioActual(userId), "Usuario obtenido correctamente"));
    }
}
