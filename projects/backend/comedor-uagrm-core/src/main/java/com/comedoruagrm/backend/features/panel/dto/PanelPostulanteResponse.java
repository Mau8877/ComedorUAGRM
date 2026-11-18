package com.comedoruagrm.backend.features.panel.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Resumen de la pantalla de inicio del Postulante: la convocatoria abierta (dato público) y en qué punto está la
 * persona respecto de ella. No trae el estado de la postulación (eso lo da «Mis postulaciones», que respeta lo que
 * todavía no se publicó).
 *
 * * `convocatoriaAbierta` es null si no hay ninguna abierta.
 * * `tieneBorrador`: empezó a postular en la convocatoria abierta y no envió. `yaPostulo`: ya envió en ella.
 */
public record PanelPostulanteResponse(ConvocatoriaAbierta convocatoriaAbierta, boolean tieneBorrador,
        boolean yaPostulo) {

    /** La convocatoria abierta que cierra primero. */
    public record ConvocatoriaAbierta(UUID codigo, String nombre, Instant fechaCierre) {
    }
}
