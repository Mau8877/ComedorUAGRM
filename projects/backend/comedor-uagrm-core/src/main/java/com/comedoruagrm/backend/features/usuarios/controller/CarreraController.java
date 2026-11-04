package com.comedoruagrm.backend.features.usuarios.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.usuarios.dto.CarreraResponse;
import com.comedoruagrm.backend.features.usuarios.service.CarreraService;
import com.comedoruagrm.backend.features.usuarios.service.CarreraService.PaginaCarreras;

import lombok.RequiredArgsConstructor;

/**
 * Catalogo de carreras para el formulario de registro. Es el unico endpoint
 * de negocio publico: se necesita antes de que exista una cuenta (ver
 * SEGURIDAD_AUTH_BACKEND.md#endpoints-publicos).
 */
@RestController
@RequestMapping("/api/v1/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraService carreraService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarreraResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PaginaCarreras pagina = carreraService.listar(page, pageSize);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Carreras obtenidas correctamente", pagina.meta()));
    }
}
