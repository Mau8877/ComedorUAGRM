package com.comedoruagrm.backend.features.postulaciones.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentoAdjuntoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentosPostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.service.ArchivoSubido;
import com.comedoruagrm.backend.features.postulaciones.service.DocumentosPostulacionService;
import com.comedoruagrm.backend.security.CurrentUserId;

import lombok.RequiredArgsConstructor;

/**
 * Documentos de una postulacion (CU16): listar los que exige la convocatoria, adjuntar, reemplazar, eliminar y
 * abrir un archivo. Exige el rol POSTULANTE (lo comprueba el `service`); el postulante sale del token, nunca de
 * la URL. La subida es `multipart/form-data` y pasa por el backend, que valida el contenido antes de guardarlo.
 */
@RestController
@RequestMapping("/api/v1/postulaciones/{codigo}/documentos")
@RequiredArgsConstructor
public class DocumentosPostulacionController {

    private final DocumentosPostulacionService documentosService;

    @GetMapping
    public ResponseEntity<ApiResponse<DocumentosPostulacionResponse>> listar(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(documentosService.listar(usuarioCodigo, codigo),
                "Documentos obtenidos correctamente"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentoAdjuntoResponse>> adjuntar(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo, @RequestParam("tipoDocumento") UUID tipoDocumento,
            @RequestParam("archivo") MultipartFile archivo) {
        DocumentoAdjuntoResponse documento = documentosService.adjuntar(usuarioCodigo, codigo, tipoDocumento,
                ArchivoSubido.de(archivo));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(documento, "Documento adjuntado correctamente"));
    }

    @PutMapping(path = "/{documento}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentoAdjuntoResponse>> reemplazar(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo, @PathVariable UUID documento,
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(ApiResponse.success(
                documentosService.reemplazar(usuarioCodigo, codigo, documento, ArchivoSubido.de(archivo)),
                "Documento reemplazado correctamente"));
    }

    @DeleteMapping("/{documento}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@CurrentUserId UUID usuarioCodigo, @PathVariable UUID codigo,
            @PathVariable UUID documento) {
        documentosService.eliminar(usuarioCodigo, codigo, documento);
        return ResponseEntity.ok(ApiResponse.success(null, "Documento eliminado correctamente"));
    }

    @GetMapping("/{documento}/acceso")
    public ResponseEntity<ApiResponse<AccesoDocumentoResponse>> acceso(@CurrentUserId UUID usuarioCodigo,
            @PathVariable UUID codigo, @PathVariable UUID documento) {
        return ResponseEntity.ok(ApiResponse.success(documentosService.acceso(usuarioCodigo, codigo, documento),
                "Dirección de acceso generada correctamente"));
    }
}
