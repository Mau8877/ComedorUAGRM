package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.List;
import java.util.UUID;

/**
 * Finalistas creados por una seleccion (CU05), en el orden de la lista priorizada. `convocatoria` es su
 * codigo publico.
 */
public record SeleccionFinalistasResponse(
        UUID convocatoria,
        int seleccionados,
        List<FinalistaSeleccionado> finalistas) {

    /** Un finalista creado: codigo del finalista, codigo de su postulacion y su posicion en la lista completa. */
    public record FinalistaSeleccionado(UUID codigo, UUID postulacion, long posicion) {
    }
}
