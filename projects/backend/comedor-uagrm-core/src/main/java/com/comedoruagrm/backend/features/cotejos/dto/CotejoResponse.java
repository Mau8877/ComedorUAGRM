package com.comedoruagrm.backend.features.cotejos.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.comedoruagrm.backend.features.cotejos.enums.ResultadoCotejo;

/** Un cotejo registrado (CU11). `realizadoPor` es solo el nombre para mostrar; nunca lleva ids internos. */
public record CotejoResponse(
        UUID codigo,
        UUID planilla,
        LocalDate fechaCotejo,
        int totalRegistros,
        int coincidencias,
        int discrepancias,
        ResultadoCotejo resultado,
        String observaciones,
        String realizadoPor,
        Instant creadoEn) {
}
