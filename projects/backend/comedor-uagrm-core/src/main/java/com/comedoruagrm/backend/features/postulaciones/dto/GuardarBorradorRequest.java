package com.comedoruagrm.backend.features.postulaciones.dto;

import java.math.BigDecimal;

/**
 * El avance del formulario socioeconomico que se guarda como borrador (CU17). Es la representacion completa del
 * formulario: todos los campos pueden venir vacios y un campo omitido se guarda como vacio (por eso es `PUT`, no
 * `PATCH`). Lo obligatorio recien se exige al enviar (CU02).
 */
public record GuardarBorradorRequest(
        BigDecimal ingresosFamiliares,
        Integer dependientes,
        String procedencia,
        String patrimonio,
        String estadoVulnerabilidad,
        BigDecimal promedioAcademico,
        DatosComplementarios complementarios) {

    /** Solo los seis campos originales (sin los complementarios). */
    public GuardarBorradorRequest(BigDecimal ingresosFamiliares, Integer dependientes, String procedencia,
            String patrimonio, String estadoVulnerabilidad, BigDecimal promedioAcademico) {
        this(ingresosFamiliares, dependientes, procedencia, patrimonio, estadoVulnerabilidad, promedioAcademico,
                null);
    }
}
