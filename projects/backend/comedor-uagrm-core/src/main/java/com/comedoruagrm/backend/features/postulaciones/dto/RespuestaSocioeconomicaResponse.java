package com.comedoruagrm.backend.features.postulaciones.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Las respuestas ya guardadas de una postulacion, para precargar el formulario (retomar un borrador o reenviar una
 * devuelta). `guardadoEn` es la fecha y hora del ultimo guardado (CU17); `complementarios` es nulo si todavia no se
 * guardo ninguno.
 */
public record RespuestaSocioeconomicaResponse(
        BigDecimal ingresosFamiliares,
        Integer dependientes,
        String procedencia,
        String patrimonio,
        String estadoVulnerabilidad,
        BigDecimal promedioAcademico,
        Instant guardadoEn,
        DatosComplementarios complementarios) {
}
