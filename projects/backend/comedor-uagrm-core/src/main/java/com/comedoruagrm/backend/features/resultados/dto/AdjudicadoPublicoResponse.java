package com.comedoruagrm.backend.features.resultados.dto;

import java.math.BigDecimal;

/**
 * Un adjudicado en la lista publica (CU22): solo nombre y apellido, nota (el puntaje final), carrera y
 * facultad. Nunca CI, correo ni datos socioeconomicos.
 */
public record AdjudicadoPublicoResponse(
        String nombres,
        String apellidos,
        BigDecimal nota,
        String carrera,
        String facultad) {
}
