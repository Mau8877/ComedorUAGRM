package com.comedoruagrm.backend.features.finalistas.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para avisar a los finalistas elegidos (CU21, flujo A). */
public record NotificarFinalistasRequest(
        @NotEmpty(message = "Selecciona al menos un finalista") List<@NotNull UUID> finalistas,
        @NotNull(message = "El plazo de entrega es obligatorio") LocalDate plazoEntrega,
        @NotBlank(message = "El lugar y horario de entrega son obligatorios")
        @Size(max = 300, message = "El lugar y horario no pueden pasar de 300 caracteres") String lugarHorario,
        @NotBlank(message = "Indica los documentos que deben presentar")
        @Size(max = 500, message = "Los documentos no pueden pasar de 500 caracteres") String documentos) {
}
