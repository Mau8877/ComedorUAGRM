package com.comedoruagrm.backend.features.auth.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.auth.dto.LoginRequest;
import com.comedoruagrm.backend.features.auth.dto.RefreshTokenRequest;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.AuthService;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.security.AuthCookies;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Autenticacion con JWT propio: registro, login, renovacion y cierre de
 * sesion. Registro, login, refresh y logout son publicos (ver RutasPublicas);
 * el logout se identifica por el refresh token (cuerpo en mobile,
 * cookie HttpOnly en la web). Registro, login y refresh tambien escriben las
 * cookies de sesion (ver AuthCookies); el cuerpo igual trae los tokens para mobile.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<SesionResponse>> registro(
            @Valid @RequestBody RegistroRequest request, HttpServletResponse response) {
        RegistroResultado resultado = authService.registrar(request);
        escribirCookies(response, resultado.sesion());
        HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(ApiResponse.success(resultado.sesion(), "Registro exitoso"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SesionResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        SesionResponse sesion = authService.iniciarSesion(request);
        escribirCookies(response, sesion);
        return ResponseEntity.ok(ApiResponse.success(sesion, "Sesión iniciada correctamente"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SesionResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshDeCookie,
            HttpServletResponse response) {
        String refreshToken = elegirRefreshToken(request, refreshDeCookie);
        if (refreshToken == null) {
            throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_06, "Tu sesión expiró. Inicia sesión de nuevo.");
        }
        SesionResponse sesion = authService.renovar(refreshToken);
        escribirCookies(response, sesion);
        return ResponseEntity.ok(ApiResponse.success(sesion, "Sesión renovada correctamente"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshDeCookie,
            HttpServletResponse response) {
        String refreshToken = elegirRefreshToken(request, refreshDeCookie);
        if (refreshToken != null) {
            authService.cerrarSesion(refreshToken);
        }
        authCookies.borrar(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

    // El cuerpo (mobile) gana sobre la cookie (web).
    private String elegirRefreshToken(RefreshTokenRequest request, String refreshDeCookie) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        return refreshDeCookie == null || refreshDeCookie.isBlank() ? null : refreshDeCookie;
    }

    private void escribirCookies(HttpServletResponse response, SesionResponse sesion) {
        authCookies.escribir(response, sesion.accessToken(), sesion.expiresIn(), sesion.refreshToken());
    }

    @GetMapping("/usuario-actual")
    public ResponseEntity<ApiResponse<UsuarioResponse>> usuarioActual(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.usuarioActual(userId), "Usuario obtenido correctamente"));
    }
}
