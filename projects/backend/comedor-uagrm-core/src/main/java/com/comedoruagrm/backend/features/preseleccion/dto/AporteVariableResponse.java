package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;

/**
 * Cuanto aporto una variable al puntaje de una postulacion (CU03). `nivel` va de 0 a 1 y es nulo si la postulacion no
 * trae ese dato; `puntos` y `maximo` estan en la escala del puntaje de la IA (0 a 70).
 */
public record AporteVariableResponse(String clave, BigDecimal nivel, BigDecimal ponderacion, BigDecimal puntos,
        BigDecimal maximo) {
}
