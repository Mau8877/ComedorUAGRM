package com.comedoruagrm.backend.features.convocatorias.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de `POST` y `PUT /api/v1/convocatorias`: la convocatoria completa. `cupos`, `puntajeMinimo` y
 * `cantidadFinalistas` son opcionales (la DUBSS aun no los define); omitidos, quedan vacios. El estado no
 * viene aca: solo cambia con las acciones de abrir y cerrar.
 */
public record ConvocatoriaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede pasar de 150 caracteres")
        String nombre,

        @NotNull(message = "La gestión académica es obligatoria")
        UUID gestionCodigo,

        @NotNull(message = "La fecha de apertura es obligatoria")
        LocalDate fechaApertura,

        @NotNull(message = "La fecha de cierre es obligatoria")
        LocalDate fechaCierre,

        @Positive(message = "Los cupos deben ser un número entero positivo")
        Integer cupos,

        @DecimalMin(value = "0", message = "El puntaje mínimo debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "El puntaje mínimo debe estar entre 0 y 100")
        @Digits(integer = 3, fraction = 2, message = "El puntaje mínimo admite hasta dos decimales")
        BigDecimal puntajeMinimo,

        @Positive(message = "La cantidad de finalistas debe ser un número entero positivo")
        Integer cantidadFinalistas,

        @NotEmpty(message = "Indica al menos un documento requerido")
        @Valid
        List<DocumentoRequeridoRequest> documentos) {
}
