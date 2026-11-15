package com.comedoruagrm.backend.features.becas.dto;

/** El resultado de un reemplazo (CU23): la beca liberada y la nueva beca vigente del reemplazante. */
public record ReemplazoResponse(BecaResponse becaLiberada, BecaResponse becaReemplazante) {
}
