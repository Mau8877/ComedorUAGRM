package com.comedoruagrm.backend.features.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del registro de un Postulante. No lleva `correo`, `rol` ni
 * `firebaseUid`: el correo y el uid salen del token verificado y el rol
 * siempre es POSTULANTE. Si el cliente los manda igual, se ignoran.
 *
 * * El registro universitario son exactamente 9 digitos y no empieza por 0.
 * * El formato del CI esta por definir con la DUBSS: solo no vacio y largo.
 */
public record RegistroRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String apellidos,

        @NotBlank(message = "El CI es obligatorio")
        @Size(max = 20, message = "El CI no puede superar los 20 caracteres")
        String ci,

        @NotBlank(message = "El registro universitario es obligatorio")
        @Pattern(regexp = "^[1-9]\\d{8}$",
                message = "El registro universitario debe tener 9 dígitos y no empezar por 0")
        String registroUniversitario,

        @NotNull(message = "La carrera es obligatoria")
        UUID carreraCodigo,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono) {
}
