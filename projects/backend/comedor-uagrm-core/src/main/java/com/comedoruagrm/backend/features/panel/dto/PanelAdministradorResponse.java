package com.comedoruagrm.backend.features.panel.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Resumen de la pantalla de inicio del Administrador DUBSS: solo conteos, sin datos personales.
 * `convocatoriaAbierta` es null cuando no hay ninguna abierta.
 */
public record PanelAdministradorResponse(long convocatoriasAbiertas, long postulacionesPendientes,
        long postulacionesCalificadas, long becasVigentes, long becasSuspendidas, long asistenciasHoy,
        ConvocatoriaAbierta convocatoriaAbierta) {

    /** La convocatoria abierta que cierra primero, con su avance. */
    public record ConvocatoriaAbierta(UUID codigo, String nombre, Instant fechaCierre, Integer cupos,
            long postulaciones) {
    }
}
