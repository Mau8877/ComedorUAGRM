package com.comedoruagrm.backend.features.contacto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.contacto.dto.ContactoRequest;
import com.comedoruagrm.backend.features.contacto.service.ContactoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Formulario de contacto de la landing (público). */
@RestController
@RequestMapping("/api/v1/contacto")
@RequiredArgsConstructor
public class ContactoController {

    private final ContactoService contactoService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> enviar(@Valid @RequestBody ContactoRequest request) {
        contactoService.enviar(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mensaje enviado correctamente"));
    }
}
