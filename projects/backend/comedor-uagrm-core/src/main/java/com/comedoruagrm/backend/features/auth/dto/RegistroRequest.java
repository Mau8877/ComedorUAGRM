package com.comedoruagrm.backend.features.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del registro de un Postulante. No lleva `rol`: siempre es POSTULANTE
 * (si el cliente lo manda igual, se ignora).
 *
 * * El registro universitario son exactamente 9 digitos y no empieza por 0.
 * * El formato del CI esta por definir con la DUBSS: solo no vacio y largo.
 * * La contraseña tiene al menos 6 caracteres y como maximo 72 (el limite de BCrypt).
 * ! La contraseña no se loguea ni se devuelve nunca.
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
        String telefono,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String contrasena,

        @NotBlank(message = "Confirma tu contraseña")
        String confirmacionContrasena) {

    @AssertTrue(message = "La contraseña y su confirmación no coinciden")
    public boolean isConfirmacionCoincide() {
        return contrasena == null || contrasena.equals(confirmacionContrasena);
    }
}
