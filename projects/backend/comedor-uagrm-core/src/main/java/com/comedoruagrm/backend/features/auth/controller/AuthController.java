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
import com.comedoruagrm.backend.features.auth.dto.CambiarContrasenaRequest;
import com.comedoruagrm.backend.features.auth.dto.RecuperarContrasenaRequest;
import com.comedoruagrm.backend.features.auth.dto.RefreshTokenRequest;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.dto.RestablecerContrasenaRequest;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.AuthService;
import com.comedoruagrm.backend.features.auth.service.AuthService.RegistroResultado;
import com.comedoruagrm.backend.features.auth.service.RestablecimientoContrasenaService;
import com.comedoruagrm.backend.features.usuarios.service.PerfilService;
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
 *
 * * Recuperar y restablecer la contraseña (CU15) son publicos; cambiar la contraseña exige
 * * sesion y vive aca (no bajo /perfil) porque la cookie `refresh_token` de la web solo viaja
 * * a /api/v1/auth: asi el backend sabe cual es la sesion actual y la conserva.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;
    private final RestablecimientoContrasenaService restablecimientoService;
    private final PerfilService perfilService;

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
        return elegirRefreshToken(request == null ? null : request.refreshToken(), refreshDeCookie);
    }

    private String elegirRefreshToken(String delCuerpo, String refreshDeCookie) {
        if (delCuerpo != null && !delCuerpo.isBlank()) {
            return delCuerpo;
        }
        return refreshDeCookie == null || refreshDeCookie.isBlank() ? null : refreshDeCookie;
    }

    private void escribirCookies(HttpServletResponse response, SesionResponse sesion) {
        authCookies.escribir(response, sesion.accessToken(), sesion.expiresIn(), sesion.refreshToken());
    }

    /** Siempre la misma respuesta, exista o no la cuenta (A1): no revela que correos estan registrados. */
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<ApiResponse<Void>> recuperarContrasena(
            @Valid @RequestBody RecuperarContrasenaRequest request) {
        restablecimientoService.solicitar(request.correo());
        return ResponseEntity.ok(ApiResponse.success(null,
                "Si la cuenta existe, enviamos un enlace al correo indicado. Vence en "
                        + restablecimientoService.vigenciaMinutos() + " minutos."));
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<ApiResponse<Void>> restablecerContrasena(
            @Valid @RequestBody RestablecerContrasenaRequest request) {
        restablecimientoService.restablecer(request.token(), request.contrasena());
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña restablecida correctamente"));
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<ApiResponse<Void>> cambiarContrasena(
            @CurrentUserId UUID userId,
            @Valid @RequestBody CambiarContrasenaRequest request,
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshDeCookie) {
        perfilService.cambiarContrasena(userId, request, elegirRefreshToken(request.refreshToken(), refreshDeCookie));
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada correctamente"));
    }

    @GetMapping("/usuario-actual")
    public ResponseEntity<ApiResponse<UsuarioResponse>> usuarioActual(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.usuarioActual(userId), "Usuario obtenido correctamente"));
    }
}
