package com.comedoruagrm.backend.features.panel.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de la pantalla de inicio del Personal del Comedor: conteos de asistencia, sin datos personales.
 * `ultimoRegistro` es null si hoy no se registró ninguna; `ultimosDias` trae siempre 7 días (los sin registros, en
 * 0), del más antiguo al de hoy.
 */
public record PanelComedorResponse(LocalDate hoy, long becasVigentes, long asistenciasHoy, long registradasPorMi,
        Instant ultimoRegistro, List<AsistenciaDia> ultimosDias) {

    /** Las asistencias registradas en un día. */
    public record AsistenciaDia(LocalDate fecha, long asistencias) {
    }
}
