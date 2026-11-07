package com.comedoruagrm.backend.features.finalistas.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/** Registro de la entrega fisica de un finalista (CU21, flujo B). Sin fecha se usa la de hoy. */
public record RegistrarEntregaRequest(
        LocalDate fechaRecepcion,
        @Size(max = 500, message = "Las observaciones no pueden pasar de 500 caracteres") String observaciones) {
}
