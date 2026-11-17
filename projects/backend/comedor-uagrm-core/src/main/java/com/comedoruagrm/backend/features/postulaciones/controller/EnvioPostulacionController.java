package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ComprobantePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.EnviarPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.FormularioPostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.service.EnvioPostulacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * Enviar la postulacion a la Beca Comedor (CU02). Exige el rol POSTULANTE (lo comprueba el `service`); el
 * postulante sale del token, nunca de la URL ni del cuerpo.
 *
 * * `POST /api/v1/postulaciones`: «Nueva postulacion». `201` si crea el borrador, `200` si ya habia uno (o una
 * * devuelta a corregir).
 * * `POST /api/v1/postulaciones/{codigo}/enviar`: envia las respuestas del formulario. `200` con el comprobante
 * * (tambien si la peticion se repite).
 */
@RestController
@RequestMapping("/api/v1/postulaciones")
@RequiredArgsConstructor
public class EnvioPostulacionController {

    private final EnvioPostulacionService envioService;

    @PostMapping
    public ResponseEntity<ApiResponse<FormularioPostulacionResponse>> iniciar(@CurrentUserId UUID usuarioCodigo) {
        EnvioPostulacionService.Inicio inicio = envioService.iniciar(usuarioCodigo);
        return ResponseEntity.status(inicio.creada() ? HttpStatus.CREATED : HttpStatus.OK).body(ApiResponse
                .success(inicio.formulario(), inicio.creada() ? "Postulación iniciada correctamente"
                        : "Postulación en curso obtenida correctamente"));
    }

    @PostMapping("/{codigo}/enviar")
    public ResponseEntity<ApiResponse<ComprobantePostulacionResponse>> enviar(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo, @RequestBody EnviarPostulacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(envioService.enviar(usuarioCodigo, codigo, request),
                "Postulación enviada correctamente"));
    }
}
