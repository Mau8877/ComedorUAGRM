package com.comedoruagrm.backend.features.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Edicion parcial de una cuenta del personal (PATCH): solo cambia lo que llega.
 *
 * * Un campo ausente (null) no se toca; uno que llega no puede estar en blanco.
 * * No lleva `ci` (no se modifica, RF-10) ni `rol` (tiene su propia accion).
 */
public record ActualizarUsuarioPersonalRequest(
        @Pattern(regexp = ".*\\S.*", message = "Los nombres no pueden estar en blanco")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
        String nombres,

        @Pattern(regexp = ".*\\S.*", message = "Los apellidos no pueden estar en blanco")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String apellidos,

        @Pattern(regexp = ".*\\S.*", message = "El correo electrónico no puede estar en blanco")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        @Email(message = "El correo electrónico no es válido")
        String correo
) {
}
