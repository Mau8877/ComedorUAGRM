package com.comedoruagrm.backend.features.actas.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Un acta de adjudicacion generada (CU08). `generadaPor` es solo el nombre para mostrar. Nunca lleva la ruta
 * del archivo en el almacenamiento ni la direccion de descarga.
 */
public record ActaResponse(
        UUID codigo,
        String numero,
        ConvocatoriaActaResponse convocatoria,
        String gestion,
        long adjudicados,
        Instant generadaEn,
        String generadaPor) {
}
