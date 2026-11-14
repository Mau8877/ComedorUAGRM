package com.comedoruagrm.backend.features.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** Nombre del rol nuevo (uno de los tres del personal; lo valida el service con ERR_US_10). */
public record CambiarRolRequest(
        @NotBlank(message = "El rol es obligatorio")
        String rol
) {
}
