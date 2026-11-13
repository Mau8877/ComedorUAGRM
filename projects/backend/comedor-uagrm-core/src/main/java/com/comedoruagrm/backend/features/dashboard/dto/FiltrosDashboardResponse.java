package com.comedoruagrm.backend.features.dashboard.dto;

import java.util.List;
import java.util.UUID;

/** Catalogo de filtros del dashboard (CU12): las gestiones, cada una con sus convocatorias. */
public record FiltrosDashboardResponse(List<GestionFiltro> gestiones) {

    /** Una gestion academica; `vigente` marca la que corresponde a hoy. */
    public record GestionFiltro(UUID codigo, String etiqueta, boolean vigente, List<ConvocatoriaFiltro> convocatorias) {
    }

    /** Una convocatoria de la gestion. */
    public record ConvocatoriaFiltro(UUID codigo, String nombre) {
    }
}
