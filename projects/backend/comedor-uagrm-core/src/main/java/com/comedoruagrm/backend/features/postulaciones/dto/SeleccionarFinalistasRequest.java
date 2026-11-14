package com.comedoruagrm.backend.features.postulaciones.dto;

import jakarta.validation.constraints.Positive;

/**
 * Cuantos finalistas seleccionar (CU05). Opcional: si falta se usa la cantidad configurada en la
 * convocatoria.
 */
public record SeleccionarFinalistasRequest(
        @Positive(message = "La cantidad debe ser un número entero positivo") Integer cantidad) {
}
