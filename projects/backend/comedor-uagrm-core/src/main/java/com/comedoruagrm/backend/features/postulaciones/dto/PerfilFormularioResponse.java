package com.comedoruagrm.backend.features.postulaciones.dto;

/** Los datos del perfil que el formulario muestra precargados y que el postulante no edita aqui. */
public record PerfilFormularioResponse(String nombres, String apellidos, String ci, String carrera) {
}
