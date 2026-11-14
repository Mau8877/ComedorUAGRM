package com.comedoruagrm.backend.features.usuarios.dto;

import java.util.UUID;

/** `rol` es el nombre del rol (POSTULANTE, ADMINISTRADOR_DUBSS, ...). */
public record UsuarioResponse(UUID codigo, String nombres, String apellidos, String correo, String rol) {
}
