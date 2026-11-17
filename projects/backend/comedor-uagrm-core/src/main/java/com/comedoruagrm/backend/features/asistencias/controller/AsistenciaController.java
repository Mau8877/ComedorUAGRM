package com.comedoruagrm.backend.features.asistencias.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.asistencias.dto.AsistenciaResponse;
import com.comedoruagrm.backend.features.asistencias.dto.BecadoAsistenciaResponse;
import com.comedoruagrm.backend.features.asistencias.dto.BuscarBecadoRequest;
import com.comedoruagrm.backend.features.asistencias.dto.RegistrarAsistenciaRequest;
import com.comedoruagrm.backend.features.asistencias.service.AsistenciaService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Registro de la asistencia digital (CU10), para la app movil del Personal del Comedor: buscar al becado por CI y
 * registrar su asistencia de hoy. Los dos endpoints exigen el rol PERSONAL_COMEDOR (lo comprueba el `service`).
 *
 * * La busqueda es un `POST` con el CI en el cuerpo (no un `GET ?ci=`) para que el CI no quede en la URL ni en los
 * * registros de acceso del proxy.
 * * Registrar responde `201` cuando crea la asistencia y `200` cuando ya existia la de hoy (idempotencia, RNF-03);
 * * en los dos casos el cuerpo lleva `yaRegistrada` y la hora del registro.
 * * No hay rutas para editar ni eliminar una asistencia (`PUT`, `PATCH` y `DELETE` responden `405`).
 */
@RestController
@RequestMapping("/api/v1/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService service;

    @PostMapping("/buscar-becado")
    public ResponseEntity<ApiResponse<BecadoAsistenciaResponse>> buscarBecado(
            @Valid @RequestBody BuscarBecadoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.buscarBecado(request.ci()),
                "Becado encontrado correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AsistenciaResponse>> registrar(@CurrentUserId UUID actorCodigo,
            @Valid @RequestBody RegistrarAsistenciaRequest request) {
        AsistenciaResponse asistencia = service.registrar(actorCodigo, request.becaCodigo());
        return asistencia.yaRegistrada()
                ? ResponseEntity.ok(ApiResponse.success(asistencia, "La asistencia de hoy ya estaba registrada"))
                : ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(asistencia, "Asistencia registrada correctamente"));
    }
}
