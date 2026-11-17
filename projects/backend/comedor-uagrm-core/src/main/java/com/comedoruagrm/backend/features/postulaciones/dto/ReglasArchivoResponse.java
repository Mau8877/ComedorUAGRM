package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.List;

/** Lo que el cliente necesita para validar un archivo antes de subirlo (CU16). El backend vuelve a validar. */
public record ReglasArchivoResponse(long tamanoMaximoBytes, List<String> formatosPermitidos,
        int maximoArchivosPorTipo) {
}
