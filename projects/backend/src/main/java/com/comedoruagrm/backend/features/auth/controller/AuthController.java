package com.comedoruagrm.backend.features.auth.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.service.AuthService;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.CurrentUserId;
import com.comedoruagrm.backend.security.FirebaseSinRegistro;
import com.comedoruagrm.backend.security.UsuarioAutenticado;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * El login lo hace Firebase: acá solo se completa el registro del dominio y,
 * ya con sesion en Firebase, se devuelve el usuario con su rol. No hay
 * endpoint de login ni de logout (el backend es stateless).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<UsuarioResponse>> registro(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody RegistroRequest request,
            HttpServletRequest http) {
        if (principal instanceof FirebaseSinRegistro token) {
            RegistroResultado resultado = authService.registrar(token, request, http.getRemoteAddr());
            HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(estado).body(ApiResponse.success(resultado.usuario(), "Registro exitoso"));
        }
        if (principal instanceof UsuarioAutenticado usuario) {
            // Registro repetido de una cuenta que ya existe: idempotente, no crea nada.
            return ResponseEntity.ok(ApiResponse.success(
                    authService.registroExistente(usuario.codigo()), "La cuenta ya estaba registrada"));
        }
        throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_01, "Tu sesión no es válida. Inicia sesión de nuevo.");
    }

    @PostMapping("/sesion")
    public ResponseEntity<ApiResponse<UsuarioResponse>> sesion(@CurrentUserId UUID userId, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.iniciarSesion(userId, http.getRemoteAddr()), "Sesión iniciada correctamente"));
    }

    @GetMapping("/usuario-actual")
    public ResponseEntity<ApiResponse<UsuarioResponse>> usuarioActual(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.usuarioActual(userId), "Usuario obtenido correctamente"));
    }
}
