package com.comedoruagrm.backend.features.convocatorias.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;

public record GestionAcademicaResponse(UUID codigo, String etiqueta, LocalDate fechaInicio, LocalDate fechaFin) {

    public static GestionAcademicaResponse de(GestionAcademica gestion) {
        return new GestionAcademicaResponse(gestion.getCodigo(), gestion.getEtiqueta(), gestion.getFechaInicio(),
                gestion.getFechaFin());
    }
}
