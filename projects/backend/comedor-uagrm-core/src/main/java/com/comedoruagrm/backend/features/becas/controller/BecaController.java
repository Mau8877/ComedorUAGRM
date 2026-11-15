package com.comedoruagrm.backend.features.becas.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.becas.dto.BecaResponse;
import com.comedoruagrm.backend.features.becas.dto.CandidatoReemplazoResponse;
import com.comedoruagrm.backend.features.becas.dto.MotivoRequest;
import com.comedoruagrm.backend.features.becas.dto.ReemplazarRequest;
import com.comedoruagrm.backend.features.becas.dto.ReemplazoResponse;
import com.comedoruagrm.backend.features.becas.service.BecaEstadoService;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Los cambios de una beca del padron (CU23): baja, suspension y reemplazo. El rol ADMINISTRADOR_DUBSS lo comprueba
 * el `service`; el administrador que actua sale del token (`@CurrentUserId`).
 */
@RestController
@RequestMapping("/api/v1/becas/{codigo}")
@RequiredArgsConstructor
public class BecaController {

    private final BecaEstadoService estadoService;

    @PostMapping("/dar-de-baja")
    public ResponseEntity<ApiResponse<BecaResponse>> darDeBaja(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody MotivoRequest request) {
        BecaResponse beca = estadoService.darDeBaja(actorCodigo, codigo, request.motivo()).datos();
        return ResponseEntity.ok(ApiResponse.success(beca, "Beca dada de baja correctamente"));
    }

    @PostMapping("/suspender")
    public ResponseEntity<ApiResponse<BecaResponse>> suspender(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody MotivoRequest request) {
        BecaResponse beca = estadoService.suspender(actorCodigo, codigo, request.motivo()).datos();
        return ResponseEntity.ok(ApiResponse.success(beca, "Beca suspendida correctamente"));
    }

    /** El siguiente postulante disponible para ocupar el cupo de una beca dada de baja. */
    @GetMapping("/reemplazo")
    public ResponseEntity<ApiResponse<CandidatoReemplazoResponse>> candidato(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(estadoService.candidatoDeReemplazo(codigo),
                "Postulante disponible obtenido correctamente"));
    }

    @PostMapping("/reemplazar")
    public ResponseEntity<ApiResponse<ReemplazoResponse>> reemplazar(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo, @Valid @RequestBody ReemplazarRequest request) {
        ReemplazoResponse reemplazo = estadoService.reemplazar(actorCodigo, codigo, request.postulanteCodigo())
                .datos();
        return ResponseEntity.ok(ApiResponse.success(reemplazo, "Beca reemplazada correctamente"));
    }
}
