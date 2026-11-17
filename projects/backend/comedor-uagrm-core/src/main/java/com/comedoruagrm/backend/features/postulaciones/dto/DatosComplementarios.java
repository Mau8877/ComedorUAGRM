package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.List;

/**
 * Los datos del formulario socioeconomico oficial de la DUBSS (FORM-DUBS-001) que no tienen columna propia en
 * `respuesta_socioeconomica`: se guardan juntos en `datos_extra` (jsonb), sin migrar el esquema.
 *
 * * Las opciones de las listas son las del formulario en papel (ver `OpcionesSocioeconomicas`). Todos los campos
 * * pueden venir vacios: en un borrador (CU17) nada es obligatorio y al enviar (CU02) el servicio dice que falta.
 * * El grupo familiar admite hasta 10 personas, como la tabla del formulario.
 * ? Que campos son obligatorios, y si estos datos entran al puntaje de la IA, esta por definir con la DUBSS.
 */
public record DatosComplementarios(
        String dependenciaEconomica,
        String ocupacionSosten,
        String rangoIngresos,
        Boolean tieneHijos,
        String cantidadHijos,
        List<MiembroGrupoFamiliar> grupoFamiliar,
        Residencia residencia,
        String tenenciaVivienda,
        Infraestructura infraestructura,
        Boolean otroBeneficio,
        String otroBeneficioDetalle,
        Boolean discapacidad,
        String discapacidadDetalle,
        String comentario) {

    /** Una fila de la tabla «Grupo familiar» del formulario. */
    public record MiembroGrupoFamiliar(String nombre, String parentesco, Integer edad, String ocupacion,
            String observacion) {
    }

    /** «5. De la residencia»: donde vive el postulante. */
    public record Residencia(String provincia, String zonaAnillo, String barrio, String calle) {
    }

    /** «7. Infraestructura de la vivienda»: la cantidad de cada ambiente. */
    public record Infraestructura(Integer dormitorios, Integer banos, Integer comedor, Integer sala,
            Integer patio) {
    }
}
