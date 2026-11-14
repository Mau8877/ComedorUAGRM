package com.comedoruagrm.backend.features.usuarios.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarUsuarioPersonalRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CambiarRolRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CrearUsuarioPersonalRequest;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioPersonalResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioPersonalService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioPersonalService.PaginaUsuariosPersonal;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestion de las cuentas del personal (CU14). Todos los endpoints exigen el rol
 * ADMINISTRADOR_DUBSS (lo comprueba el `service`, ver SEGURIDAD_AUTH_BACKEND.md); el
 * administrador que actua sale del token (`@CurrentUserId`), nunca del body.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[rol]", "filter[activo]");

    private final UsuarioPersonalService usuarioPersonalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioPersonalResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[rol]", required = false) String rol,
            @RequestParam(name = "filter[activo]", required = false) String activo,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        validarFiltros(request);
        PaginaUsuariosPersonal pagina = usuarioPersonalService.listar(
                page, pageSize, search, rol, aBooleano(activo), sort);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Cuentas obtenidas correctamente", pagina.meta()));
    }

    // La respuesta lleva una contraseña temporal: nada de caches (ni del navegador ni de un proxy).
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioCreadoResponse>> crear(
            @CurrentUserId UUID actorCodigo, @Valid @RequestBody CrearUsuarioPersonalRequest request) {
        UsuarioCreadoResponse creada = usuarioPersonalService.crear(actorCodigo, request).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.success(creada, "Cuenta creada correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<UsuarioPersonalResponse>> editar(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody ActualizarUsuarioPersonalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarioPersonalService.editar(actorCodigo, codigo, request).datos(),
                "Cuenta actualizada correctamente"));
    }

    @PostMapping("/{codigo}/cambiar-rol")
    public ResponseEntity<ApiResponse<UsuarioPersonalResponse>> cambiarRol(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody CambiarRolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarioPersonalService.cambiarRol(actorCodigo, codigo, request).datos(),
                "Rol cambiado correctamente"));
    }

    @PostMapping("/{codigo}/desactivar")
    public ResponseEntity<ApiResponse<UsuarioPersonalResponse>> desactivar(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarioPersonalService.desactivar(actorCodigo, codigo).datos(),
                "Cuenta desactivada correctamente"));
    }

    @PostMapping("/{codigo}/reactivar")
    public ResponseEntity<ApiResponse<UsuarioPersonalResponse>> reactivar(
            @CurrentUserId UUID actorCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarioPersonalService.reactivar(actorCodigo, codigo).datos(),
                "Cuenta reactivada correctamente"));
    }

    // Un filter[...] que no es soportado se rechaza (no se ignora en silencio).
    private void validarFiltros(HttpServletRequest request) {
        request.getParameterMap().keySet().stream()
                .filter(nombre -> nombre.startsWith("filter[") && !FILTROS_SOPORTADOS.contains(nombre))
                .findFirst()
                .ifPresent(nombre -> {
                    throw new ValidationException(UsuarioErrorCodes.ERR_US_11,
                            "El filtro «" + nombre + "» no está soportado. Filtros válidos: filter[rol], filter[activo]");
                });
    }

    private Boolean aBooleano(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(valor.trim())) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(valor.trim())) {
            return Boolean.FALSE;
        }
        throw new ValidationException(UsuarioErrorCodes.ERR_US_11, "filter[activo] debe ser true o false");
    }
}
