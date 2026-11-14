package com.comedoruagrm.backend.features.comunicados.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.comunicados.dto.ComunicadoResponse;
import com.comedoruagrm.backend.features.comunicados.dto.PublicarComunicadoRequest;
import com.comedoruagrm.backend.features.comunicados.service.ComunicadoService;
import com.comedoruagrm.backend.features.comunicados.service.ComunicadoService.PaginaComunicados;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Comunicados (CU21). Todos los endpoints exigen el rol ADMINISTRADOR_DUBSS (lo comprueba el `service`). */
@RestController
@RequestMapping("/api/v1/comunicados")
@RequiredArgsConstructor
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    @PostMapping
    public ResponseEntity<ApiResponse<ComunicadoResponse>> publicar(
            @CurrentUserId UUID actorCodigo, @Valid @RequestBody PublicarComunicadoRequest request) {
        ComunicadoResponse comunicado = comunicadoService.publicar(actorCodigo, request).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(comunicado, "Comunicado publicado correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComunicadoResponse>>> listar(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        PaginaComunicados pagina = comunicadoService.listar(page, pageSize);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Comunicados obtenidos correctamente", pagina.meta()));
    }
}
