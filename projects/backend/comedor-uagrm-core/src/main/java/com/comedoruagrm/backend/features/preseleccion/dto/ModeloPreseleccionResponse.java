package com.comedoruagrm.backend.features.preseleccion.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Una version del modelo con sus parametros. `vigente` es la que usan los calculos nuevos. */
public record ModeloPreseleccionResponse(
        UUID codigo,
        String version,
        String descripcion,
        boolean vigente,
        Instant creadoEn,
        List<ParametroModeloResponse> parametros) {
}
