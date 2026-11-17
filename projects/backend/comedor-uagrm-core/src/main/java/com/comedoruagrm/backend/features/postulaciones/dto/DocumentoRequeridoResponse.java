package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.List;
import java.util.UUID;

/** Un documento que exige la convocatoria, si esta adjuntado y con que archivos (CU16). */
public record DocumentoRequeridoResponse(UUID tipoDocumentoCodigo, String nombre, String descripcion,
        boolean obligatorio, boolean adjuntado, List<DocumentoAdjuntoResponse> archivos) {
}
