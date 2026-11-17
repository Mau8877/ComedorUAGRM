package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/** La confirmacion de «Guardar borrador» (CU17): cuando se guardo y cuantos campos del formulario faltan aun. */
public record BorradorGuardadoResponse(
        UUID postulacion,
        Instant guardadoEn,
        int camposPendientes) {
}
