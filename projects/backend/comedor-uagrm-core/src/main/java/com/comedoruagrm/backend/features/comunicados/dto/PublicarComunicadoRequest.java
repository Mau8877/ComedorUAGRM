package com.comedoruagrm.backend.features.comunicados.dto;

import java.util.UUID;

import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de un comunicado a publicar (CU21, flujo C). `convocatoriaId` es obligatoria para `FINALISTAS`. */
public record PublicarComunicadoRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede pasar de 150 caracteres") String titulo,
        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 5000, message = "El contenido no puede pasar de 5000 caracteres") String contenido,
        @NotNull(message = "Indica a quién va dirigido") DestinatariosComunicado destinatarios,
        UUID convocatoriaId) {
}
