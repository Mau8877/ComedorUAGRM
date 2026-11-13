package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.LocalDate;

/**
 * El bloque «Eres finalista» (CU04): el plazo de entrega (`null` si aun no se aviso), si la documentacion fisica
 * ya fue recibida y los documentos a presentar segun el ultimo aviso de finalista (`null` si no hay aviso).
 */
public record FinalistaPostulanteResponse(LocalDate plazoEntrega, boolean documentacionRecibida,
        String documentosFisicos) {
}
