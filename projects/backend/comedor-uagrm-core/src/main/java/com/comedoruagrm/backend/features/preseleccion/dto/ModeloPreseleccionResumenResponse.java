package com.comedoruagrm.backend.features.preseleccion.dto;

import java.time.Instant;
import java.util.UUID;

/** Una fila del historial de versiones: `vigente` o archivada. */
public record ModeloPreseleccionResumenResponse(UUID codigo, String version, String descripcion, boolean vigente,
        Instant creadoEn) {
}
