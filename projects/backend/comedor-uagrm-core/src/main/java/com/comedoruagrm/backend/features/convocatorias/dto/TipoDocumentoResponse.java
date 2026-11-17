package com.comedoruagrm.backend.features.convocatorias.dto;

import java.util.UUID;

import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;

public record TipoDocumentoResponse(UUID codigo, String nombre, String descripcion) {

    public static TipoDocumentoResponse de(TipoDocumento tipo) {
        return new TipoDocumentoResponse(tipo.getCodigo(), tipo.getNombre(), tipo.getDescripcion());
    }
}
