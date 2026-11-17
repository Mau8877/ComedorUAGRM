package com.comedoruagrm.backend.features.postulaciones.dto;

import java.math.BigDecimal;

/**
 * Las respuestas del formulario socioeconomico (CU02). Todos los campos pueden venir vacios a proposito: el
 * servicio devuelve QUE falta (A3) con `ERR_ENV_04` en vez del `400` generico de Bean Validation.
 *
 * * `complementarios` trae los datos del formulario oficial de la DUBSS que no tienen columna propia.
 */
public record EnviarPostulacionRequest(
        BigDecimal ingresosFamiliares,
        Integer dependientes,
        String procedencia,
        String patrimonio,
        String estadoVulnerabilidad,
        BigDecimal promedioAcademico,
        DatosComplementarios complementarios) {

    /** Solo los seis campos originales (sin los complementarios). */
    public EnviarPostulacionRequest(BigDecimal ingresosFamiliares, Integer dependientes, String procedencia,
            String patrimonio, String estadoVulnerabilidad, BigDecimal promedioAcademico) {
        this(ingresosFamiliares, dependientes, procedencia, patrimonio, estadoVulnerabilidad, promedioAcademico,
                null);
    }
}
