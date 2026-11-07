package com.comedoruagrm.backend.features.comunicados.dto;

import java.time.Instant;
import java.util.UUID;

import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;

/** Un comunicado publicado. `totalDestinatarios` son los avisos que dejo en las bandejas. Sin ids internos. */
public record ComunicadoResponse(
        UUID codigo,
        UUID convocatoriaId,
        String titulo,
        String contenido,
        DestinatariosComunicado destinatarios,
        Instant publicadoEn,
        long totalDestinatarios) {
}
