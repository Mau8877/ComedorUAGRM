package com.comedoruagrm.backend.features.preseleccion.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que el backend le manda al modulo de IA para calcular el puntaje de una postulacion (CU03, CU26). Es el cuerpo
 * de `POST {app.ia.base-url}/api/v1/puntaje`. Lleva el codigo publico de la postulacion y las variables del modelo,
 * nunca nombres, CI ni contacto.
 *
 * * `modelo` (version, ej. `v1.4`) y `parametros` (clave de la variable → ponderacion) hacen que la IA no guarde
 * * estado: aplica los parametros que recibe. Son `null` solo si todavia no se guardo ninguna version (CU26); en
 * * ese caso la IA usa su modelo por defecto y responde con la version que uso.
 * * Los cinco ultimos salen del formulario oficial de la DUBSS (`respuesta_socioeconomica.datos_extra`); son `null`
 * * en una postulacion enviada antes de que el formulario los pidiera y entonces esas variables no puntuan.
 */
public record SolicitudPuntaje(
        UUID postulacion,
        BigDecimal ingresosFamiliares,
        int dependientes,
        String procedencia,
        String carrera,
        String patrimonio,
        String estadoVulnerabilidad,
        BigDecimal promedioAcademico,
        String modelo,
        Map<String, BigDecimal> parametros,
        String dependenciaEconomica,
        String ocupacionSosten,
        Boolean tieneHijos,
        String cantidadHijos,
        Boolean discapacidad) {

    /** Solo las variables originales (sin los datos del formulario oficial). */
    public SolicitudPuntaje(UUID postulacion, BigDecimal ingresosFamiliares, int dependientes, String procedencia,
            String carrera, String patrimonio, String estadoVulnerabilidad, BigDecimal promedioAcademico,
            String modelo, Map<String, BigDecimal> parametros) {
        this(postulacion, ingresosFamiliares, dependientes, procedencia, carrera, patrimonio, estadoVulnerabilidad,
                promedioAcademico, modelo, parametros, null, null, null, null, null);
    }
}
