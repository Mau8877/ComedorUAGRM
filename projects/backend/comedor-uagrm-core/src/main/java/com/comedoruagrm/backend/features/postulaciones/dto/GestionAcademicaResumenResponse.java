package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.UUID;

/** La gestion academica de una postulacion (`nombre` es su etiqueta), para «Mis postulaciones» (CU04). */
public record GestionAcademicaResumenResponse(UUID codigo, String nombre) {
}
