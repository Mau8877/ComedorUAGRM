package com.comedoruagrm.backend.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pedido del enlace para restablecer la contraseña (CU15, flujo A). Solo el correo:
 * la respuesta es siempre la misma exista o no la cuenta.
 */
public record RecuperarContrasenaRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo) {
}
