package com.comedoruagrm.backend.features.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Actualizacion parcial del perfil (PATCH, CU15): solo los datos de contacto.
 *
 * * Un campo ausente (null) no se toca; uno que llega no puede estar en blanco.
 * * No lleva `ci`, nombres, apellidos, registro universitario ni carrera: si el cliente los
 * * manda igual, Jackson los descarta (el CI no se modifica, RF-10).
 * * `contrasenaActual` solo se usa (y es obligatoria) cuando cambia el correo.
 * ! La contraseña no se loguea.
 */
public record ActualizarPerfilRequest(
        @Pattern(regexp = ".*\\S.*", message = "El correo electrónico no puede estar en blanco")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        @Email(message = "El correo electrónico no es válido")
        String correo,

        @Pattern(regexp = ".*\\S.*", message = "El teléfono no puede estar en blanco")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        String contrasenaActual) {
}
