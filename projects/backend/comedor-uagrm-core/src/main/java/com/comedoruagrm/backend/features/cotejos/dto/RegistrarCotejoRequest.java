package com.comedoruagrm.backend.features.cotejos.dto;

import com.comedoruagrm.backend.features.cotejos.enums.ResultadoCotejo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que el Administrador constato al comparar la planilla firmada con los registros digitales (CU11). La
 * consistencia entre las cantidades, el resultado y las observaciones la valida el `service`.
 */
public record RegistrarCotejoRequest(
        @NotNull(message = "El total de registros es obligatorio")
        @Min(value = 0, message = "El total de registros no puede ser negativo")
        @Max(value = 1_000_000, message = "El total de registros es demasiado grande") Integer totalRegistros,
        @NotNull(message = "Las coincidencias son obligatorias")
        @Min(value = 0, message = "Las coincidencias no pueden ser negativas")
        @Max(value = 1_000_000, message = "Las coincidencias son demasiado grandes") Integer coincidencias,
        @NotNull(message = "Las discrepancias son obligatorias")
        @Min(value = 0, message = "Las discrepancias no pueden ser negativas")
        @Max(value = 1_000_000, message = "Las discrepancias son demasiado grandes") Integer discrepancias,
        @NotNull(message = "El resultado es obligatorio") ResultadoCotejo resultado,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres") String observaciones) {
}
