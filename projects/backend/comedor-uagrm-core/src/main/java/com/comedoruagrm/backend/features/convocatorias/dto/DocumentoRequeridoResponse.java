package com.comedoruagrm.backend.features.convocatorias.dto;

import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumento;

public record DocumentoRequeridoResponse(TipoDocumentoResponse tipoDocumento, boolean obligatorio) {

    public static DocumentoRequeridoResponse de(ConvocatoriaDocumento documento) {
        return new DocumentoRequeridoResponse(TipoDocumentoResponse.de(documento.getTipoDocumento()),
                documento.isObligatorio());
    }
}
