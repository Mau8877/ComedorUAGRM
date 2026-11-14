package com.comedoruagrm.backend.features.resultados.dto;

import java.util.UUID;

/** La convocatoria a la que pertenece una publicacion o un resumen (CU22). */
public record ConvocatoriaResultadosResponse(UUID codigo, String nombre, String estado) {
}
