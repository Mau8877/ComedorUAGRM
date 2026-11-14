package com.comedoruagrm.backend.features.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear la cuenta de un funcionario (CU14).
 *
 * * `rol` es el nombre de uno de los tres roles del personal; si es otro (o
 * * Postulante) lo rechaza el service con ERR_US_10, no esta validacion.
 * * El formato del CI esta por definir con la DUBSS: solo no vacio y largo.
 */
public record CrearUsuarioPersonalRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String apellidos,

        @NotBlank(message = "El CI es obligatorio")
        @Size(max = 20, message = "El CI no puede superar los 20 caracteres")
        String ci,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        @Email(message = "El correo electrónico no es válido")
        String correo,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {
}
