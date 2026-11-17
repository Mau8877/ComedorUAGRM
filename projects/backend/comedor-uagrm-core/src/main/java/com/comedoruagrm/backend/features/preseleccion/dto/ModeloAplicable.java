package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Una version del modelo lista para calcular con ella: su id interno, su version y la ponderacion de cada variable. */
public record ModeloAplicable(long id, String version, Map<String, BigDecimal> parametros) {
}
