package com.comedoruagrm.backend.features.preseleccion.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

/** Los parametros de una version nueva del modelo (CU26): una ponderacion por cada variable. */
public record GuardarModeloRequest(
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres") String descripcion,
        List<ParametroModeloRequest> parametros) {
}
