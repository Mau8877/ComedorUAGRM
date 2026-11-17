package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.List;

/** Las opciones de las listas desplegables del formulario (por definir con la DUBSS, ver `OpcionesSocioeconomicas`). */
public record OpcionesFormularioResponse(
        List<String> procedencias,
        List<String> patrimonios,
        List<String> estadosVulnerabilidad,
        List<String> dependencias,
        List<String> ocupacionesSosten,
        List<String> rangosIngresos,
        List<String> cantidadesHijos,
        List<String> tenenciasVivienda) {
}
