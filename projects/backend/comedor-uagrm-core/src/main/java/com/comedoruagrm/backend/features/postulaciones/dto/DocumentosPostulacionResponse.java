package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.List;

/**
 * Los documentos de una postulacion (CU16): la lista que exige la convocatoria con su estado.
 * `puedeEditar` dice si todavia se puede adjuntar, reemplazar o eliminar (borrador o devuelta, convocatoria abierta).
 * Es un solo recurso, no una coleccion paginada: la lista la fija la convocatoria y es corta.
 */
public record DocumentosPostulacionResponse(boolean puedeEditar, List<DocumentoRequeridoResponse> requeridos,
        ReglasArchivoResponse reglas) {
}
