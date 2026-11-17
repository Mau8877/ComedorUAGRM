package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;

/** Una variable del modelo y su ponderacion; `nombre` es para mostrar. */
public record ParametroModeloResponse(String clave, String nombre, BigDecimal ponderacion) {
}
