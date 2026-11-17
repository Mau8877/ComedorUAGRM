package com.comedoruagrm.backend.features.convocatorias.dto;

import java.util.UUID;

import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;

/** La gestion de una convocatoria dentro de su respuesta: solo lo que la lista necesita mostrar. */
public record GestionResumenResponse(UUID codigo, String etiqueta) {

    public static GestionResumenResponse de(GestionAcademica gestion) {
        return new GestionResumenResponse(gestion.getCodigo(), gestion.getEtiqueta());
    }
}
