package com.comedoruagrm.backend.features.asistencias.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** La beca cuya asistencia de hoy se registra (CU10): el codigo que devolvio la busqueda por CI. */
public record RegistrarAsistenciaRequest(
        @NotNull(message = "La beca es obligatoria") UUID becaCodigo) {
}
