package com.comedoruagrm.backend.features.notificaciones.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.notificaciones.dto.NotificacionResponse;
import com.comedoruagrm.backend.features.notificaciones.dto.ResumenNotificacionesResponse;
import com.comedoruagrm.backend.features.notificaciones.service.BandejaNotificacionesService;
import com.comedoruagrm.backend.features.notificaciones.service.BandejaNotificacionesService.PaginaNotificaciones;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * Notificaciones: el aviso de prueba del Administrador DUBSS (CU-EXTRA-01, lo exige el `service`) y la
 * bandeja del usuario autenticado (CU21, contrato de NOTIFICACIONES_WEB.md). Todo exige sesion y opera
 * solo sobre los avisos del usuario del token.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final BandejaNotificacionesService bandejaService;

    @PostMapping("/prueba")
    public ResponseEntity<ApiResponse<Void>> enviarPrueba(@CurrentUserId UUID userId) {
        notificacionService.enviarPrueba(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "La notificación de prueba se está enviando"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<NotificacionResponse>>> listar(@CurrentUserId UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort) {
        PaginaNotificaciones pagina = bandejaService.listar(userId, page, pageSize, sort);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Listado obtenido correctamente", pagina.meta()));
    }

    @GetMapping("/me/resumen")
    public ResponseEntity<ApiResponse<ResumenNotificacionesResponse>> resumen(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(bandejaService.resumir(userId), "Resumen obtenido correctamente"));
    }

    @PostMapping("/{codigo}/leer")
    public ResponseEntity<ApiResponse<Void>> leer(@CurrentUserId UUID userId, @PathVariable UUID codigo) {
        bandejaService.marcarLeida(userId, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificación marcada como leída"));
    }

    @PostMapping("/leer-todo")
    public ResponseEntity<ApiResponse<Void>> leerTodo(@CurrentUserId UUID userId) {
        bandejaService.marcarTodasLeidas(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificaciones marcadas como leídas"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> eliminarTodas(@CurrentUserId UUID userId) {
        bandejaService.eliminarTodas(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificaciones eliminadas"));
    }
}
