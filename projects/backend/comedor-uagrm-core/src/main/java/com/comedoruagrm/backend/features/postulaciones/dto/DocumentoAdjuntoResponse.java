package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.UUID;

/** Un archivo adjuntado a la postulacion (CU16). No lleva la ruta de almacenamiento. */
public record DocumentoAdjuntoResponse(UUID codigo, String nombreArchivo, String mimeType, long tamanoBytes,
        Instant subidoEn) {
}
