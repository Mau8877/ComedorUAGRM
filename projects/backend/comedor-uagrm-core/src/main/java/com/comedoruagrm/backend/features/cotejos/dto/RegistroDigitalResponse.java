package com.comedoruagrm.backend.features.cotejos.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Un registro digital de asistencia dentro del periodo de la planilla (CU11): quien asistio, que dia y a que hora. No
 * lleva el CI ni ids internos.
 */
public record RegistroDigitalResponse(
        String nombres,
        String apellidos,
        String carrera,
        LocalDate fecha,
        Instant horaRegistro) {
}
