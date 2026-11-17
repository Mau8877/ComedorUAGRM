package com.comedoruagrm.backend.features.postulaciones.service;

import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;

import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte los datos complementarios del formulario (CU02/CU17) de y hacia el JSON de `datos_extra`.
 *
 * * Es solo texto <-> objeto: la validacion de los valores vive en {@link ReglasRespuestaSocioeconomica}.
 * * Un JSON que no se puede leer (columna escrita a mano o por otra version) se trata como «sin datos» y no tumba
 * * la pantalla: el postulante vuelve a completar esa parte en vez de recibir un `500`.
 */
final class ComplementariosJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ComplementariosJson() {
    }

    /** El JSON a guardar; nulo si no hay nada que guardar. */
    static String escribir(DatosComplementarios datos) {
        return datos == null ? null : MAPPER.writeValueAsString(datos);
    }

    /** El objeto guardado; nulo si no hay JSON o no se puede leer. */
    static DatosComplementarios leer(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, DatosComplementarios.class);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
