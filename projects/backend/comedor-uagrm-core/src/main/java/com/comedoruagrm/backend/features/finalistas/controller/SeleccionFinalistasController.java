package com.comedoruagrm.backend.features.finalistas.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.finalistas.service.SeleccionFinalistasService;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionFinalistasResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionarFinalistasRequest;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Seleccionar a los finalistas de una convocatoria cerrada (CU05). Exige el rol ADMINISTRADOR_DUBSS (lo
 * comprueba el `service`). El cuerpo es opcional: sin cantidad se usa la configurada en la convocatoria.
 */
@RestController
@RequestMapping("/api/v1/convocatorias/{codigo}/finalistas")
@RequiredArgsConstructor
public class SeleccionFinalistasController {

    private final SeleccionFinalistasService seleccionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SeleccionFinalistasResponse>> seleccionar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody(required = false) SeleccionarFinalistasRequest request) {
        SeleccionFinalistasResponse resultado = seleccionService.seleccionar(actorCodigo, codigo,
                request == null ? new SeleccionarFinalistasRequest(null) : request).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(resultado, "Finalistas seleccionados correctamente"));
    }
}
