package com.comedoruagrm.backend.features.postulaciones.service;

import java.util.UUID;

/**
 * Resultado «valida» de la validacion del CU18, para el CU02 que la incluye.
 *
 * * `postulanteId` y `convocatoriaId` son ids internos: solo viajan entre servicios, nunca a un DTO.
 * * `postulacionAReenviar` es el codigo de la postulacion «devuelta» de esa convocatoria (CU19, A3) que el
 * * CU02 debe volver a enviar en vez de crear otra; `null` si es un envio nuevo o si el borrador (CU17) es
 * * la propia postulacion que se esta enviando.
 */
public record ResultadoValidacion(long postulanteId, long convocatoriaId, UUID postulacionAReenviar) {
}
