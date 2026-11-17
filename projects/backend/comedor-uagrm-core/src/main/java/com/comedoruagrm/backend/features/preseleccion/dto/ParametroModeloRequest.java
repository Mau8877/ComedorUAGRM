package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;

/** La ponderacion (puntos maximos) de una variable del modelo. Se valida en el servicio, para explicar que esta mal. */
public record ParametroModeloRequest(String clave, BigDecimal ponderacion) {
}
