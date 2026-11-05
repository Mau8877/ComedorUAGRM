package com.comedoruagrm.backend.features.usuarios.dto;

/** Datos del perfil que solo tiene el Postulante. */
public record PerfilPostulanteResponse(String registroUniversitario, CarreraResponse carrera, String telefono) {
}
