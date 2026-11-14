package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Una postulacion enviada en el listado «Mis postulaciones» (CU04). `estado` es el estado visible para el
 * postulante (en minuscula, como el diseño logico), que puede diferir del real hasta publicar los resultados.
 */
public record MiPostulacionResumenResponse(
        UUID codigo,
        ConvocatoriaResumenResponse convocatoria,
        GestionAcademicaResumenResponse gestionAcademica,
        Instant fechaEnvio,
        String estado,
        short numeroEnvios) {
}
